# Lecciones del harness

Registro de errores, falsos verdes y bloqueos. **Una lección solo se cierra cuando se convierte
en un artefacto**: regla en `AGENTS.md`, test, script, hook o paso de CI que impide que vuelva.

## Plantilla

```markdown
### L-00X · Título corto
- **Fecha:** AAAA-MM-DD
- **Síntoma:** qué se observó.
- **Causa raíz:** por qué ocurrió de verdad.
- **Prevención:** qué se cambió.
- **Artefacto:** ruta concreta (regla/test/script/hook/CI) que lo bloquea.
- **Comprobación:** comando o test que demuestra que ya no pasa.
```

---

### L-001 · Victoria declarada sin evidencia
- **Fecha:** 2026-11-25
- **Síntoma:** riesgo de responder «listo» tras cambios que compilan, sin ejecutar la suite completa.
- **Causa raíz:** no existía un único gate obligatorio ni un formato de evidencia verificable.
- **Prevención:** `make verify` como única definición de terminado; bloque `FERRO VERIFY` obligatorio.
- **Artefacto:** `AGENTS.md` §2 y §11; `scripts/verify.sh`; `.githooks/pre-push`.
- **Comprobación:** `make verify` sale 0 y `.harness/last-verify.txt` contiene commit, contadores y PASS.

### L-002 · Build verde sin tests
- **Fecha:** 2026-11-25
- **Síntoma:** un proyecto recién creado puede pasar `mvn verify` / `ng test` sin ejecutar nada.
- **Causa raíz:** Surefire y Vitest no siempre fallan si no encuentran tests; el verde es vacío.
- **Prevención:** `failIfNoTests=true`, guardas de contador > 0 y baseline mínimo versionado.
- **Artefacto:** `backend/pom.xml`; `scripts/verify.sh` (`backend_tests_run`, `frontend_tests_run`);
  `scripts/check-test-baseline.sh`; `docs/harness/test-baseline.json`.
- **Comprobación:** borrar un test hace fallar `check-test-baseline.sh`; no ejecutar tests hace fallar `verify`.

### L-003 · El host no tiene JDK ni Node
- **Fecha:** 2026-11-25
- **Síntoma:** `mvn`/`npm` no existen en el servidor de desarrollo; los agentes no podían validar.
- **Causa raíz:** se asumió un toolchain local que no está instalado.
- **Prevención:** todos los comandos corren en Docker con versiones fijas y cachés persistentes.
- **Artefacto:** `scripts/backend.sh`, `scripts/frontend.sh`, `scripts/lib.sh`; `make doctor`.
- **Comprobación:** `make verify` funciona sin `java`/`mvn`/`node` en el host.

### L-004 · ArchUnit vacío rompe el esqueleto
- **Fecha:** 2026-11-25
- **Síntoma:** las reglas ArchUnit fallaban con «failed to check any classes» en M0, antes de existir código.
- **Causa raíz:** ArchUnit considera error una regla que no evalúa ninguna clase; en el esqueleto hay capas vacías.
- **Prevención:** `allowEmptyShould(true)` **solo** en reglas de capas aún inexistentes, con comentario y revisitadas al crear la capa.
- **Artefacto:** `backend/src/test/java/com/ferro/architecture/HexagonalArchitectureTest.java`.
- **Comprobación:** al añadir el primer `*Controller`, la regla pasa a evaluar clases y deja de ser vacua.

### L-005 · Spring Boot 4 renombra paquetes y starters de test
- **Fecha:** 2026-11-25
- **Síntoma:** imports de Boot 3 (`AutoConfigureMockMvc`, `spring-boot-starter-test` para MVC) no encuentran clase.
- **Causa raíz:** Boot 4 modularizó starters; MVC test vive en `spring-boot-starter-webmvc-test` y
  `org.springframework.boot.webmvc.test.autoconfigure`.
- **Prevención:** usar los starters/paquetes de Boot 4 y no copiar ejemplos de tutoriales pre-4.x.
- **Artefacto:** `backend/pom.xml`; `backend/src/test/java/com/ferro/HealthEndpointTest.java`.
- **Comprobación:** `make verify-backend` compila y ejecuta el test de health.

### L-006 · Checkstyle marca la clase main y los tests como utilidades
- **Fecha:** 2026-11-25
- **Síntoma:** `HideUtilityClassConstructor` fallaba en `FerroApplication` y en la clase ArchUnit.
- **Causa raíz:** heurística de Checkstyle para clases sin constructor ni métodos de instancia.
- **Prevención:** supresiones acotadas a `*Application.java` y `src/test`, no desactivar el check globalmente.
- **Artefacto:** `backend/config/checkstyle/suppressions.xml`.
- **Comprobación:** `make verify-backend` con 0 violaciones y el check sigue activo en producción.

