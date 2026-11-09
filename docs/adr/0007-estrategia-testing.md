# ADR-0007 · Estrategia de testing: cinco niveles con gates en CI

**Estado:** Aceptada · 2026-11-09 · decidida con el cliente (ronda 2: «exhaustiva»)

## Contexto

El objetivo CV exige que la calidad sea demostrable, no declarada. El cliente eligió
el nivel más alto de automatización (unit + Testcontainers + ArchUnit + Playwright + PIT).

## Decisión

| Nivel | Herramientas | Qué cubre | Gate |
|---|---|---|---|
| 1. Unit dominio | JUnit + AssertJ (+ Mockito solo en application) | Agregados, VOs, invariantes RN-01…24, cálculos (e1RM, volumen, PRs, streak) | Cada PR; rápido (< 10 s) |
| 2. Integración | Spring Boot Test + **Testcontainers** (Postgres real 18) | Repos, SQL nativo, migraciones Flyway, seguridad (anti-IDOR, rotación refresh, rate limit), endpoints con MockMvc/WebTestClient | Cada PR |
| 3. Arquitectura | **ArchUnit** + Spring Modulith verification | Reglas §4 doc 04 (hexagonal, límites entre módulos) | Cada PR — **build rojo** (RNF-44) |
| 4. E2E | **Playwright** (Chromium desktop + móvil; WebKit iOS-sim) + **axe-core** | Happy paths críticos: registro→verificación→rutina→sesión completa→finish→PRs→stats; OAuth mockeado; body; i18n/a11y | Cada PR en suite corta; nocturna completa |
| 5. Mutación | **PIT** (paquetes `..domain..`) | Calidad real de los asserts del dominio | Job semanal; objetivo ≥ 70 % (RNF-43) |

Cobertura: JaCoCo — ≥ 80 % en `identity`/`training`/`progress`, ≥ 70 % global
(RNF-41/42); frontend: Vitest + Testing Library en stores/componentes clave ≥ 70 %
de stores (RNF-45). Contract: la OpenAPI generada (springdoc) se publica como artefacto
del build y se diff-ea en CI para detectar cambios involuntarios (RNF-50).

## Alternativas descartadas

| Alternativa | Por qué no |
|---|---|
| Tests de integración con H2 | No reproduce pg_trgm/índices parciales/uuidv7; daría falsos verdes exactamente en el SQL más delicado (ADR-0005) |
| Contract testing con Pact | No hay consumidores externos del API en MVP |
| Cypress | Playwright mejor multi-navegador/paralelismo y una sola herramienta para a11y (axe) |

## Consecuencias

- **Positivas:** refactors valientes desde el día 1; el dominio sin Spring arranca
  en ms (feedback rápido); la regla ArchUnit convierte la arquitectura en un hecho
  verificable, no una diapositiva.
- **Negativas:** CI más lento (Testcontainers) → se mitiga reusing containers y
  paralelizando; PIT semanal en vez de por-PR para no frenar el flujo.
- **Disciplina:** cada bug de producción debe terminar en un test (regla de proyecto).
