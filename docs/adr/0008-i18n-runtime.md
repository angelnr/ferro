# ADR-0008 · i18n en runtime con @ngx-translate (ES + EN)

**Estado:** Aceptada · 2026-11-09 · decidida con el cliente (ronda 1: ES+EN)

## Contexto

La app debe ser ES y EN **conmutables en caliente** y desplegada como una única URL
PWA en Vercel. Angular i18n integrado ($localize) compila **una build por locale**:
dos deploys/URLs o routing por locale, fricción con la PWA (manifest único) y doble
pipeline. Los nombres de ejercicio viven en BD (bilingües por columnas, doc 05).

## Decisión

1. **@ngx-translate** con JSON por locale (`es.json`/`en.json`) y cambio de idioma en
   runtime sin recargar (persistido en el perfil del usuario — RN-19 — y en el store).
2. Fechas/números/plurales con `Intl` nativo + `@ngx-translate` para plurales cuando
   haga falta; `LOCALE_ID` sincronizado para pipes de Angular.
3. Nombres de ejercicios: columnas `name_es`/`name_en` seleccionadas por el locale
   del perfil (API); el locale del perfil manda sobre `Accept-Language`.
4. Errores API y emails: catálogos `es`/`en` en backend por `Accept-Language`/
   perfil (doc 06 §0, RF-084).
5. A11y: `lang` del `<html>` actualizado dinámicamente (lectores de pantalla).

## Alternativas descartadas

| Alternativa | Por qué no |
|---|---|
| Angular i18n integrado (build-time) | Es la vía «oficial», pero exige una build/URL por idioma: rompe la PWA única, duplica el deploy y el switching de idioma se vuelve navegación |
| i18n solo en ES, EN para v2 | El bilingüismo es parte del valor CV y del producto (decisión del cliente) |

## Consecuencias

- **Positivas:** una build, un deploy, un manifest; cambio instantáneo; añadir
  idiomas en v2 es añadir JSON.
- **Negativas:** librería externa (mantenida por la comunidad; riesgo acotado:
  superficie de uso pequeña); no se usa `$localize` para plantillas; los JSON pueden
  desincronizarse entre idiomas → se mitiga con un test de CI que compara claves
  `es` ↔ `en` (fail si falta alguna).
