#!/usr/bin/env bash
# Actualiza el baseline de tests. Úsalo solo al añadir tests, nunca al borrarlos.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BASELINE="$ROOT/docs/harness/test-baseline.json"

backend="$(grep -RhE '^[[:space:]]*@(Test|ParameterizedTest|ArchTest)[[:space:]]*$' "$ROOT/backend/src/test" --include='*.java' | wc -l | tr -d ' ')"
frontend="$(grep -RhE '^[[:space:]]*(it|test)\(' "$ROOT/frontend/src" --include='*.spec.ts' | wc -l | tr -d ' ')"

python3 - "$BASELINE" "$backend" "$frontend" <<'PY'
import json, sys
path, backend, frontend = sys.argv[1], int(sys.argv[2]), int(sys.argv[3])
data = {"backend": backend, "frontend": frontend}
with open(path, "w", encoding="utf-8") as handle:
    json.dump(data, handle, indent=2)
    handle.write("\n")
print(f"Baseline actualizado: backend={backend}, frontend={frontend}")
PY

printf 'Revisa el diff de %s y justifícalo en el commit.\n' "${BASELINE#"$ROOT/"}"
