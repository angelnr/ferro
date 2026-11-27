#!/usr/bin/env bash
# Verificación única y honesta de Ferro.
#
#   scripts/verify.sh              # todo: backend + frontend + secretos + docs + harness
#   scripts/verify.sh --backend    # solo backend
#   scripts/verify.sh --frontend   # solo frontend
#   scripts/verify.sh --fast       # lint/formato/secretos; NO válido como evidencia final
#   scripts/verify.sh --ci         # omite la comprobación de hooks locales
#
# Sale 0 solo si todos los pasos ejecutados pasan. No hay "skip" silenciosos.
set -euo pipefail
source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/lib.sh"

MODE="all"
CI_MODE=false
for arg in "$@"; do
  case "$arg" in
    --backend) MODE="backend" ;;
    --frontend) MODE="frontend" ;;
    --fast) MODE="fast" ;;
    --ci) CI_MODE=true ;;
    -h|--help)
      sed -n '2,12p' "$0"
      exit 0
      ;;
    *)
      die "Argumento desconocido: $arg (usa --backend, --frontend, --fast, --ci)"
      ;;
  esac
done

mkdir -p "$LOG_DIR"
FAILURES=0
STEP_NAMES=()
STEP_RESULTS=()
STEP_LOGS=()

slugify() { printf '%s' "$1" | tr '[:upper:] ' '[:lower:]-' | tr -cd 'a-z0-9.-'; }

run_step() {
  local name="$1"
  shift
  local slug log
  slug="$(slugify "$name")"
  log="$LOG_DIR/$slug.log"
  log_step "$name"
  if "$@" >"$log" 2>&1; then
    log_ok "$(tail -n 1 "$log" | cut -c1-160)"
    STEP_NAMES+=("$name")
    STEP_RESULTS+=("PASS")
    STEP_LOGS+=("$log")
  else
    log_fail "$name"
    printf '%s--- últimas líneas de %s ---%s\n' "$C_DIM" "$log" "$C_RESET"
    tail -n 40 "$log"
    STEP_NAMES+=("$name")
    STEP_RESULTS+=("FAIL")
    STEP_LOGS+=("$log")
    FAILURES=$((FAILURES + 1))
  fi
}

backend_tests() {
  local total=0
  if compgen -G "$ROOT/backend/target/surefire-reports/*.txt" >/dev/null; then
    total="$(grep -h '^Tests run:' "$ROOT"/backend/target/surefire-reports/*.txt \
      | awk -F'[,: ]+' '{t += $3} END {print t + 0}')"
  fi
  printf '%s' "$total"
}

frontend_tests() {
  local log="$LOG_DIR/frontend-tests.log"
  if [[ -f "$log" ]]; then
    sed -r 's/\x1B\[[0-9;]*[mK]//g' "$log" \
      | grep -Eo 'Tests +[0-9]+ passed' \
      | grep -Eo '[0-9]+' \
      | tail -n 1
  else
    printf '0'
  fi
}

backend_tests_run() {
  local count
  count="$(backend_tests)"
  if [[ "$count" -le 0 ]]; then
    log_fail "Surefire no ejecutó ningún test. Un build sin tests no es evidencia."
    return 1
  fi
  log_info "Backend: $count tests ejecutados"
}

frontend_tests_run() {
  local count
  count="$(frontend_tests)"
  count="${count:-0}"
  if [[ "$count" -le 0 ]]; then
    log_fail "Vitest no ejecutó ningún test. Un build sin tests no es evidencia."
    return 1
  fi
  log_info "Frontend: $count tests ejecutados"
}

