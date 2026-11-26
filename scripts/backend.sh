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
  # --network host: la app alcanza Postgres/Mailpit de infra/docker-compose.dev.yml en 127.0.0.1.
  tty_args=()
  if [[ -t 0 && -t 1 ]]; then tty_args=(-it); fi
  exec docker run --rm "${tty_args[@]}" --network host "${DOCKER_COMMON[@]}" \
    --name ferro-api-dev \
    -e SERVER_PORT="${SERVER_PORT:-8080}" \
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
