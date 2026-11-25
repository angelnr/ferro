---
description: Convierte un error o falso verde en una lección con artefacto anti-repetición
---

Haz una retroalimentación del harness sobre: $ARGUMENTS

1. Añade una entrada a `docs/harness/lessons.md` usando la plantilla (síntoma, causa raíz,
   prevención, artefacto, comprobación). Numera la siguiente L-00X.
2. Convierte la lección en al menos un artefacto real:
   - regla en `AGENTS.md`, o
   - test que reproduce el fallo, o
   - check en `scripts/check-harness.sh` / `scripts/verify.sh`, o
   - hook en `.githooks/`, o
   - paso de CI.
3. Si tocas el harness, actualiza `scripts/check-harness.sh` para que el invariante quede protegido.
4. Ejecuta `make verify` y adjunta la evidencia.

Una lección sin artefacto verificable no está cerrada.
