---
description: Ejecuta el protocolo completo de verificación de Ferro y exige evidencia
---

Carga la skill `verificar` y sigue su procedimiento al pie de la letra para el ámbito: $ARGUMENTS

Requisitos:

1. Ejecuta `make fmt` y después `make verify` (completo, nunca `--fast` como evidencia).
2. Si falla, corrige la causa raíz y repite. No debilites gates ni borres tests.
3. Termina pegando el bloque `FERRO VERIFY` literal de `.harness/last-verify.txt`.
4. Indica qué criterios de aceptación cubren los tests y qué queda fuera.

No respondas «listo» hasta que `make verify` salga 0.
