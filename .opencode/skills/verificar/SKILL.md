---
name: Verificar Ferro
description: Protocolo obligatorio de verificación y evidencia antes de declarar una tarea terminada en Ferro. Úsala siempre que termines un cambio, antes de decir "listo", al arreglar un test o al comprobar si el build sigue verde.
---

# Verificar Ferro

No existe «terminado» sin `make verify` en verde y su bloque de evidencia pegado.

## Procedimiento

1. Ejecuta el diagnóstico solo si es la primera vez o algo falla:

   ```bash
   make doctor
   make hooks
   ```

2. Formatea antes de verificar:

   ```bash
   make fmt
   ```

3. Ejecuta el gate completo. **Nunca uses `--fast` como evidencia final**:

   ```bash
   make verify
   ```

4. Comprueba el resultado:
   - Código de salida `0` y `result: PASS`.
   - `.harness/last-verify.txt` contiene commit, modo, contadores y pasos.
   - Backend y frontend tienen `tests > 0`.

5. Pega el bloque `FERRO VERIFY` completo en tu respuesta final, seguido de:
   - qué comportamiento se añadió/corrigió;
   - qué criterios de aceptación cubren los tests;
   - qué queda explícitamente fuera (y por qué no bloquea).

## Si falla

- Lee `.harness/logs/<paso>.log` y corrige la causa raíz.
- No comentes tests, no bajes umbrales, no uses `|| true`, `@Disabled`, `--no-verify`.
- No reduzcas `docs/harness/test-baseline.json`.
- Si el gate es incorrecto, para y propone el cambio en un commit aparte con justificación.

## Prohibido

- Declarar victoria con `make verify-fast`, `make test` o «compila».
- Decir que CI está verde sin verlo en GitHub Actions.
- Ejecutar solo el módulo tocado y presentarlo como verificación completa.
- Cambiar el harness para que pase el cambio.

## Evidencia esperada

```
FERRO VERIFY
mode:      all
date:      ...
commit:    <sha> (clean)
backend:   tests=<n>  Apache Maven ...
frontend:  tests=<n>  node=...
steps:
  - PASS  ...
result:    PASS
```