check_hooks_installed() {
  local configured
  configured="$(git -C "$ROOT" config --get core.hooksPath || true)"
  if [[ "$configured" != ".githooks" ]]; then
    log_fail "Hooks no instalados. Ejecuta: make hooks"
    return 1
  fi
  local hook
  for hook in "$ROOT"/.githooks/*; do
    if [[ ! -x "$hook" ]]; then
      log_fail "Hook no ejecutable: ${hook#"$ROOT/"}. Ejecuta: make hooks"
      return 1
    fi
  done
}

# --- Pasos -----------------------------------------------------------------

run_harness_checks() {
  "$ROOT/scripts/check-harness.sh" || return 1
  "$ROOT/scripts/check-test-baseline.sh" || return 1
}

run_secrets() {
  run_gitleaks git /repo --redact --verbose --exit-code=1
}

run_docs() {
  require_command python3
  python3 "$ROOT/scripts/check-docs.py"
}

run_seed() {
  require_command python3
  python3 "$ROOT/scripts/validate-seed.py"
}

run_backend_verify() {
  mvn_cmd clean verify || return 1
  backend_tests_run
}

run_backend_lint() {
  mvn_cmd spotless:check checkstyle:check
}

run_frontend_install() {
  frontend_install
}

run_frontend_verify() {
  run_frontend npm run format:check || return 1
  run_frontend npm run lint || return 1
}

run_frontend_tests() {
  run_frontend npm run test:coverage || return 1
  frontend_tests_run
}

run_frontend_build() {
  run_frontend npm run build || return 1
  "$ROOT/scripts/check-bundle-size.sh"
}

run_frontend_lint() {
  run_frontend npm run format:check || return 1
  run_frontend npm run lint || return 1
}

case "$MODE" in
  all)
    require_docker
    if [[ "$CI_MODE" == false ]]; then
      run_step "Hooks instalados" check_hooks_installed
    fi
    run_step "Harness: invariantes" run_harness_checks
    run_step "Secretos: gitleaks" run_secrets
    run_step "Docs: enlaces relativos" run_docs
    run_step "Catálogo: seed" run_seed
    run_step "Backend: mvn verify" run_backend_verify
    run_step "Frontend: instalar dependencias" run_frontend_install
    run_step "Frontend: formato y lint" run_frontend_verify
    run_step "Frontend: tests" run_frontend_tests
    run_step "Frontend: build de producción" run_frontend_build
    ;;
  backend)
    require_docker
    run_step "Harness: invariantes" run_harness_checks
    run_step "Catálogo: seed" run_seed
    run_step "Backend: mvn verify" run_backend_verify
    ;;
  frontend)
    require_docker
    run_step "Harness: invariantes" run_harness_checks
    run_step "Frontend: instalar dependencias" run_frontend_install
    run_step "Frontend: formato y lint" run_frontend_verify
    run_step "Frontend: tests" run_frontend_tests
    run_step "Frontend: build de producción" run_frontend_build
    ;;
  fast)
    if [[ "$CI_MODE" == false ]]; then
      run_step "Hooks instalados" check_hooks_installed
    fi
    run_step "Harness: invariantes" run_harness_checks
    run_step "Secretos: gitleaks" run_secrets
    run_step "Backend: formato y checkstyle" run_backend_lint
    run_step "Frontend: instalar dependencias" run_frontend_install
    run_step "Frontend: formato y lint" run_frontend_lint
    ;;
esac

# --- Evidencia --------------------------------------------------------------

SHA="$(git_sha)"
DIRTY="$(git_dirty)"
BE_TESTS=0
FE_TESTS=0
JAVA_VERSION="no ejecutado (mode=$MODE)"
MVN_VERSION=""
NODE_VERSION="no ejecutado (mode=$MODE)"
NPM_VERSION=""

if [[ "$MODE" != "frontend" ]]; then
  JAVA_VERSION="$(run_backend java -version 2>&1 | head -n 1 || true)"
  MVN_VERSION="$(run_backend mvn -v 2>/dev/null | head -n 1 || true)"
  BE_TESTS="$(backend_tests)"
fi
if [[ "$MODE" != "backend" ]]; then
  NODE_VERSION="$(run_frontend node --version 2>/dev/null || true)"
  NPM_VERSION="$(run_frontend npm --version 2>/dev/null || true)"
  FE_TESTS="$(frontend_tests)"
  FE_TESTS="${FE_TESTS:-0}"
fi
if [[ "$MODE" == "fast" ]]; then
  # --fast no ejecuta tests: no heredes contadores de una ejecución anterior.
  BE_TESTS="no ejecutados"
  FE_TESTS="no ejecutados"
fi

EVIDENCE="$HARNESS_DIR/last-verify.txt"
{
  echo "FERRO VERIFY"
  echo "mode:      $MODE"
  echo "date:      $(date -Iseconds)"
  echo "commit:    $SHA ($DIRTY)"
  echo "backend:   tests=$BE_TESTS  ${JAVA_VERSION:-java=?}  ${MVN_VERSION:-maven=?}"
  echo "frontend:  tests=$FE_TESTS  node=${NODE_VERSION:-?}  npm=${NPM_VERSION:-?}"
  echo "steps:"
  for i in "${!STEP_NAMES[@]}"; do
    echo "  - ${STEP_RESULTS[$i]}  ${STEP_NAMES[$i]}"
  done
  if [[ "$FAILURES" -eq 0 ]]; then
    echo "result:    PASS"
  else
    echo "result:    FAIL ($FAILURES paso(s))"
  fi
} | tee "$EVIDENCE"

if [[ "$MODE" == "fast" && "$FAILURES" -eq 0 ]]; then
  printf '\n%sAVISO:%s --fast no ejecuta tests. NO es evidencia final; usa `make verify`.%s\n' \
    "$C_YELLOW" "$C_RESET" "$C_RESET"
fi

if [[ "$FAILURES" -gt 0 ]]; then
  printf '\n%sVerificación FALLIDA.%s No declares el trabajo terminado. Corrige la causa raíz, no el gate.\n' \
    "$C_RED" "$C_RESET"
  exit 1
fi

printf '\n%sVerificación PASS.%s Evidencia guardada en %s\n' \
  "$C_GREEN" "$C_RESET" "${EVIDENCE#"$ROOT/"}"
