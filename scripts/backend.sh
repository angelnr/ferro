#!/usr/bin/env bash
# Ejecuta Maven (Java 21) dentro de Docker para no depender del JDK del host.
# Uso: scripts/backend.sh [goal...]   (por defecto: verify)
#      scripts/backend.sh shell
set -euo pipefail
source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/lib.sh"

if [[ "${1:-}" == "shell" ]]; then
  require_docker
  ensure_image "$MAVEN_IMAGE"
  mkdir -p "$M2_CACHE"
  exec docker run -it "${DOCKER_COMMON[@]}" -v "$M2_CACHE:/tmp/.m2" -w /workspace/backend "$MAVEN_IMAGE" sh
fi

if [[ "${1:-}" == "run" ]]; then
  require_docker
  ensure_image "$MAVEN_IMAGE"
  mkdir -p "$M2_CACHE"
  # Allowlist CORS/CSRF: localhost + todas las IPv4 del host en el puerto del
  # dev server, para poder usar la UI desde otro dispositivo de la tailnet/LAN.
  cors_origins="${CORS_ALLOWED_ORIGINS:-http://localhost:4200,http://127.0.0.1:4200}"
  extra_origins="$(dev_frontend_origins)"
  if [[ -n "$extra_origins" ]]; then
    cors_origins="${cors_origins},${extra_origins}"
  fi
  # --network host: la app alcanza Postgres/Mailpit de infra/docker-compose.dev.yml en 127.0.0.1.
  tty_args=()
  if [[ -t 0 && -t 1 ]]; then tty_args=(-it); fi
  docker rm -f ferro-api-dev >/dev/null 2>&1 || true
  exec docker run --rm "${tty_args[@]}" --network host "${DOCKER_COMMON[@]}" \
    --name ferro-api-dev \
    -e SERVER_PORT="${SERVER_PORT:-8080}" \
    -e CORS_ALLOWED_ORIGINS="$cors_origins" \
    -e APP_FRONTEND_URL="${APP_FRONTEND_URL:-http://localhost:4200}" \
    -e GOOGLE_CLIENT_ID="${GOOGLE_CLIENT_ID:-}" \
    -e GOOGLE_CLIENT_SECRET="${GOOGLE_CLIENT_SECRET:-}" \
    -e GOOGLE_REDIRECT_URI="${GOOGLE_REDIRECT_URI:-http://localhost:4200/auth/oauth/callback/google}" \
    -e GITHUB_CLIENT_ID="${GITHUB_CLIENT_ID:-}" \
    -e GITHUB_CLIENT_SECRET="${GITHUB_CLIENT_SECRET:-}" \
    -e GITHUB_REDIRECT_URI="${GITHUB_REDIRECT_URI:-http://localhost:4200/auth/oauth/callback/github}" \
    -v "$M2_CACHE:/tmp/.m2" \
    -w /workspace/backend \
    "$MAVEN_IMAGE" \
    mvn -B -ntp -Dmaven.repo.local=/tmp/.m2/repository spring-boot:run
fi

goals=("$@")
if [[ ${#goals[@]} -eq 0 ]]; then
  goals=(verify)
fi

mvn_cmd "${goals[@]}"