### L-007 · `set -e` no protege funciones dentro de un `if`
- **Fecha:** 2026-11-25
- **Síntoma:** `make verify` reportó PASS mientras `mvn verify` fallaba por Spotless; el paso del backend se marcó verde.
- **Causa raíz:** en Bash, invocar una función como condición (`if run_paso; then`) desactiva `errexit`
  dentro de toda la función. El primer comando falló y el siguiente (`backend_tests_run`) devolvió 0,
  así que el paso pasó y se contaron tests de una ejecución anterior.
- **Prevención:** cada comando de un paso compuesto lleva `|| return 1` explícito; `mvn clean verify`
  elimina reportes viejos antes de contar; el contador exige > 0.
- **Artefacto:** `scripts/verify.sh` (funciones `run_backend_verify`, `run_frontend_verify`,
  `run_frontend_tests`, `run_harness_checks`); `scripts/check-harness.sh` (gates no desactivables).
- **Comprobación:** un `pom.xml` sin formatear hace que `make verify-backend` salga FAIL; antes pasaba.

### L-008 · Testcontainers 2.x renombra sus módulos
- **Fecha:** 2026-11-25
- **Síntoma:** con el BOM de Boot 4.1, `org.testcontainers:postgresql` y `:junit-jupiter` daban 404.
- **Causa raíz:** Testcontainers 2.x cambió los artefactos a `testcontainers-postgresql` y
  `testcontainers-junit-jupiter` (mismo groupId).
- **Prevención:** usar las coordenadas de la versión gestionada por el BOM; no copiar snippets de 1.x.
- **Artefacto:** `backend/pom.xml`; invariante en `scripts/check-harness.sh`.
- **Comprobación:** `Postgres18ContainerTest` arranca `postgres:18-alpine` y consulta su versión.

### L-009 · PostgreSQL 18 cambia el punto de montaje del volumen
- **Fecha:** 2026-11-26
- **Síntoma:** el contenedor `postgres:18-alpine` entraba en bucle de reinicio con «data in /var/lib/postgresql/data (unused mount/volume)».
- **Causa raíz:** desde la imagen 18 el volumen debe montarse en `/var/lib/postgresql`; los datos viven en un subdirectorio versionado para permitir `pg_upgrade`.
- **Prevención:** montar el volumen en `/var/lib/postgresql` y arrancar el contenedor con healthcheck.
- **Artefacto:** `infra/docker-compose.dev.yml`; invariante en `scripts/check-harness.sh`.
- **Comprobación:** `make dev-up` deja `ferro-postgres` en `healthy` y `pg_isready` responde.

### L-010 · `citext` no normaliza a minúsculas
- **Fecha:** 2026-11-26
- **Síntoma:** un test esperaba que insertar `Test@Ferro.dev` devolviese `test@ferro.dev`; `citext` devolvía el texto original.
- **Causa raíz:** `citext` hace comparaciones y unicidad case-insensitive, pero **conserva** el valor tal cual se insertó.
- **Prevención:** la normalización NFKC + trim + minúsculas es responsabilidad del VO `Email` (docs/05 §6.5); la BD solo garantiza unicidad case-insensitive.
- **Artefacto:** `backend/src/main/java/com/ferro/identity/domain/Email.java`; `FlywayMigrationsTest` comprueba ambos comportamientos.
- **Comprobación:** `EmailTest` y el test de migraciones pasan con el caso mixto y el duplicado.

### L-011 · Boot 4 modulariza la autoconfiguración de Flyway
- **Fecha:** 2026-11-26
- **Síntoma:** la API arrancaba sin errores contra Postgres, pero no existía `flyway_schema_history` ni se aplicaba `V1`.
- **Causa raíz:** en Boot 4 la autoconfiguración vive en módulos separados; `flyway-core` aporta la librería, pero no `spring-boot-flyway`. Sin ese módulo, Flyway simplemente no se ejecuta (fallo silencioso).
- **Prevención:** usar `spring-boot-starter-flyway` y verificar en un test que el bean `Flyway` existe y que hay migraciones aplicadas.
- **Artefacto:** `backend/pom.xml`; `FlywaySpringIntegrationTest`; invariante en `scripts/check-harness.sh`.
- **Comprobación:** `mvn verify` falla si se quita el starter o si Flyway no aplica `V1` en el contexto Spring.

