#!/usr/bin/env bash
# Diagnóstico del entorno. No modifica nada; dice exactamente qué falta.
set -uo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
errors=0
warnings=0

ok() { printf 'OK    %s\n' "$*"; }
warn() { printf 'WARN  %s\n' "$*"; warnings=$((warnings + 1)); }
bad() { printf 'FAIL  %s\n' "$*"; errors=$((errors + 1)); }

check_cmd() {
  local cmd="$1" required="${2:-required}"
  if command -v "$cmd" >/dev/null 2>&1; then
    ok "$cmd: $("$cmd" --version 2>&1 | head -n 1)"
  elif [[ "$required" == "required" ]]; then
    bad "falta '$cmd'"
  else
    warn "falta '$cmd' (opcional en el host; el harness usa Docker)"
  fi
}

printf '== Herramientas del host ==\n'
check_cmd git
check_cmd docker
check_cmd python3
check_cmd curl
check_cmd java optional
check_cmd mvn optional
check_cmd node optional
check_cmd npm optional

printf '\n== Docker ==\n'
if command -v docker >/dev/null 2>&1; then
  if docker info >/dev/null 2>&1; then
    ok "daemon accesible ($(docker version --format '{{.Server.Version}}'))"
  else
    bad "el daemon de Docker no responde"
  fi
  if docker compose version >/dev/null 2>&1; then
    ok "docker compose: $(docker compose version --short 2>/dev/null || echo presente)"
  else
    warn "docker compose no disponible (lo necesitarás en M0 para Postgres/Mailpit)"
  fi
  [[ -S /var/run/docker.sock ]] && ok "socket /var/run/docker.sock presente (Testcontainers)" \
    || warn "sin /var/run/docker.sock; Testcontainers no funcionará"
fi

printf '\n== Imágenes del harness ==\n'
MAVEN_IMAGE="${FERRO_MAVEN_IMAGE:-maven:3.9-eclipse-temurin-21}"
NODE_IMAGE="${FERRO_NODE_IMAGE:-node:24.22.1-alpine}"
GITLEAKS_IMAGE="${FERRO_GITLEAKS_IMAGE:-ghcr.io/gitleaks/gitleaks:latest}"
for image in "$MAVEN_IMAGE" "$NODE_IMAGE" "$GITLEAKS_IMAGE"; do
  if docker image inspect "$image" >/dev/null 2>&1; then
    ok "imagen local $image"
  else
    warn "imagen no descargada: $image (se bajará en la primera verificación)"
  fi
done

printf '\n== Hooks de git ==\n'
configured="$(git -C "$ROOT" config --get core.hooksPath || true)"
if [[ "$configured" == ".githooks" ]]; then
  ok "core.hooksPath=.githooks"
else
  warn "hooks no instalados; ejecuta: make hooks"
fi
for hook in "$ROOT"/.githooks/*; do
  [[ -e "$hook" ]] || continue
  [[ -x "$hook" ]] && ok "hook ejecutable ${hook#"$ROOT/"}" || warn "hook no ejecutable ${hook#"$ROOT/"}"
done

printf '\n== Cachés y espacio ==\n'
mkdir -p "$HOME/.cache/ferro/m2" "$HOME/.cache/ferro/npm"
ok "caché Maven: $HOME/.cache/ferro/m2"
ok "caché npm:   $HOME/.cache/ferro/npm"
available_kb="$(df -Pk "$ROOT" | awk 'NR==2 {print $4}')"
if [[ "$available_kb" -lt 3000000 ]]; then
  warn "quedan $((available_kb / 1024)) MB libres; Testcontainers e imágenes necesitan espacio"
else
  ok "espacio libre: $((available_kb / 1024)) MB"
fi

printf '\n== Puertos de desarrollo (solo aviso) ==\n'
for port in 5432 8025 1025 9090 3003 8080 4200; do
  if command -v ss >/dev/null 2>&1 && ss -ltn "sport = :$port" 2>/dev/null | grep -q LISTEN; then
    warn "puerto $port ya ocupado (revisa ~/srv/PUERTOS.md)"
  fi
done

printf '\n== Red (solo aviso) ==\n'
curl -fsS --max-time 8 -o /dev/null https://repo.maven.apache.org/maven2/ \
  && ok "Maven Central accesible" || warn "no se pudo contactar Maven Central"
curl -fsS --max-time 8 -o /dev/null https://registry.npmjs.org/ \
  && ok "registry.npmjs.org accesible" || warn "no se pudo contactar npm"

printf '\n== Resultado ==\n'
if [[ "$errors" -gt 0 ]]; then
  printf '%s errores, %s avisos. Corrige los FAIL antes de verificar.\n' "$errors" "$warnings"
  exit 1
fi
printf 'Entorno listo (%s avisos). Siguiente: make hooks && make verify\n' "$warnings"
