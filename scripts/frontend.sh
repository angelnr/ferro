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
  serve)
    require_docker
    ensure_image "$NODE_IMAGE"
    mkdir -p "$NPM_CACHE"
    tty_args=()
    if [[ -t 0 && -t 1 ]]; then tty_args=(-it); fi
    # 127.0.0.1 por defecto; FERRO_FRONTEND_BIND=0.0.0.0 para tailnet/LAN.
    frontend_bind="${FERRO_FRONTEND_BIND:-127.0.0.1}"
    frontend_port="${FERRO_FRONTEND_PORT:-4200}"
    log_info "Angular en http://${frontend_bind}:${frontend_port}"
    exec docker run --rm "${tty_args[@]}" -p "${frontend_bind}:${frontend_port}:4200" "${DOCKER_COMMON[@]}" \
      -e NG_CLI_ANALYTICS=false \
      -v "$NPM_CACHE:/tmp/.npm" \
      -w /workspace/frontend \
      "$NODE_IMAGE" \
      npm start -- --host 0.0.0.0 --port 4200
    ;;
  e2e)
    run_playwright
    ;;
  "")
    die "Uso: scripts/frontend.sh install|shell|run <script>|<args npm…>"
    ;;
  *)
    run_frontend npm "$@"
    ;;
esac
