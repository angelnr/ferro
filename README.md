# 🏋️ Ferro — Tracker de entrenamientos de fuerza

> **Ferro** (del latín *ferrum*, hierro): registra tus sesiones de gimnasio, vigila tus
> progresiones y convierte cada serie en datos. Una alternativa open source a
> [Hevy](https://hevyapp.com), construida como proyecto de portfolio de nivel profesional.

[![CI](https://github.com/angelnr/ferro/actions/workflows/ci.yml/badge.svg)](https://github.com/angelnr/ferro/actions/workflows/ci.yml)
[![Licencia](https://img.shields.io/badge/licencia-MIT-blue)](./LICENSE)
[![Estado](https://img.shields.io/badge/estado-M0%20en%20curso-orange)](./docs/07-roadmap.md)

---

## ¿Qué es?

Aplicación web multiusuario (PWA) para hacer **tracking de entrenamientos de fuerza e
hipertrofia**: logging de series (reps, peso, RIR), supersets y drop sets, temporizador de
descanso, rutinas reutilizables, historial con récords personales (PRs), estadísticas de
progreso y mediciones corporales. Sin red social: el foco es el dominio.

| Rol | Descripción |
|---|---|
| **Usuario final** | Cualquier persona que entrene con pesas y quiera registrar y analizar su progreso |
| **Reclutador técnico** | Puede leer la documentación, la arquitectura y el código como muestra de ingeniería |

## Stack (decisión cerrada — ver [ADRs](./docs/adr/))

| Capa | Tecnología |
|---|---|
| Frontend | Angular 22 (standalone, signals, zoneless, Signal Forms) · Tailwind CSS 4 · NgRx SignalStore · PWA · Vitest |
| Backend | Java 21 LTS · Spring Boot 4.1 (Framework 7, Jakarta EE 11) · Maven · arquitectura hexagonal + DDD táctico · Spring Modulith |
| API | REST `/api/v1` · OpenAPI 3 (springdoc) · errores RFC 7807 |
| Base de datos | PostgreSQL 18 · Flyway · JPA (escrituras) + SQL nativo (estadísticas) |
| Seguridad | Spring Security · Argon2id · JWT (access + refresh con rotación) · OAuth2 Google/GitHub |
| Tests | JUnit · Mockito · Testcontainers · ArchUnit · Playwright · PIT (mutación) |
| Infra | Docker + Compose · GitHub Actions (CI/CD) · Vercel (frontend) · Oracle Cloud Always Free ARM (backend + BD) · Prometheus + Grafana |

## Arquitectura en una imagen

```mermaid
flowchart LR
    U([Usuario]) -->|HTTPS| FE[Ferro PWA · Angular<br/>Vercel]
    U -->|HTTPS| LB[Caddy · TLS]
    LB --> API[Ferro API · Spring Boot<br/>Oracle Cloud ARM]
    API --> PG[(PostgreSQL 18)]
    PROM[Prometheus] --> API
    GRAF[Grafana] --> PROM
    API -->|SMTP| MAIL[Brevo / Mailpit dev]
    API <-->|OAuth2| GOO[Google · GitHub]
```

Monolito modular con seis bounded contexts: `identity`, `catalog`, `training`,
`progress`, `body` y `platform`. Detalle completo en
[docs/04-arquitectura.md](./docs/04-arquitectura.md).

## Documentación

| Documento | Contenido |
|---|---|
| [01 · Visión de producto](./docs/01-vision-producto.md) | Problema, objetivos, personas, alcance IN/OUT, riesgos |
| [02 · Requisitos funcionales](./docs/02-requisitos-funcionales.md) | Historias de usuario con criterios de aceptación y reglas de negocio |
| [03 · Requisitos no funcionales](./docs/03-requisitos-no-funcionales.md) | Rendimiento, seguridad, RGPD, a11y, i18n, observabilidad |
| [04 · Arquitectura](./docs/04-arquitectura.md) | C4, bounded contexts, hexagonal, eventos, despliegue |
| [05 · Modelo de datos](./docs/05-modelo-datos.md) | ER, tablas, índices, enums, seeds |
| [06 · API REST](./docs/06-api-rest.md) | Contrato de endpoints, ejemplos, errores, paginación |
| [07 · Roadmap](./docs/07-roadmap.md) | Milestones M0–M7 (8 semanas) con Definition of Done |
| [08 · Backlog v2](./docs/08-backlog-v2.md) | Funcionalidad deliberadamente fuera del MVP |
| [09 · Glosario](./docs/09-glosario.md) | Lenguaje ubicuo ES↔EN del dominio |
| [AGENTS.md](./AGENTS.md) | Harness: reglas de trabajo, gates y protocolo de evidencia |
| [infra/README.md](./infra/README.md) | Servicios locales, Compose de producción y operación |
| [Lecciones del harness](./docs/harness/lessons.md) | Errores → artefactos que impiden repetirlos |
| [ADR-0001…0009](./docs/adr/) | Decisiones de arquitectura justificadas |

## Estado del proyecto

**✅ M0 completado** (tag `v0.1.0`) **y M1 completado** (tag `v0.2.0`, 26-nov-2026).
Identidad completa: registro/login/refresh, verificación de email, perfil, contraseñas,
borrado, rate limiting, CSP, OAuth Google/GitHub con PKCE y UI Angular con E2E Playwright.
Para usar OAuth en real solo hay que configurar las credenciales de las apps; sin ellas los
botones quedan ocultos.

**✅ M2 completado** (tag `v0.3.0`, 26-nov-2026). Catálogo con 207 ejercicios sembrados,
búsqueda sin acentos en ES/EN, filtros, paginación, CRUD/archivado custom, estadísticas
reales de detalle (PRs, volumen, e1RM y serie de 52 semanas), combobox accesible con debounce
y creación inline.

**M3 en curso:** sesión en vivo con creación, ejercicios, series inmediatas, volumen, e1RM,
PRs, descarte y primera UI de entrenamiento. Faltan temporizador, reordenación/supersets,
auto-cierre 24 h e historial.

## Desarrollo local y verificación

El harness **no necesita JDK ni Node en el host**: ejecuta versiones fijas en Docker
(Java 21 + Maven 3.9, Node 24).

```bash
make bootstrap    # diagnóstico del entorno + instalación de git hooks
make dev-up       # Postgres 18 + Mailpit + Prometheus + Grafana
make run-backend  # API en http://localhost:8080 (Spring Boot + Flyway)
make run-frontend # Angular en http://localhost:4200
make verify       # gate completo + bloque de evidencia
```

Servicios de desarrollo:

| Servicio | URL | Credenciales |
|---|---|---|
| API | http://localhost:8080/actuator/health | — |
| Swagger | http://localhost:8080/api/docs | — |
| Mailpit | http://localhost:8025 | — |
| Prometheus | http://localhost:9090 | — |
| Grafana | http://localhost:3003 | `admin` / `ferro` |

Si el 8080 está ocupado: `SERVER_PORT=8082 make run-backend` (y ajusta el target de
Prometheus en `infra/prometheus/prometheus.yml` si quieres scrapear ese puerto). Las
migraciones Flyway se aplican solas al arrancar; para resetear la BD en local usa
`docker compose -f infra/docker-compose.dev.yml down -v`.

### OAuth en local

Los botones de Google/GitHub solo aparecen si el backend tiene credenciales. Crea las apps
OAuth con redirect URI `http://localhost:4200/auth/oauth/callback/google` (y `.../github`)
y exporta:

```bash
GOOGLE_CLIENT_ID=... GOOGLE_CLIENT_SECRET=... make run-backend
GITHUB_CLIENT_ID=... GITHUB_CLIENT_SECRET=... make run-backend
```

Sin credenciales, `GET /api/v1/auth/oauth/providers` devuelve ambos proveedores a `false` y
los botones quedan ocultos. GitHub con email privado devuelve `OAUTH_EMAIL_REQUIRED`; el paso
de completar el email en la UI queda pendiente.

### Acceso desde otros dispositivos (tailnet/LAN)

Por defecto el dev server solo escucha en `127.0.0.1`. Para abrirlo a tu tailnet o LAN:

```bash
FERRO_FRONTEND_BIND=0.0.0.0 make run-frontend   # http://<ip-del-servidor>:4200
make run-backend                                # añade las IPv4 del host a CORS/CSRF
```

- `make run-backend` amplía la allowlist con todas las IPv4 del host en el puerto 4200
  (incluida la de Tailscale), así que no hay que editar `application.yml`.
- En desarrollo `apiBaseUrl` se calcula con el hostname del navegador:
  `http://<ip-del-servidor>:8082/api/v1`. Si cambias el puerto de la API con `SERVER_PORT`,
  ajusta `DEV_API_PORT` en `frontend/src/environments/environment.ts`.
- `APP_FRONTEND_URL=http://<ip-del-servidor>:4200 make run-backend` corrige los enlaces de los
  emails (verificación, reset) cuando los abres desde otro dispositivo.
- OAuth sigue necesitando redirect URI públicos registrados en Google/GitHub; desde la tailnet
  puedes añadir `http://<ip-del-servidor>:4200/auth/oauth/callback/<proveedor>` en el proveedor
  y `GOOGLE_REDIRECT_URI`/`GITHUB_REDIRECT_URI`.
- Si el puerto no responde, revisa el firewall del host (p. ej. `sudo ufw allow in on tailscale0
  to any port 4200`).

| Comando | Para qué |
|---|---|
| `make doctor` | Comprueba Docker, hooks, cachés, red y puertos |
| `make fmt` | Aplica Spotless (Java) y Prettier (Angular) |
| `make test` | Tests unitarios e integración (Testcontainers) |
| `make verify` | **Definition of Done**: backend `mvn clean verify`, frontend completo, gitleaks, docs e invariantes del harness |
| `make verify-backend` / `make verify-frontend` | Verificación de una sola parte |
| `make dev-up` / `make dev-down` | Levanta/para la infraestructura local |
| `make run-frontend` | Angular en Docker (http://localhost:4200) |

`make verify` sale 0 **solo** si todos los gates pasan, deja la evidencia en
`.harness/last-verify.txt` y la imprime. El protocolo completo está en [AGENTS.md](./AGENTS.md).

Estado actual: **M0 en curso**. Ya existen esqueleto backend/frontend, harness, CI,
infraestructura local, esquema Flyway completo `V1–V5` y Dockerfile/Compose de producción
(borrador). Faltan SignalStore/i18n base, registro de puertos y el tag `v0.1.0`.



## Licencia

[MIT](./LICENSE) · © 2026 Ángel ([@angelnr](https://github.com/angelnr))
