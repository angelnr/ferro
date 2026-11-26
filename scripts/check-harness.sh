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
  backend/Dockerfile \
  backend/.dockerignore \
  backend/config/maven/settings-ci.xml \
  backend/src/test/java/com/ferro/infrastructure/Postgres18ContainerTest.java \
  backend/src/test/java/com/ferro/support/AbstractPostgresIntegrationTest.java \
  backend/src/test/java/com/ferro/infrastructure/FlywayMigrationsTest.java \
  backend/src/test/java/com/ferro/infrastructure/FlywaySpringIntegrationTest.java \
  backend/src/test/java/com/ferro/identity/interfaces/rest/AuthFlowIntegrationTest.java \
  backend/src/main/java/com/ferro/identity/interfaces/rest/EmailVerificationController.java \
  backend/src/main/java/com/ferro/identity/application/EmailVerificationService.java \
  backend/src/main/java/com/ferro/identity/infrastructure/mail/MailEmailSender.java \
  backend/src/main/java/com/ferro/platform/security/OpenApiConfig.java \
  backend/src/main/java/com/ferro/identity/interfaces/rest/UserController.java \
  backend/src/main/java/com/ferro/identity/interfaces/rest/PasswordResetController.java \
  backend/src/main/java/com/ferro/identity/interfaces/rest/OAuthController.java \
  backend/src/main/java/com/ferro/identity/interfaces/rest/OAuthCallbackResponse.java \
  backend/src/main/java/com/ferro/identity/application/oauth/OAuthLoginService.java \
  backend/src/main/java/com/ferro/identity/application/oauth/OAuthStateService.java \
  backend/src/main/java/com/ferro/identity/infrastructure/oauth/GoogleOAuthClient.java \
  backend/src/main/java/com/ferro/identity/infrastructure/oauth/GitHubOAuthClient.java \
  backend/src/test/java/com/ferro/identity/interfaces/rest/OAuthFlowIntegrationTest.java \
  backend/src/main/java/com/ferro/catalog/interfaces/rest/ExerciseController.java \
  backend/src/main/java/com/ferro/catalog/infrastructure/JdbcExerciseRepository.java \
  backend/src/test/java/com/ferro/catalog/interfaces/rest/ExerciseSearchIntegrationTest.java \
  backend/src/test/java/com/ferro/catalog/interfaces/rest/ExerciseCrudIntegrationTest.java \
  backend/src/test/java/com/ferro/catalog/interfaces/rest/ExerciseStatsIntegrationTest.java \
  backend/src/main/java/com/ferro/catalog/infrastructure/JdbcExerciseStatsRepository.java \
  backend/src/main/java/com/ferro/training/interfaces/rest/TrainingController.java \
  backend/src/main/java/com/ferro/training/application/TrainingService.java \
  backend/src/main/java/com/ferro/training/infrastructure/JdbcTrainingRepository.java \
  backend/src/test/java/com/ferro/training/interfaces/rest/TrainingFlowIntegrationTest.java \
  backend/src/test/java/com/ferro/training/interfaces/rest/TrainingMaintenanceIntegrationTest.java \
  backend/src/main/java/com/ferro/training/application/TrainingMaintenanceService.java \
  backend/src/main/java/com/ferro/platform/api/EmailNotifier.java \
  backend/src/main/java/com/ferro/catalog/application/ExerciseCatalogService.java \
  backend/src/main/java/com/ferro/identity/application/PasswordResetService.java \
  backend/src/main/java/com/ferro/platform/api/RateLimiter.java \
  backend/src/main/java/com/ferro/platform/ratelimit/RateLimitFilter.java \
  backend/src/main/java/com/ferro/platform/security/SecurityHeadersFilter.java \
  backend/src/test/java/com/ferro/platform/ratelimit/RateLimitIntegrationTest.java \
  frontend/vercel.json \
  backend/src/main/resources/security/common-passwords.txt \
  backend/src/main/resources/db/migration/V1__identity_and_extensions.sql \
  backend/src/main/resources/db/migration/V2__catalog.sql \
  backend/src/main/resources/db/migration/V3__training.sql \
  backend/src/main/resources/db/migration/V4__progress_and_body.sql \
  backend/src/main/resources/db/migration/V5__feature_flags.sql \
  backend/src/main/resources/db/migration/V6__catalog_seed.sql \
  docs/seed-ejercicios.csv \
  scripts/validate-seed.py \
  infra/docker-compose.dev.yml \
  infra/docker-compose.prod.yml \
  infra/caddy/Caddyfile \
  infra/.env.example \
  frontend/package.json \
  frontend/angular.json \
  frontend/src/app/core/store/ui.store.ts \
  frontend/src/app/core/auth/auth.store.ts \
  frontend/src/app/core/auth/session.store.ts \
  frontend/src/app/core/auth/auth.interceptor.ts \
  frontend/src/app/core/auth/auth.guard.ts \
  frontend/src/app/features/auth/login/login.page.ts \
  frontend/src/app/features/auth/register/register.page.ts \
  frontend/src/app/features/auth/oauth-buttons/oauth-buttons.ts \
  frontend/src/app/features/auth/oauth-callback/oauth-callback.page.ts \
  frontend/src/app/features/settings/settings.page.ts \
  frontend/src/app/features/catalog/catalog.page.ts \
  frontend/src/app/features/catalog/catalog.page.spec.ts \
  frontend/src/app/features/catalog/exercise-picker/exercise-picker.ts \
  frontend/src/app/features/catalog/exercise-detail/exercise-detail.page.ts \
  frontend/src/app/features/training/training.page.ts \
  frontend/src/app/features/training/training.page.spec.ts \
  frontend/src/app/features/training/training.api.ts \
  frontend/e2e/auth.spec.ts \
  frontend/playwright.config.ts \
  frontend/src/app/features/home/home.page.ts \
  frontend/src/app/features/auth/auth-pages.spec.ts \
  frontend/vercel.json \
  frontend/src/app/features/README.md \
  frontend/public/assets/i18n/es.json \
  frontend/public/assets/i18n/en.json \
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