### L-012 · Maven Central devuelve 429 en CI
- **Fecha:** 2026-11-26
- **Síntoma:** el job de backend falló resolviendo el parent POM con `429 Too Many Requests`; el código estaba bien.
- **Causa raíz:** los runners de GitHub comparten rangos de IP y Central limita peticiones; un runner sin caché descarga cientos de artefactos de golpe.
- **Prevención:** mirror de Central solo para CI (`settings-ci.xml`), misma coordenada/checksum; local sigue usando Central. La caché de Maven reduce el impacto a partir de la segunda ejecución.
- **Artefacto:** `backend/config/maven/settings-ci.xml`; `FERRO_MAVEN_SETTINGS` en `scripts/lib.sh` y `.github/workflows/ci.yml`; invariante en `scripts/check-harness.sh`.
- **Comprobación:** `gh run watch` del workflow CI; resolución de dependencias con un repositorio Maven vacío.

### L-013 · Contradicción entre RN-13 y el CHECK de `session_sets.weight`
- **Fecha:** 2026-11-26
- **Síntoma:** doc 05 limitaba `weight` a 0..1000, pero RN-13 admite lastre negativo en bodyweight (asistencia).
- **Causa raíz:** la tabla del modelo no reflejaba la regla de negocio; un CHECK 0..1000 habría roto dominadas asistidas.
- **Prevención:** permitir `-1000..1000` y validar por ejercicio en la aplicación; doc 05 actualizado.
- **Artefacto:** `V3__training.sql`; invariante en `scripts/check-harness.sh`; test de migraciones.
- **Comprobación:** las migraciones aplican y el test de esquema pasa con el CHECK nuevo.

### L-014 · Una excepción de autenticación no debe revertir el bloqueo/la revocación
- **Fecha:** 2026-11-26
- **Síntoma:** al lanzar `InvalidCredentials` tras un intento fallido, o `RefreshInvalid` tras detectar reutilización, la transacción podía revertir el contador de fallos o la revocación de toda la familia de refresh.
- **Causa raíz:** `@Transactional` hace rollback por defecto ante cualquier `RuntimeException`, incluida la que se usa para devolver 401.
- **Prevención:** `@Transactional(noRollbackFor = …)` en login y refresh para persistir los efectos de seguridad y responder 401/403 en la misma operación.
- **Artefacto:** `LoginService`, `RefreshService`; tests `account_locks_after_five_consecutive_failures` y `refresh_rotates_and_reusing_a_revoked_token_revokes_the_family`.
- **Comprobación:** 5 fallos consecutivos bloquean la cuenta y reutilizar un refresh invalida también el nuevo.

### L-015 · El health de SMTP tumba `/actuator/health`
- **Fecha:** 2026-11-26
- **Síntoma:** todos los tests de contexto pasaban, pero `healthEndpointIsUpWithoutAuthentication` devolvía 503 al añadir `spring-boot-starter-mail`.
- **Causa raíz:** el indicador `mail` de Actuator intenta conectar con SMTP y marca la salud global como DOWN si Mailpit no está disponible (CI no lo levanta).
- **Prevención:** `management.health.mail.enabled=false`; la readiness depende de la BD, no del proveedor de email. Los fallos de envío se loguean y el usuario puede reenviar.
- **Artefacto:** `application.yml`; `MailEmailSender` captura `MailException`; test de health.
- **Comprobación:** `mvn verify` pasa con y sin SMTP disponible.

### L-016 · Gitleaks marca los placeholders de secretos
- **Fecha:** 2026-11-26
- **Síntoma:** el pre-commit bloqueó el commit por `JWT_SECRET=cambia-este-secreto...` en `infra/.env.example`.
- **Causa raíz:** la regla `generic-api-key` detecta alta entropía aunque sea un placeholder.
- **Prevención:** dejar el valor vacío en el ejemplo y explicar cómo generarlo; el compose exige la variable en tiempo de arranque.
- **Artefacto:** `infra/.env.example`; gitleaks en pre-commit/CI.
- **Comprobación:** `gitleaks git --staged` sin hallazgos y `docker compose config` falla si falta `JWT_SECRET`.

