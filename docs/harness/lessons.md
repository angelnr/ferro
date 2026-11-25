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

---

## Revisión por milestone

Al cerrar cada milestone (M0…M7): leer las lecciones abiertas, convertir las repetidas en reglas
del harness y anotar en el PR qué lecciones se cierran o se añaden.