for f in .githooks/pre-commit .githooks/pre-push .githooks/commit-msg scripts/verify.sh scripts/backend.sh scripts/frontend.sh scripts/validate-seed.py; do
  need_exec "$f"
done

need_grep .github/workflows/ci.yml 'FERRO_MAVEN_SETTINGS' 'mirror de Maven en CI'
need_grep backend/pom.xml '<failIfNoTests>true</failIfNoTests>' 'failIfNoTests=true'
need_grep backend/pom.xml 'maven-checkstyle-plugin' 'checkstyle'
need_grep backend/pom.xml 'spotless-maven-plugin' 'spotless'
need_grep backend/pom.xml 'jacoco-maven-plugin' 'jacoco'
need_grep backend/pom.xml 'COVEREDRATIO' 'umbral de cobertura JaCoCo'
need_grep backend/pom.xml 'testcontainers-postgresql' 'Testcontainers PostgreSQL'
need_grep backend/pom.xml 'spring-boot-starter-flyway' 'autoconfiguración de Flyway de Boot 4'
need_grep backend/pom.xml 'spring-boot-starter-oauth2-resource-server' 'resource server JWT'
need_grep backend/pom.xml 'bcprov-jdk18on' 'BouncyCastle para Argon2id'
need_grep backend/pom.xml 'springdoc-openapi-starter-webmvc-ui' 'OpenAPI/Swagger'
need_grep backend/pom.xml 'spring-boot-starter-mail' 'envío de emails'
need_grep backend/src/main/resources/application.yml 'oauth' 'configuración OAuth'
need_grep backend/src/main/java/com/ferro/identity/application/oauth/Pkce.java 'S256|SHA-256' 'PKCE'
need_grep frontend/package.json '@playwright/test' 'Playwright'
need_grep frontend/playwright.config.ts 'webServer' 'E2E con servidor propio'
need_grep Makefile 'e2e:' 'target E2E'
need_grep backend/src/main/resources/application.yml 'api-docs' 'ruta de OpenAPI'
need_grep backend/src/main/java/com/ferro/platform/security/SecurityConfig.java 'email-verification/confirm' 'confirmación pública'
need_grep backend/src/main/java/com/ferro/platform/security/SecurityConfig.java '/api/api-docs' 'OpenAPI público'
need_grep backend/src/main/java/com/ferro/identity/interfaces/rest/UserController.java '/api/v1/users/me' 'contrato de perfil'
need_grep backend/src/main/java/com/ferro/identity/interfaces/rest/PasswordResetController.java '/api/v1/auth/password-reset' 'contrato de recuperación'
need_grep backend/src/main/resources/security/common-passwords.txt '^# Top 10k' 'lista de contraseñas comunes'
need_grep backend/src/main/java/com/ferro/identity/interfaces/rest/OAuthController.java 'complete-email' 'onboarding de email OAuth'
need_grep backend/src/main/java/com/ferro/identity/application/port/EmailSender.java 'sendWelcomeEmail' 'email de bienvenida'
need_grep backend/src/main/java/com/ferro/identity/application/port/EmailSender.java 'sendSecurityAlertEmail' 'email de seguridad'
need_grep frontend/src/app/features/auth/oauth-callback/oauth-callback.page.ts 'needsEmail' 'UI de email OAuth'
need_grep backend/pom.xml 'bucket4j_jdk17-core' 'rate limiting bucket4j'
need_grep backend/src/main/java/com/ferro/catalog/interfaces/rest/ExerciseController.java '/api/v1/exercises' 'contrato de catálogo'
need_grep backend/src/main/resources/db/migration/V6__catalog_seed.sql 'INSERT INTO exercises' 'seed de catálogo'
need_grep frontend/src/app/app.routes.ts 'settings' 'ruta de ajustes'
need_grep frontend/src/app/app.routes.ts 'catalog' 'ruta de catálogo'
need_grep backend/src/main/resources/application.yml 'custom-limit' 'límite de ejercicios custom'
need_grep backend/src/main/java/com/ferro/catalog/infrastructure/JdbcExerciseStatsRepository.java 'personal_records' 'stats de PRs'
need_grep frontend/src/app/features/catalog/exercise-picker/exercise-picker.ts 'role="combobox"' 'combobox accesible'
need_grep backend/src/main/java/com/ferro/training/interfaces/rest/TrainingController.java '/api/v1/workout-sessions' 'contrato de training'
need_grep backend/src/main/java/com/ferro/training/domain/SetMath.java 'e1rm' 'cálculo e1RM'
need_grep frontend/src/app/features/training/training.page.ts 'training.finish' 'UI de sesión en vivo'
need_grep frontend/src/app/features/training/training.page.ts 'restRemaining' 'temporizador de descanso'
need_grep backend/src/main/java/com/ferro/training/application/TrainingMaintenanceService.java 'autoCloseStaleSessions' 'auto-cierre 24 h'
need_grep backend/src/main/java/com/ferro/training/application/TrainingService.java 'groupSuperset' 'supersets'
need_grep frontend/src/app/features/training/training.page.ts 'groupSuperset' 'UI de supersets'
need_grep frontend/src/app/features/training/training.page.ts 'draggable' 'drag & drop de ejercicios'
need_grep frontend/src/app/features/training/training.page.ts 'totalSets' 'resumen de sesión'
need_grep backend/src/main/java/com/ferro/training/interfaces/rest/RoutineController.java 'duplicate' 'CRUD de rutinas'
need_grep backend/src/main/resources/db/migration/V7__session_target_sets.sql 'session_target_sets' 'series objetivo pendientes'
need_grep frontend/src/app/features/routines/routine-builder.page.ts 'targetReps' 'builder de rutinas'
need_grep backend/src/main/java/com/ferro/progress/interfaces/rest/ProgressController.java 'summary' 'panel de progreso'
need_grep backend/src/main/java/com/ferro/body/interfaces/rest/BodyController.java 'body-measurements' 'mediciones corporales'
need_grep frontend/src/app/features/progress/stats.page.ts 'weeklyVolume' 'gráficas de progreso'
need_grep backend/src/main/resources/application.yml 'rate-limit' 'configuración de rate limit'
need_grep backend/src/main/java/com/ferro/platform/security/SecurityHeadersFilter.java 'Content-Security-Policy' 'cabecera CSP'
need_grep frontend/vercel.json 'Content-Security-Policy' 'CSP del frontend'
need_grep infra/caddy/Caddyfile 'Strict-Transport-Security' 'HSTS en Caddy'
need_grep backend/src/main/java/com/ferro/platform/security/SecurityConfig.java 'anyRequest\(\)' 'regla de autenticación global'
need_grep backend/src/main/java/com/ferro/platform/security/SecurityConfig.java 'SessionCreationPolicy.STATELESS' 'API stateless'
need_grep backend/src/main/java/com/ferro/identity/interfaces/rest/AuthController.java '/api/v1/auth' 'contrato de auth'
need_grep backend/pom.xml 'flyway-database-postgresql' 'módulo Flyway PostgreSQL'
need_grep backend/src/test/java/com/ferro/support/AbstractPostgresIntegrationTest.java 'postgres:18-alpine' 'PostgreSQL real 18 (no H2)'
need_grep infra/docker-compose.dev.yml 'postgres:18-alpine' 'Postgres 18 en Compose'
need_grep infra/docker-compose.dev.yml 'ferro_pgdata:/var/lib/postgresql' 'volumen de Postgres 18 en /var/lib/postgresql'
need_grep backend/src/main/resources/db/migration/V3__training.sql 'weight BETWEEN -1000 AND 1000' 'peso negativo para RN-13'
need_grep backend/Dockerfile 'USER ferro' 'imagen backend sin root'
need_grep backend/Dockerfile 'actuator/health/readiness' 'healthcheck de la imagen backend'
need_grep infra/docker-compose.prod.yml 'condition: service_healthy' 'arranque ordenado de Postgres/API'
need_grep infra/docker-compose.prod.yml 'JWT_SECRET' 'secreto JWT obligatorio en producción'
need_grep backend/pom.xml 'maven-enforcer-plugin' 'enforcer'
need_grep backend/pom.xml '<java.version>21</java.version>' 'Java 21'
need_grep frontend/package.json '"test:coverage"' 'script test:coverage'
need_grep frontend/package.json '"format:check"' 'script format:check'
need_grep frontend/package.json '"lint"' 'script lint'
need_grep frontend/package.json '@ngrx/signals' 'SignalStore'
need_grep frontend/package.json '@ngx-translate/core' 'i18n runtime'
need_grep frontend/src/app/app.config.ts 'provideTranslateService' 'i18n configurado'
need_grep frontend/src/app/app.config.ts 'provideAppInitializer' 'restore de sesión al arrancar'
need_grep frontend/src/app/core/auth/auth.store.ts 'signalStore' 'AuthStore con SignalStore'
need_grep frontend/angular.json '"coverageInclude"' 'cobertura sobre todas las fuentes de la app'
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