### L-017 · Cambiar contraseña no debe cerrar la sesión actual
- **Fecha:** 2026-11-26
- **Síntoma:** al revocar todos los refresh tras un cambio de contraseña, el usuario se quedaba fuera también en el dispositivo desde el que cambiaba.
- **Causa raíz:** se revocaba la familia completa sin distinguir el token de la sesión actual.
- **Prevención:** conservar el hash del refresh de la cookie actual y revocar el resto; si no hay cookie, revocar todas.
- **Artefacto:** `ChangePasswordService`; `revokeAllForUserExcept`; test `changes_password_and_revokes_other_sessions`.
- **Comprobación:** el refresh de la sesión actual sigue funcionando y el de otro dispositivo devuelve 401.

### L-018 · Los tests funcionales no pueden compartir los límites de producción
- **Fecha:** 2026-11-26
- **Síntoma:** el registro de 5/h por IP habría hecho fallar la suite de integración, que crea decenas de usuarios desde la misma IP.
- **Causa raíz:** el rate limiter se activa por defecto y los tests funcionales no son tráfico real.
- **Prevención:** `ferro.rate-limit.enabled=false` en tests de flujo y una clase dedicada con el límite activo para probar el 429.
- **Artefacto:** `RateLimitIntegrationTest`; propiedad en `AuthFlowIntegrationTest`.
- **Comprobación:** la suite normal pasa y el test de rate limit verifica `429 + Retry-After: 60`.

### L-019 · La CSP global rompe Swagger UI
- **Fecha:** 2026-11-26
- **Síntoma:** al aplicar `default-src 'self'` a todas las respuestas, la UI de Swagger dejaba de cargar sus recursos.
- **Causa raíz:** la UI de springdoc necesita scripts/estilos propios y no sigue la CSP pensada para la API JSON.
- **Prevención:** aplicar CSP a la API y excluir `/api/docs` y `/api/api-docs`; la CSP del frontend se define aparte en `vercel.json`.
- **Artefacto:** `SecurityHeadersFilter`; `frontend/vercel.json`; test de cabeceras.
- **Comprobación:** health devuelve las cabeceras y Swagger sigue respondiendo.

### L-020 · `localhost` y `127.0.0.1` son orígenes CORS distintos
- **Fecha:** 2026-11-26
- **Síntoma:** el registro desde `http://127.0.0.1:4200` fallaba con «No se pudo completar la operación», mientras `http://localhost:4200` funcionaba.
- **Causa raíz:** la allowlist CORS solo contenía `http://localhost:4200`; para el navegador son orígenes diferentes.
- **Prevención:** permitir ambos orígenes en desarrollo y probar el preflight explícitamente.
- **Artefacto:** `application.yml`; `SecurityConfig`; test `cors_allows_both_local_frontend_origins`.
- **Comprobación:** `OPTIONS /auth/login` devuelve `Access-Control-Allow-Origin` para los dos.

### L-021 · La cobertura de Angular solo medía los ficheros cargados por los tests
- **Fecha:** 2026-11-26
- **Síntoma:** las páginas lazy no aparecían en el informe de cobertura; el 70 % era un falso verde.
- **Causa raíz:** V8 coverage instrumenta solo módulos importados; las páginas cargadas por router nunca entraban.
- **Prevención:** `coverageInclude: ["src/app/**/*.ts"]` y tests de páginas/store; el umbral ahora cubre todo.
- **Artefacto:** `frontend/angular.json`; `auth-pages.spec.ts`; `auth.store.spec.ts`; invariante del harness.
- **Comprobación:** el informe incluye `features/auth/*` y sigue ≥70 %.

### L-022 · PKCE sin estado en BD: state firmado + code_verifier
- **Fecha:** 2026-11-26
- **Síntoma:** el flujo OAuth necesita conservar el `code_verifier` entre authorize y callback, pero no hay tabla para estados.
- **Causa raíz:** guardar el verifier en memoria rompería con varias instancias; una tabla añade migración y limpieza.
- **Prevención:** state JWT HS256 de 5 min que transporta provider, verifier y redirect_uri; el callback lo valida con el `JwtDecoder`.
- **Artefacto:** `OAuthStateService`, `Pkce`, `OAuthFlowIntegrationTest`.
- **Comprobación:** el callback canjea el code y un state de otro proveedor devuelve 401.

### L-023 · Node Alpine y Playwright glibc no comparten `node_modules`
- **Fecha:** 2026-11-26
- **Síntoma:** `make e2e` con la imagen Playwright falló con «Cannot find native binding» al arrancar Angular.
- **Causa raíz:** los binarios nativos instalados en `node:alpine` son musl; la imagen Playwright es Ubuntu/glibc.
- **Prevención:** copiar el frontend sin `node_modules` a un directorio de trabajo y hacer `npm ci` dentro de la imagen Playwright.
- **Artefacto:** `run_playwright` en `scripts/lib.sh`; CI job `e2e`.
- **Comprobación:** `make e2e` ejecuta 5 tests en Chromium.

