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

goals=("$@")
if [[ ${#goals[@]} -eq 0 ]]; then
  goals=(verify)
fi

mvn_cmd "${goals[@]}"
