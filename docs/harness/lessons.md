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

---

## Revisión por milestone

Al cerrar cada milestone (M0…M7): leer las lecciones abiertas, convertir las repetidas en reglas
del harness y anotar en el PR qué lecciones se cierran o se añaden.
