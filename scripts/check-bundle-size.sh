#!/usr/bin/env bash
# RNF-06: JS inicial comprimido ≤ 300 KB. Se ejecuta tras `ng build`.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DIST="$ROOT/frontend/dist/ferro/browser"
LIMIT=$((300 * 1024))

[[ -d "$DIST" ]] || { echo "No existe $DIST; ejecuta el build primero" >&2; exit 1; }
total="$(find "$DIST" -maxdepth 1 -name '*.js' -type f -print0 | xargs -0 -r -n1 gzip -c | wc -c)"
if (( total > LIMIT )); then
  echo "Bundle JS gzip: $((total / 1024)) KB > 300 KB" >&2
  exit 1
fi
echo "Bundle JS gzip: $((total / 1024)) KB (límite 300 KB)"
