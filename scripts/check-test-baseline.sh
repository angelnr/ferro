#!/usr/bin/env bash
# Impide que un PR "verde" elimine tests para conseguirlo.
# El baseline solo sube o se actualiza de forma explícita con `make baseline`.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BASELINE="$ROOT/docs/harness/test-baseline.json"

[[ -f "$BASELINE" ]] || { echo "Falta $BASELINE" >&2; exit 1; }

backend_now="$(grep -RhE '^[[:space:]]*@(Test|ParameterizedTest|ArchTest)[[:space:]]*$' "$ROOT/backend/src/test" --include='*.java' | wc -l | tr -d ' ')"
frontend_now="$(grep -RhE '^[[:space:]]*(it|test)\(' "$ROOT/frontend/src" --include='*.spec.ts' | wc -l | tr -d ' ')"

python3 - "$BASELINE" "$backend_now" "$frontend_now" <<'PY'
import json, sys
baseline_path, backend_now, frontend_now = sys.argv[1], int(sys.argv[2]), int(sys.argv[3])
baseline = json.load(open(baseline_path))
errors = []
for key, now in (("backend", backend_now), ("frontend", frontend_now)):
    minimum = int(baseline.get(key, 0))
    if now < minimum:
        errors.append(f"{key}: hay {now} tests, el baseline exige >= {minimum}")
if errors:
    print("FAIL  Se han eliminado tests:", file=sys.stderr)
    for error in errors:
        print(f"      {error}", file=sys.stderr)
    print("      Restaura los tests o justifica el cambio actualizando el baseline.", file=sys.stderr)
    sys.exit(1)
print(f"Baseline OK: backend={backend_now} (min {baseline.get('backend')}), frontend={frontend_now} (min {baseline.get('frontend')})")
PY
