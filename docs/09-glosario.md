# 09 · Glosario — lenguaje ubicuo (ES ↔ EN)

| Español | Inglés (código/identificadores) | Definición operativa |
|---|---|---|
| Sesión | `WorkoutSession` | Un entrenamiento completo: desde que se pulsa «Empezar» hasta «Finalizar» (o descarte/auto-cierre). Tiene ejercicios ordenados y series |
| Sesión activa | `ACTIVE session` | La única sesión sin finalizar del usuario (RN-08); se recupera al recargar (RN-22) |
| Ejercicio | `Exercise` | Movimiento del catálogo (global) o personal (custom). Referencia inmutable en historial |
| Ejercicio en sesión/rutina | `SessionExercise` / `RoutineExercise` | La instancia ordenada de un ejercicio dentro de una sesión o plantilla (con su descanso, superset y notas propias) |
| Serie | `Set` (`SessionSet`) | Un grupo de repeticiones registrado: `{reps, peso, RIR?, tipo, nota?}`. Siempre inmediatamente persistida |
| Serie objetivo | `Target set` (`RoutineSet`) | Plantilla de serie en una rutina: `{tipo, reps objetivo, peso sugerido?, RIR objetivo?}` — no computa nada hasta registrarse real |
| Repetición (rep) | `Rep` | Nº entero de repeticiones de la serie |
| Peso | `Weight` | kg con 2 decimales; en bodyweight = lastre añadido (RN-13); almacenado SI, presentación kg/lb (RN-01) |
| Lastre | `Added weight` | Peso añadido a un ejercicio bodyweight (negativo = asistencia) |
| RIR | `RIR` (Reps In Reserve) | Repeticiones que el usuario podría haber hecho aún al terminar la serie. 0 = al fallo. Opcional, 0–10 |
| Tipo de serie | `SetType` | `WARMUP` (calentamiento), `NORMAL`, `FAILURE` (fallo), `DROP` (drop set: series DROP consecutivas, RN-11) |
| Superset | `Superset` | Grupo de 2–5 ejercicios adyacentes que se alternan (presentación + orden + timer, RN-10) |
| Rutina / plantilla | `Routine` | Estructura reutilizable de ejercicios + series objetivo + descansos. Punto de partida de sesiones |
| PR / récord personal | `PersonalRecord` | `{MAX_WEIGHT \| MAX_E1RM}` por ejercicio, recalculado idempotente (RN-05). Histórico completo |
| 1RM estimado | `e1RM` | Epley: `peso × (1 + reps/30)`; solo ≤ 12 reps y sin WARMUP (RN-03) |
| Volumen | `Volume` | `Σ peso × reps` de series que computan (RN-04); por serie/ejercicio/sesión/semana |
| Racha | `Streak` | Días naturales consecutivos con ≥ 1 sesión finalizada (RN-06) |
| Semana ISO | `ISO week` | Lunes→domingo (RN-07), base de todas las agregaciones semanales |
| Medición corporal | `BodyMeasurement` | Registro de peso y/o perímetros (pecho, cintura, cadera, brazo, muslo) con fecha (RN-23) |
| Grupo muscular | `MuscleGroup` | Taxonomía fija de 13 grupos (doc 05) — primario y secundarios |
| Equipamiento | `Equipment` | Taxonomía fija de 8 valores |
| Mecánica | `Mechanics` | `COMPOUND` (compuesto) / `ISOLATION` (aislamiento) |
| Cuenta | `User` | Identidad con email único, preferencias y consentimiento RGPD |
| Cuenta no verificada | `UNVERIFIED` | Email sin confirmar; bloquea login tras 24 h del registro (RN-16) |
| Token de acceso | `Access token` (JWT) | 15 min, en memoria del cliente, nunca localStorage |
| Refresh | `Refresh token` | Opaco, 30 días, hash SHA-256 en BD, rotado en cada uso (RN-15), en cookie httpOnly |
| Vinculación | `Account linking` | Asociar una identidad OAuth a una cuenta existente por email verificado (RN-17) |
| Temporizador de descanso | `Rest timer` | Cuenta atrás autoiniciada al confirmar serie (RF-038); estado local no persistido |
| Cronómetro de sesión | `Session clock` | Duración desde `started_at` (RF-039) |
| Auto-cierre | `AUTO_CLOSED` | Cierre automático de sesión activa > 24 h (RN-09) |
| Feature flag | `FeatureFlag` | Toggle global de plataforma (RF-082) |
| Bounded context | — | Módulo con lenguaje y modelo propios: `identity`, `catalog`, `training`, `progress`, `body`, `platform` |
| Problema (error) | `Problem` (RFC 7807) | Formato único de error API con `code` estable (doc 06 §8) |

Convenciones de lenguaje:

- **Código 100 % en inglés** (identificadores, logs, mensajes de commit); **docs en
  español**; UI y emails en es/en según locale del usuario.
- Los términos de la tabla son los únicos válidos en PRs, tests y docs; sinónimos
  («workout», «log», «entrada», «registro de entreno») prohibidos para evitar el
  bable de diseño.
- Traducciones UI revisadas juntas en M6 (glosario como fuente).
