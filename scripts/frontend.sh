#!/usr/bin/env bash
# Ejecuta npm/Node 24 dentro de Docker para no depender del Node del host.
# Uso: scripts/frontend.sh install | shell | run <script> | <args npm...>
set -euo pipefail
source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/lib.sh"

case "${1:-}" in
  install)
    run_frontend npm ci
    ;;
  shell)
    require_docker
    ensure_image "$NODE_IMAGE"
    mkdir -p "$NPM_CACHE"
    exec docker run -it "${DOCKER_COMMON[@]}" -v "$NPM_CACHE:/tmp/.npm" -w /workspace/frontend "$NODE_IMAGE" sh
    ;;
  "")
    die "Uso: scripts/frontend.sh install|shell|run <script>|<args npm…>"
    ;;
  *)
    run_frontend npm "$@"
    ;;
esac
