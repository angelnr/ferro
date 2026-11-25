#!/usr/bin/env bash
# Utilidades compartidas por los scripts del harness de Ferro.
# No ejecutar directamente: se carga con `source`.

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
HARNESS_DIR="$ROOT/.harness"
LOG_DIR="$HARNESS_DIR/logs"

MAVEN_IMAGE="${FERRO_MAVEN_IMAGE:-maven:3.9-eclipse-temurin-21}"
NODE_IMAGE="${FERRO_NODE_IMAGE:-node:24.22.1-alpine}"
GITLEAKS_IMAGE="${FERRO_GITLEAKS_IMAGE:-ghcr.io/gitleaks/gitleaks:latest}"

M2_CACHE="${FERRO_M2_CACHE:-$HOME/.cache/ferro/m2}"
NPM_CACHE="${FERRO_NPM_CACHE:-$HOME/.cache/ferro/npm}"

if [[ -t 1 ]]; then
  C_RESET=$'\033[0m'
  C_BOLD=$'\033[1m'
  C_GREEN=$'\033[32m'
  C_RED=$'\033[31m'
  C_YELLOW=$'\033[33m'
  C_DIM=$'\033[2m'
else
  C_RESET="" C_BOLD="" C_GREEN="" C_RED="" C_YELLOW="" C_DIM=""
fi

log_step() { printf '%s==>%s %s\n' "$C_BOLD" "$C_RESET" "$*"; }
log_info() { printf '%s    %s%s\n' "$C_DIM" "$*" "$C_RESET"; }
log_ok() { printf '%s OK %s %s\n' "$C_GREEN" "$C_RESET" "$*"; }
log_warn() { printf '%sWARN%s %s\n' "$C_YELLOW" "$C_RESET" "$*"; }
log_fail() { printf '%sFAIL%s %s\n' "$C_RED" "$C_RESET" "$*"; }

die() {
  log_fail "$*"
  exit 1
}

require_command() {
  command -v "$1" >/dev/null 2>&1 || die "Falta el comando '$1'. Instálalo o revisa scripts/doctor.sh."
}

require_docker() {
  require_command docker
  docker info >/dev/null 2>&1 || die "Docker no responde. Arranca el daemon antes de verificar."
}

ensure_image() {
  local image="$1"
  docker image inspect "$image" >/dev/null 2>&1 || {
    log_info "Descargando imagen $image (solo la primera vez)…"
    docker pull "$image" >/dev/null
  }
}

# Argumentos comunes de Docker: usuario actual, HOME escribible y socket para Testcontainers.
DOCKER_COMMON=(
  --rm
  --user "$(id -u):$(id -g)"
  -e HOME=/tmp
  -v "$ROOT:/workspace"
)
if [[ -S /var/run/docker.sock ]]; then
  DOCKER_COMMON+=(
    -v "/var/run/docker.sock:/var/run/docker.sock"
    --group-add "$(stat -c '%g' /var/run/docker.sock)"
  )
fi

run_backend() {
  require_docker
  ensure_image "$MAVEN_IMAGE"
  mkdir -p "$M2_CACHE"
  docker run "${DOCKER_COMMON[@]}" \
    -e MAVEN_CONFIG=/tmp/.m2 \
    -v "$M2_CACHE:/tmp/.m2" \
    -w /workspace/backend \
    "$MAVEN_IMAGE" "$@"
}

run_frontend() {
  require_docker
  ensure_image "$NODE_IMAGE"
  mkdir -p "$NPM_CACHE"
  docker run "${DOCKER_COMMON[@]}" \
    -e NG_CLI_ANALYTICS=false \
    -v "$NPM_CACHE:/tmp/.npm" \
    -w /workspace/frontend \
    "$NODE_IMAGE" "$@"
}

run_gitleaks() {
  require_docker
  ensure_image "$GITLEAKS_IMAGE"
  docker run --rm -v "$ROOT:/repo" "$GITLEAKS_IMAGE" "$@"
}

mvn_cmd() {
  run_backend mvn -B -ntp -Dmaven.repo.local=/tmp/.m2/repository "$@"
}

frontend_install() {
  if [[ "${FERRO_INSTALL:-auto}" == "always" ]]; then
    run_frontend npm ci
    return
  fi
  if [[ ! -d "$ROOT/frontend/node_modules" ]] \
    || [[ "$ROOT/frontend/package-lock.json" -nt "$ROOT/frontend/node_modules/.package-lock.json" ]]; then
    run_frontend npm ci
  else
    log_info "node_modules ya está al día (FERRO_INSTALL=always para forzar npm ci)"
  fi
}

git_sha() {
  git -C "$ROOT" rev-parse --short HEAD 2>/dev/null || echo "sin-git"
}

git_dirty() {
  if [[ -n "$(git -C "$ROOT" status --porcelain 2>/dev/null)" ]]; then echo "dirty"; else echo "clean"; fi
}
