# AGENTS.md — Reglas del harness de Ferro

> Contexto persistente para cualquier agente (o persona) que trabaje en este repo.
> Si este archivo contradice `docs/`, gana `docs/` — y hay que corregir este archivo en el mismo PR.

## 1. Qué es Ferro y dónde está la verdad

Ferro es una PWA de tracking de entrenamientos de fuerza: Angular 22 + Spring Boot 4.1 (Java 21)
+ PostgreSQL 18, monolito modular con DDD táctico y arquitectura hexagonal por bounded context.

Orden de autoridad:

1. `docs/02-requisitos-funcionales.md` (RF + reglas de negocio RN-01…24).
2. `docs/03-requisitos-no-funcionales.md` (RNF).
3. `docs/04-arquitectura.md` §4 (límites entre módulos) y ADRs en `docs/adr/`.
4. `docs/05-modelo-datos.md` y `docs/06-api-rest.md` (contratos).
5. `docs/07-roadmap.md` (alcance del milestone activo).
6. Este archivo (cómo se trabaja y se verifica).

**Estado actual (nov-2026):** documentación v1.0 cerrada y esqueleto inicial ejecutable
(`backend/` + `frontend/`) con gates desde el día 1. **M0 sigue en curso**: aún no hay
endpoints de negocio, persistencia ni servicios de infraestructura. El producto se construye
milestone a milestone; no adelantes funcionalidad de milestones futuros.

## 2. Regla de oro: no hay victoria sin evidencia

Una tarea solo está terminada cuando **`make verify` sale con código 0** y pegas el bloque
`FERRO VERIFY` completo en tu respuesta. Sin excepciones, sin resúmenes inventados.

Prohibido:

- Decir «listo», «funciona», «completado» o «tests pasan» sin haber ejecutado la verificación.
- Usar `make verify-fast` como evidencia final (no ejecuta tests).
- Comentar, borrar o `skip`ear tests para que el build pase.
- Bajar umbrales de cobertura, desactivar Checkstyle/ArchUnit/Spotless o quitar reglas.
- Añadir `|| true`, `continue-on-error`, `@Disabled` o `--no-verify` para esquivar un gate.
- Reducir el baseline de tests (`docs/harness/test-baseline.json`).
- Cambiar el harness y presentarlo como un cambio normal: los gates son parte del producto.
- Afirmar que CI está verde sin haberlo comprobado en GitHub Actions.

Si un gate falla: corrige la **causa raíz**. Si crees que el gate está mal, para, explica el
porqué y cámbialo en un commit aparte, con justificación y sin reducir la protección.

Un test que no se ejecutó no cuenta. Un paso `SKIP` no es un `PASS`. Un build sin tests no es
un build verde (`failIfNoTests=true` + baseline lo impiden).

## 3. Comandos del harness

El host puede no tener JDK ni Node: **todo se ejecuta en Docker** con versiones fijas.

| Comando | Qué hace | Cuándo |
|---|---|---|
| `make doctor` | Diagnóstico de Docker, hooks, cachés, red y puertos | Primera vez y si algo falla |
| `make hooks` | Instala `.githooks` (`core.hooksPath`) | Una vez por clon |
| `make bootstrap` | `doctor` + `hooks` | Primera vez |
| `make fmt` | Spotless (Java) + Prettier (Angular) | Antes de cada commit |
| `make lint` | Formato + Checkstyle + ESLint, sin tests | Iteración rápida |
| `make test` | Tests unitarios backend/frontend | Iteración |
| `make verify` | **Gate completo**: harness, secretos, docs, backend (`mvn verify`), frontend (formato, lint, tests con cobertura, build) | **Antes de declarar terminado** |
| `make verify-fast` | Lint + secretos; **no válido como evidencia** | Pre-commit manual |
| `make verify-backend` / `make verify-frontend` | Verificación de una sola parte | Debug |
| `make dev-up` / `make dev-down` | Postgres 18, Mailpit, Prometheus y Grafana | Desarrollo local |
| `make run-backend` | API en Docker contra la infra local (`SERVER_PORT=8082` si 8080 está ocupado) | Probar la app |
| `make run-frontend` | Angular en Docker en http://localhost:4200 | Probar la UI |
| `make secrets` | gitleaks sobre el historial | Auditoría |
| `make baseline` | Actualiza el baseline tras **añadir** tests | Solo con tests nuevos |
| `make e2e` | Playwright (M1). Hoy falla a propósito | M1+ |

Evidencia: `make verify` escribe `.harness/last-verify.txt` y lo imprime. Pega ese bloque.

## 4. Protocolo de trabajo (obligatorio)

1. **Lee** la historia RF y sus RN/ADR. Identifica criterios de aceptación verificables.
2. **Escribe primero el test** que falla (red). Nombra el test con el comportamiento, no con el método.
3. **Implementa** lo mínimo para ponerlo en verde, respetando la dirección de dependencias.
4. **Formatea**: `make fmt`.
5. **Verifica completo**: `make verify`. Si falla, vuelve al paso 2/3.
6. **Evidencia**: pega el bloque `FERRO VERIFY` y explica qué se verificó y qué queda fuera.
7. **Aprende**: si hubo un bug, un falso verde o un bloqueo, añade una lección (§9).

Cambios pequeños y atómicos, en trunk-based: `main` siempre verde y desplegable. Un PR = una
intención. No mezcles refactor, feature y harness en el mismo commit.

## 5. Definition of Done

- [ ] Criterios de aceptación del RF cubiertos por tests cuyo nombre los hace trazables.
- [ ] Nivel de test correcto (ADR-0007): dominio unitario; integración con Testcontainers;
      ArchUnit/Modulith; E2E Playwright cuando el flujo lo requiera.
