#!/usr/bin/env bash
# Invariantes del harness. Si alguien "arregla" un build debilitando gates, esto falla.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
errors=0

fail() {
  printf 'FAIL  %s\n' "$*" >&2
  errors=$((errors + 1))
}

need_file() {
  [[ -f "$ROOT/$1" ]] || fail "falta $1"
}

need_exec() {
  [[ -x "$ROOT/$1" ]] || fail "$1 debe ser ejecutable (make hooks)"
}

need_grep() {
  local file="$1" pattern="$2" label="$3"
  grep -Eq "$pattern" "$ROOT/$file" || fail "$file no contiene $label"
}

for f in \
  AGENTS.md \
  Makefile \
  backend/pom.xml \
  backend/src/test/java/com/ferro/infrastructure/Postgres18ContainerTest.java \
  backend/src/test/java/com/ferro/support/AbstractPostgresIntegrationTest.java \
  backend/src/test/java/com/ferro/infrastructure/FlywayMigrationsTest.java \
  backend/src/test/java/com/ferro/infrastructure/FlywaySpringIntegrationTest.java \
  backend/src/main/resources/db/migration/V1__identity_and_extensions.sql \
  infra/docker-compose.dev.yml \
  frontend/package.json \
  frontend/angular.json \
  docs/harness/lessons.md \
  docs/harness/test-baseline.json \
  .githooks/pre-commit \
  .githooks/pre-push \
  .githooks/commit-msg \
  scripts/verify.sh \
  scripts/check-docs.py \
  scripts/check-test-baseline.sh; do
  need_file "$f"
done

for f in .githooks/pre-commit .githooks/pre-push .githooks/commit-msg scripts/verify.sh scripts/backend.sh scripts/frontend.sh; do
  need_exec "$f"
done

need_grep backend/pom.xml '<failIfNoTests>true</failIfNoTests>' 'failIfNoTests=true'
need_grep backend/pom.xml 'maven-checkstyle-plugin' 'checkstyle'
need_grep backend/pom.xml 'spotless-maven-plugin' 'spotless'
need_grep backend/pom.xml 'jacoco-maven-plugin' 'jacoco'
need_grep backend/pom.xml 'COVEREDRATIO' 'umbral de cobertura JaCoCo'
need_grep backend/pom.xml 'testcontainers-postgresql' 'Testcontainers PostgreSQL'
need_grep backend/pom.xml 'spring-boot-starter-flyway' 'autoconfiguración de Flyway de Boot 4'
need_grep backend/pom.xml 'flyway-database-postgresql' 'módulo Flyway PostgreSQL'
need_grep backend/src/test/java/com/ferro/support/AbstractPostgresIntegrationTest.java 'postgres:18-alpine' 'PostgreSQL real 18 (no H2)'
need_grep infra/docker-compose.dev.yml 'postgres:18-alpine' 'Postgres 18 en Compose'
need_grep infra/docker-compose.dev.yml 'ferro_pgdata:/var/lib/postgresql' 'volumen de Postgres 18 en /var/lib/postgresql'
need_grep backend/pom.xml 'maven-enforcer-plugin' 'enforcer'
need_grep backend/pom.xml '<java.version>21</java.version>' 'Java 21'
need_grep frontend/package.json '"test:coverage"' 'script test:coverage'
need_grep frontend/package.json '"format:check"' 'script format:check'
need_grep frontend/package.json '"lint"' 'script lint'
need_grep frontend/angular.json '"coverageThresholds"' 'umbrales de cobertura'

python3 - "$ROOT/frontend/angular.json" <<'PY'
import json, sys
path = sys.argv[1]
data = json.load(open(path))
options = data["projects"]["ferro"]["architect"]["test"]["options"]
thresholds = options.get("coverageThresholds", {})
for metric in ("statements", "branches", "functions", "lines"):
    if thresholds.get(metric, 0) < 70:
        print(f"FAIL  angular.json: umbral {metric} < 70", file=sys.stderr)
        sys.exit(1)
PY

# Ningún gate debe poder desactivarse en silencio.
if grep -R "continue-on-error" "$ROOT/.github" >/dev/null 2>&1; then
  fail "CI usa continue-on-error (un gate no puede ser opcional)"
fi
if grep -RInE '@Disabled|\.skip\(|xit\(|xdescribe\(|\.only\(|fit\(|fdescribe\(' \
  "$ROOT/backend/src/test" "$ROOT/frontend/src" --include='*.java' --include='*.ts' >/dev/null 2>&1; then
  fail "hay tests desactivados o focalizados (@Disabled/.skip/.only): no cuentan como evidencia"
fi
if grep -R "no-verify" "$ROOT/.githooks" "$ROOT/scripts" --exclude='check-harness.sh' >/dev/null 2>&1; then
  fail "algún script/hook intenta saltarse los hooks (--no-verify)"
fi

if [[ "$errors" -ne 0 ]]; then
  printf '\n%s invariante(s) del harness roto(s).\n' "$errors" >&2
  printf 'Los gates son parte del producto: si necesitas cambiarlos, hazlo explícito y justificado.\n' >&2
  exit 1
fi

echo "Harness OK: gates, hooks, baseline y scripts presentes."