### L-024 · La UI no se podía abrir desde otro dispositivo (tailnet/LAN)
- **Fecha:** 2026-11-26
- **Síntoma:** `http://<ip-tailscale>:4200` no respondía aunque Angular estuviera arrancado y `curl` local funcionara.
- **Causa raíz:** tres supuestos de «todo en la misma máquina»: el publish de Docker era `127.0.0.1:4200`, `apiBaseUrl` era `http://localhost:8082` (el `localhost` del dispositivo cliente) y la allowlist CORS/CSRF solo tenía orígenes locales.
- **Prevención:** interfaz de escucha configurable (`FERRO_FRONTEND_BIND`), `apiBaseUrl` derivado de `location.hostname` y allowlist ampliada con las IPv4 reales del host en `run-backend`.
- **Artefacto:** `scripts/frontend.sh`; `scripts/backend.sh` + `dev_frontend_origins` en `scripts/lib.sh`; `frontend/src/environments/environment.ts`; test `environment.spec.ts`.
- **Comprobación:** `make verify`; manualmente `curl -I http://<ip>:4200` y login desde otro dispositivo con la API en `:8082`.

### L-024 · El seed del catálogo no puede ir en V2 si V2 ya está aplicada
- **Fecha:** 2026-11-26
- **Síntoma:** el plan original ponía el seed en V2, pero V2 ya estaba aplicada en dev/CI; editar V2 rompería checksums.
- **Causa raíz:** forwards-only (RNF-40) y una migración de esquema ya publicada.
- **Prevención:** seed en `V6__catalog_seed.sql`, con la tabla de migraciones de doc 05 actualizada.
- **Artefacto:** V6; `docs/05-modelo-datos.md`; validador CI del CSV.
- **Comprobación:** Testcontainers aplica V1–V6 desde una BD vacía y el seed tiene 207 filas.

### L-025 · «press banca» no es una subcadena de «Press de banca»
- **Fecha:** 2026-11-26
- **Síntoma:** la búsqueda literal de RF-021 no encontraba «Press de banca con barra».
- **Causa raíz:** el nombre contiene la stopword «de»; un `LIKE '%press banca%'` no coincide.
- **Prevención:** trocear la consulta en tokens y exigir que todos aparezcan (en cualquiera de los dos idiomas).
- **Artefacto:** `JdbcExerciseRepository`; `ExerciseSearchIntegrationTest`.
- **Comprobación:** «press banca», «banca» y «BENCH» devuelven resultados; «extension» encuentra «Extensión».

### L-026 · En PostgreSQL, un `INSERT` duplicado aborta la transacción
- **Fecha:** 2026-11-26
- **Síntoma:** al intentar abrir una segunda sesión activa, el handler devolvía 500 en vez de 409: no se podía consultar la sesión activa después del `DuplicateKeyException`.
- **Causa raíz:** el error de unicidad deja la transacción PostgreSQL en estado abortado; cualquier consulta posterior falla.
- **Prevención:** comprobar la sesión activa **antes** del `INSERT`; el `catch` de la carrera no vuelve a consultar en esa transacción.
- **Artefacto:** `JdbcTrainingRepository.create`; test `TrainingFlowIntegrationTest`.
- **Comprobación:** dos `POST /workout-sessions` devuelven 201 y 409 con `activeSessionId`.

### L-027 · Un `@RestControllerAdvice` genérico eclipsa los handlers específicos
- **Fecha:** 2026-11-26
- **Síntoma:** `SessionNotFoundException` acababa en 500 porque el consejo de identidad capturaba `Exception` antes.
- **Causa raíz:** el orden de los `@RestControllerAdvice` no está definido por especificidad.
- **Prevención:** los consejos de módulo (`training`, `catalog`) llevan `@Order(HIGHEST_PRECEDENCE)`.
- **Artefacto:** `TrainingExceptionHandler`, `CatalogExceptionHandler`; tests de 404.
- **Comprobación:** una sesión ajena devuelve 404, no 500.

---

## Revisión por milestone

Al cerrar cada milestone (M0…M7): leer las lecciones abiertas, convertir las repetidas en reglas
del harness y anotar en el PR qué lecciones se cierran o se añaden.
