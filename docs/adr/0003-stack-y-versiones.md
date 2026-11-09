# ADR-0003 · Stack, versiones y monorepo

**Estado:** Aceptada · 2026-11-09 · decidida con el cliente (rondas 1 y 2)

## Contexto

Elección inicial del cliente: Angular + Java + Spring Boot + Docker + PostgreSQL.
Verificado el estado del arte en nov-2026 (docs oficiales Spring/Angular) y las
preferencias del cliente (Java 21 LTS por madurez de documentación, Maven por
mercado español, monorepo público para el CV).

## Decisión

| Pieza | Elección | Versión fijada en M0 |
|---|---|---|
| Frontend | Angular (standalone, signals, zoneless, OnPush por defecto, Signal Forms, Vitest) | 22.x (activa hasta jun-2027) |
| CSS/UI | Tailwind CSS + componentes propios (dark-first) | Tailwind 4.x |
| Estado | NgRx SignalStore | última compatible |
| i18n | @ngx-translate (runtime) — ver ADR-0008 | — |
| Backend | Java LTS + Spring Boot (Framework 7, Jakarta EE 11, Tomcat 11, JUnit) | Java **21 LTS** + Spring Boot **4.1.x** |
| Build | Maven | 3.9.x |
| ORM | Spring Data JPA (Hibernate 7) para escritura; SQL nativo para stats (ADR-0005) | — |
| Migraciones | Flyway (forwards-only, RNF-40) | gestionada por BOM |
| BD | PostgreSQL | 18.x (imagen oficial multi-arch) |
| Docs API | springdoc-openapi (OpenAPI 3.1) | compatible Boot 4 |
| Rate limit | bucket4j | 8.x |
| Tests BE | JUnit, Mockito, AssertJ, Testcontainers, ArchUnit | — |
| Mutación | PIT (job semanal) | — |
| Tests FE | Vitest (default Angular ≥21) + Testing Library | — |
| E2E | Playwright (Chromium + WebKit móvil) | — |
| Infra | Docker multi-stage (Temurin JRE 21), Compose dev/prod, GHCR, GitHub Actions | — |
| Local dev JDK/Node | SDKMAN (Temurin 21) + nvm (Node 24 LTS) en el servidor de desarrollo | — |

**Monorepo** (una sola repo pública, `backend/` + `frontend/` + `infra/` + `docs/`):
el CV cuenta una historia completa en un enlace; CI por carpeta con `paths` filters.

## Alternativas descartadas

| Alternativa | Por qué no |
|---|---|
| Java 25 LTS | Perfectamente soportado (Spring 17–27), pero el cliente prefiere 21 por volumen de referencias; coste cero real en este proyecto |
| Gradle | Más rápido; Maven domina las ofertas de empleo del mercado objetivo del cliente |
| React/Vue | El cliente trae camino andado en Angular; migrar framework no suma al objetivo CV |
| PrimeNG / Angular Material | Material: look genérico difícil de alejar de «Google app»; PrimeNG: potente pero estética propia difícil de tunear; Tailwind+propios da el look premium oscuro tipo Hevy que el producto pide |
| Repos separados por app | Fragmenta la narrativa del portfolio y duplica CI/CD |

## Consecuencias

- **Positivas:** todo «moderno y actual» de ambas plataformas es aprovechable
  (zoneless, signals, Vitest, Signal Forms / Spring 7, Jackson 3, virtual threads
  si se necesitan); una sola URL para el CV.
- **Negativas:** monorepo obliga a CI condicional por rutas; Angular cambia major
  anualmente (siguiente jun-2027) — asumido con ventana de soporte v22 ≥ 1 año.
- **Nota de riesgo:** Spring Boot 4.1 con Jackson 3 y JUnit 6 rompe tutoriales
  antiguos (código pre-4.x con javax/jackson2 no compila tal cual); se asume y se
  documenta en el README de desarrollo.
