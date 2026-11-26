# ADR-0010 · `@axe-core/playwright` como dependencia solo de test

**Estado:** Aceptada · 2026-11-27

## Contexto

RNF-28 exige WCAG 2.1 AA y el roadmap M6 pide axe-core en E2E sin violaciones
críticas. Implementar un motor de accesibilidad propio sería incompleto y costoso.

## Decisión

Añadir `@axe-core/playwright` como **devDependency** fijada, usada únicamente en la
suite Playwright. No entra en el bundle de producción ni en la imagen de la API.

## Consecuencias

- **Positivas:** gate real de accesibilidad en CI, mantenido por Deque.
- **Negativas:** una dependencia más de desarrollo; se revisará al actualizar Playwright.
- **Verificación:** test E2E que falla ante violaciones `critical`.