- [ ] `mvn verify` verde: tests + Checkstyle + Spotless + ArchUnit + Modulith.
- [ ] Frontend: `format:check`, ESLint, Vitest y build verdes; cobertura ≥ umbral.
- [ ] Sin secretos, sin logs de depuración, sin código comentado, sin `TODO` sin dueño.
- [ ] Docs actualizadas si cambian contrato, datos, reglas o decisiones (o ADR nuevo).
- [ ] Migraciones Flyway forwards-only; nunca editar una migración ya aplicada.
- [ ] Commit en Conventional Commits (`feat(training): …`).
- [ ] `make verify` PASS + bloque de evidencia pegado en la respuesta.

## 6. Reglas de arquitectura (verificadas por ArchUnit + Modulith)

- `domain` no importa Spring, Jakarta Persistence/Validation, Jackson ni capas externas.
- `application` no importa `infrastructure` ni `interfaces`; depende de puertos.
- `infrastructure`/`interfaces` implementan puertos; el dominio nunca los importa.
- Un módulo solo usa de otro su paquete `api` o sus eventos. Nunca `internal`/`domain` ajeno.
- `shared` es un kernel mínimo y abierto; si crece, probablemente el contexto está mal cortado.
- Sin `@Autowired` de campo: constructor (o `@RequiredArgsConstructor`).
- Código e identificadores en inglés; documentación, commits y mensajes de UI en español/inglés
  según i18n. Glosario ES↔EN en `docs/09-glosario.md`.
- No añadas dependencias ni cambies versiones fijadas (Java 21, Spring Boot 4.1.x, Angular 22,
  PostgreSQL 18, Node 24) sin ADR y aprobación explícita.
- Los tests de integración usan **PostgreSQL real 18 en Testcontainers**, nunca H2 (ADR-0007).

## 7. Límites de alcance

- **M0 no implementa endpoints de negocio.** Solo esqueleto, salud, gates e infraestructura.
- No inventes requisitos ni campos: si falta una decisión, pregunta o propone un ADR.
- No implementes «de paso» funcionalidad de milestones posteriores.
- No reescribas documentos de requisitos para que encajen con una implementación cómoda.

## 8. Mapa del harness

```
AGENTS.md                        reglas persistentes (este archivo)
Makefile                         interfaz única de comandos
scripts/verify.sh                gate completo + evidencia
scripts/doctor.sh                diagnóstico del entorno
scripts/check-harness.sh         invariantes de los gates (anti-debilitamiento)
scripts/check-test-baseline.sh   impide borrar tests
scripts/check-docs.py            enlaces relativos de README/docs
scripts/backend.sh|frontend.sh   ejecución Docker con versiones fijas
.githooks/                       pre-commit, commit-msg, pre-push
.github/workflows/ci.yml         misma verificación en CI
infra/docker-compose.dev.yml    Postgres 18, Mailpit, Prometheus y Grafana
infra/docker-compose.prod.yml   API + Postgres + Caddy (borrador M0)
backend/Dockerfile              imagen multi-stage JRE 21, usuario no root
backend/src/main/resources/db/migration/  migraciones Flyway (V1–V5, esquema completo)
frontend/src/app/core/auth/     SessionStore, AuthStore, interceptor y guards
frontend/src/app/features/auth/ login, registro, verificación y recuperación
frontend/vercel.json            CSP y cabeceras del frontend
docs/harness/lessons.md          lecciones → reglas/tests/hooks
docs/harness/test-baseline.json  mínimo de tests exigido
backend/src/test/.../Postgres18ContainerTest.java  humo de integración con PostgreSQL 18 real
.opencode/skills/verificar/      protocolo de verificación para agentes
```

Para **añadir un gate**: crea el script/test, conéctalo en `scripts/verify.sh` y en CI,
documenta el comando en este archivo y añade el invariante a `scripts/check-harness.sh`.
Para **cambiar un gate**: commit separado, explica qué protección se pierde y por qué es aceptable.

## 9. Bucle de aprendizaje (no repetir errores)

Cada bug, falso verde o bloqueo deja una lección en `docs/harness/lessons.md`.
Formato: síntoma → causa raíz → prevención → **artefacto** que lo impide (regla, test, script,
hook o paso de CI) → cómo se comprueba. Una lección sin artefacto no está cerrada.

Al cerrar cada milestone: revisa las lecciones, promociona las recurrentes a reglas de este
archivo o a checks automáticos, y elimina las que ya no apliquen (con justificación).

## 10. OpenCode

- `/verificar` ejecuta el protocolo de verificación (skill `verificar`).
- `/retro` convierte lo aprendido en una lección + artefacto.
- La fuente de verdad es siempre `make verify`, no la memoria del agente.
- Nunca uses `git commit --no-verify` ni `git push --no-verify`.
- Si Docker no está disponible, **no puedes verificar**: declara el bloqueo, no una victoria.

## 11. Checklist antes de responder «terminado»

1. ¿Ejecuté `make verify` completo (no `--fast`) y salió 0?
2. ¿Pegué el bloque `FERRO VERIFY` con commit y contadores de tests?
3. ¿Los criterios de aceptación están cubiertos por tests visibles?
4. ¿Toqué algún gate, baseline o test existente? ¿Está justificado y separado?
5. ¿Queda algún `TODO`, mock temporal, log de debug o secreto?
6. ¿Añadí una lección si aprendí algo que puede repetirse?

Si alguna respuesta es «no», no está terminado.
