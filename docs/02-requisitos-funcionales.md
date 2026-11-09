# 02 · Requisitos funcionales (SRS)

| Campo | Valor |
|---|---|
| Versión | 1.0 · 2026-11-09 · alcance congelado para MVP |
| Formato | RF-### · módulo · prioridad MoSCoW ([MUST]/[SHOULD]/[COULD]) |
| Normativa | Los criterios de aceptación y las reglas de negocio (RN) son vinculantes para código y tests |

Módulos = bounded contexts (ver [04-arquitectura](./04-arquitectura.md)).

| Módulo | Contexto | Historias |
|---|---|---|
| M1 | `identity` — cuentas, credenciales, tokens | RF-001…011 |
| M2 | `catalog` — ejercicios | RF-020…025 |
| M3 | `training` — sesiones y series | RF-030…042 |
| M4 | `training` — rutinas/plantillas | RF-050…054 |
| M5 | `progress` — historial, PRs, estadísticas | RF-060…068 |
| M6 | `body` — peso y medidas corporales | RF-070…073 |
| M7 | `platform` — ajustes, API, flags, emails, ops | RF-080…088 |

---

## 0 · Reglas de negocio (RN) — normativas

> **Norma general:** si un criterio de aceptación contradice una RN, gana la RN.

- **RN-01 Unidades canónicas.** Todo se almacena en SI: peso en **kg** (2 decimales),
  medidas en **cm** (1 decimal), tiempo en **s**. La preferencia `lb` del perfil es
  solo presentación: `lb = kg × 2.2046226`, redondeo a 1 decimal.
- **RN-02 Rangos de entrada.** peso ∈ [0, 1000] kg · reps ∈ [1, 1000] · RIR ∈ [0, 10]
  (entero, opcional) · descanso ∈ [15, 600] s · notas ≤ 500 caracteres · nombres ≤ 80.
- **RN-03 e1RM (Epley).** `e1RM = peso × (1 + reps/30)`, calculado solo para series
  tipo NORMAL, FAILURE y DROP. Si `reps > 12` no se calcula (null). Calentamiento nunca.
- **RN-04 Volumen.** Volumen de serie = `peso × reps` en series NORMAL/FAILURE/DROP;
  calentamiento aporta 0; bodyweight sin lastre aporta 0; bodyweight con lastre aporta
  `lastre × reps`. Volumen de ejercicio/sesión = suma de sus series.
- **RN-05 PRs automáticos.** Por ejercicio y usuario, dos tipos:
  **MAX_WEIGHT** (mayor peso en una serie de reps ≥ 1, tipos NORMAL/FAILURE/DROP) y
  **MAX_E1RM** (mayor e1RM). El PR persiste `{valor, fecha, session_id, set_id}`.
  Recalculo idempotente al finalizar/editar sesiones que impliquen el ejercicio.
  En bodyweight, MAX_WEIGHT compara el lastre (RN-13).
- **RN-06 Racha (streak).** Número de días naturales consecutivos con ≥ 1 sesión
  finalizada, contando hoy solo si hoy hay sesión finalizada. Un día sin sesiones
  rompe la racha. Cálculo en la zona horaria del perfil (Europe/Madrid por defecto).
- **RN-07 Semanas ISO.** Toda agregación semanal (volumen, sesiones) usa semanas
  ISO-8601 (lunes→domingo) en la zona horaria del usuario.
- **RN-08 Sesión única activa.** Máximo 1 sesión sin finalizar por usuario. Intentar
  abrir otra → error 409 `SESSION_ALREADY_ACTIVE` con el id de la activa.
- **RN-09 Auto-cierre.** Job diario: sesión activa con `started_at` > 24 h se cierra
  como `AUTO_CLOSED` (duración = 24 h máx. real hasta última serie) y se notifica
  por email (flag `email.session-autoclosed`).
- **RN-10 Superset.** Grupo de 2–5 ejercicios adyacentes dentro de sesión/plantilla.
  Es presentación + orden + temporizador alternante; las series pertenecen a cada
  ejercicio igualmente. Se deshace reordenando fuera del grupo.
- **RN-11 Drop set.** Se registra como series consecutivas de tipo DROP sobre el mismo
  ejercicio; la UI las agrupa visualmente. No hay estructura anidada.
- **RN-12 Límites de contenido.** ≤ 30 ejercicios por sesión/plantilla · ≤ 15 series
  por ejercicio · identidades de ejercicio dentro de sesión ordenadas por `position`
  (0-based, sin huecos garantizado por la API).
- **RN-13 Bodyweight.** Ejercicio con flag `bodyweight`: el peso registrado se
  interpreta como **lastre añadido** (negativo = asistencia, p. ej. banda/dominada
  asistida). Volumen y PRs según RN-04/RN-05.
- **RN-14 Contraseñas.** ≥ 10 caracteres con minúscula + mayúscula + dígito; se
  rechazan las 10.000 contraseñas más comunes (lista embebida en el servicio).
- **RN-15 Tokens.** Access JWT 15 min (`sub`, `jti`, `iat`, `exp`, `typ=access`,
  firma HS256 con secreto ≥ 256 bits rotado por entorno). Refresh opaco 30 días,
  almacenado como SHA-256 en BD, **rotado en cada uso**; si se reutiliza un refresh
  revocado → se revocan todos los tokens del usuario + email de seguridad.
- **RN-16 Verificación de email.** Tras el registro se envía un enlace (token de un
  solo uso, 24 h). La sesión post-registro ya está iniciada con banner persistente
  «verifica tu email». Si a las 24 h no está verificado, el login queda bloqueado
  hasta verificar (reenvío disponible).
- **RN-17 Vinculación OAuth.** Tras un login OAuth, si el email del proveedor coincide
  con una cuenta existente verificada → se vincula a esa cuenta. Si el proveedor no
  entrega email usable (GitHub con email privado), la UI pide completarlo antes de
  continuar. Nunca existen dos cuentas con el mismo email.
- **RN-18 Borrado de cuenta.** Confirmación re-autenticando (contraseña o sesión
  OAuth vigente). Borrado real en cascada en una transacción: sesiones, series,
  rutinas, PRs, mediciones, ejercicios custom, tokens. Respuesta 204.
- **RN-19 Localización API.** Errores localizados por `Accept-Language` (`es`/`en`,
  fallback `es`). Nombres de ejercicio: locale del perfil > Accept-Language.
- **RN-20 Rate limits.** Registro 5/h por IP · login 10/min por IP+email · refresh
  30/min por IP · API autenticada 100/min por usuario. Exceso → 429 con `Retry-After`.
- **RN-21 Fechas.** ISO-8601 con offset (p. ej. `2026-11-09T18:30:00+01:00`);
  almacenamiento `timestamptz` en UTC; presentación en zona del perfil.
- **RN-22 Sesión en vivo = persistencia inmediata.** Cada serie se envía y persiste
  al registrarse (no hay borrador local ni batch). La sesión activa se recupera con
  `GET /workout-sessions/active` al recargar.
- **RN-23 Mediciones corporales.** Registro válido si contiene ≥ 1 campo (peso o
  cualquier perímetro). Peso ∈ [20, 400] kg · perímetros ∈ [10, 250] cm.
- **RN-24 Plantillas.** Series objetivo = `{tipo, reps objetivo, peso sugerido (opc.),
  RIR objetivo (opc.)}`. Al iniciar sesión desde plantilla se precargan como series
  «pendientes» completables; no computan volumen/PR hasta registrar reps+peso reales.

---

## M1 · Identidad (`identity`)

### RF-001 · Registro con email y contraseña [MUST]

Como **P1** quiero crear una cuenta con email y contraseña para guardar mis datos.

Criterios:
1. Formulario: nombre (≤ 80), email, contraseña (RN-14), checkbox de consentimiento
   RGPD art. 9 (datos de salud) obligatorio.
2. Validación cliente + servidor (Bean Validation); email único global (409
   `EMAIL_TAKEN` — revelar duplicado en registro es aceptable y estándar).
3. Contraseña con **Argon2id** (2^{16} memoria, 2 iteraciones — OWASP 2024).
4. Se crea usuario `ENABLED`/`UNVERIFIED`, se inicia sesión inmediata y se envía
   email de verificación (RN-16) y de bienvenida (flag `email.welcome`).
5. Rate limit RN-20. El nombre nunca se trimea con espacios internos; se normaliza NFKC.

```gherkin
Escenario: registro válido
  Dado que no existe cuenta "p1@ferro.dev"
  Cuando envío POST /api/v1/auth/register {nombre, email, contraseña "Ferro1234", consent=true}
  Entonces recibo 201 con {user, accessToken(15m), refreshToken(30d)}
  Y se envía email de verificación a "p1@ferro.dev"
  Y GET /api/v1/auth/me con el access token devuelve el usuario con status UNVERIFIED

Escenario: email duplicado
  Dado que existe cuenta "p1@ferro.dev"
  Cuando envío el mismo registro
  Entonces recibo 409 {type, title, code:"EMAIL_TAKEN"} (problema RFC 7807)
```

### RF-002 · Login con email [MUST]

Criterios:
1. `POST /auth/login` → 200 `{user, accessToken, refreshToken}`; 401 genérico
   `INVALID_CREDENTIALS` (no revela si falla el email o la contraseña).
2. Bloqueo progresivo: 5 fallos seguidos en 15 min → bloqueo 15 min (403 `ACCOUNT_LOCKED`).
3. Cuentas con email sin verificar tras 24 h del registro → 403 `EMAIL_UNVERIFIED`
   (con reenvío incluido en la respuesta).
4. Login no revoca refresh anteriores (múltiples dispositivos permitidos).

### RF-003 · Refresh y rotación de tokens [MUST]

Criterios:
1. `POST /auth/refresh` con refresh válido → 200 con **nuevo access + nuevo refresh**
   (RN-15); el refresh usado queda revocado.
2. Refresh expirado/revocado → 401 `REFRESH_INVALID`.
3. Reutilización de un refresh revocado → 401 + revocación de TODOS los tokens del
   usuario + email de seguridad «actividad sospechosa» (flag `email.security`).

### RF-004 · Logout [MUST]

`POST /auth/logout` con el refresh → lo revoca (204). El access caduca solo (15 min).
Logout «de todos los dispositivos» disponible desde perfil: revoca todos los refresh.

### RF-005 · Login con Google [MUST]

Criterios:
1. Botón «Continuar con Google» → OAuth2 Authorization Code + PKCE contra Google
   (OIDC); el backend canjea el code y obtiene `sub`, email, email_verified.
2. Email verificado de Google + cuenta existente → vincula (RN-17) e inicia sesión.
3. Email nuevo → crea cuenta `VERIFIED` (Google garantiza el email) con locale heredado
   del navegador y unidades kg por defecto.
4. El access token nunca se loguea; el id_token de Google se valida contra JWKS.

### RF-006 · Login con GitHub [MUST]

Criterios:
1. Igual que RF-005 vía OAuth2 de GitHub (user-api + emails-api).
2. Si GitHub no entrega email (privado) → paso intermedio en la UI para introducir
   email; se envía verificación y la cuenta queda UNVERIFIED hasta confirmar.
3. Email público verificado → equivalente a RF-005.3.

### RF-007 · Perfil y preferencias [MUST]

`GET /auth/me` + `PATCH /users/me`: nombre, locale (`es`/`en`), unidades (`KG`/`LB`),
tema (`DARK` por defecto / `LIGHT`), duración de descanso por defecto (90 s),
zona horaria (solo lectura en MVP: `Europe/Madrid`), confirmar al recalcular stats.
Validaciones y 204/400 según contrato API (doc 06).

### RF-008 · Cambio de contraseña [MUST]

Requiere contraseña actual; si es correcta → nueva (RN-14), revoca todos los refresh
excepto el de la sesión actual, email de notificación. Contraseña actual incorrecta
→ 401 y cuenta en el bloqueo progresivo de RF-002.

### RF-009 · Recuperación de contraseña [SHOULD]

`POST /auth/password-reset/request` (email) → siempre 202 (no revela existencia).
Email con token de un solo uso (30 min, hash SHA-256 en BD). `POST /auth/password-reset/
confirm` → 204 y revoca todos los refresh. Rate limit 3/h por email.

### RF-010 · Eliminar cuenta [MUST]

Botón «zona de peligro» → modal que exige re-autenticación (RN-18) → borrado en cascada
→ 204 → logout cliente. Email de confirmación de baja (flag `email.account-deleted`).

### RF-011 · Seguridad de sesión [MUST]

El access token vive solo en memoria (signal del store); al recargar se hace silent
refresh. El refresh viaja en cookie `httpOnly; Secure; SameSite=None` (dominio API) —
ver ADR-0006. El endpoint `/auth/refresh` valida `Origin` contra la allowlist (CSRF).

---

## M2 · Catálogo de ejercicios (`catalog`)

### RF-020 · Catálogo pre-cargado [MUST]

~200 ejercicios (seed Flyway V2, fuente: lista curada en repositorio, doc 05 §5) con:
`name_es`, `name_en`, grupo muscular primario, secundarios (lista), equipamiento,
mecánica (`COMPOUND`/`ISOLATION`), flag `bodyweight`, `is_custom=false`.
Requisito: cubrir PPL/upper-lower completo (press, sentadilla, peso muerto, jalones,
bíceps/tríceps, hombro, core…). Listado en `docs/05-modelo-datos.md` §5.

### RF-021 · Búsqueda y filtros [MUST]

`GET /exercises?q=&muscle=&equipment=&page=`: texto (insensible a acentos y mayúsculas,
`pg_trgm`), filtro por grupo muscular y equipamiento; resultados: del catálogo global +
custom del usuario (los suyos marcados `mine: true` primero si hay `q`); paginado
(page/size ≤ 100). Orden: relevancia (trgm) → alfabético.

### RF-022 · Crear ejercicio personalizado [MUST]

`POST /exercises` con los campos del catálogo (nombre es/en opcional uno de los dos,
mínimo uno; grupo primario obligatorio) → `is_custom=true`, `owner_id=user`.
Visible solo para su dueño. Límite: 100 custom por usuario.

### RF-023 · Editar/archivar custom [MUST]

Editable por su dueño mientras no tenga series registradas; si tiene historial solo se
puede **archivar** (`archived=true`: desaparece de búsquedas, permanece en historial).
El catálogo global no es editable por usuarios.

### RF-024 · Detalle de ejercicio [MUST]

`GET /exercises/{id}`: ficha con atributos + stats del usuario (PRs activos, mejor e1RM,
volumen total, nº de sesiones) + mini-gráfica e1RM (últimas 52 semanas) si el usuario
tiene datos; si no, sección «aún sin datos» con CTA a entrenar.

### RF-025 · Selector con autocompletado [MUST]

Componente reutilizable (sesión y plantilla): input con debounce 250 ms → RF-021,
navegación por teclado, selección con Enter, chip del elegido, crear custom inline
(abre modal RF-022) si no hay resultados. Accesible (combobox ARIA).

---

## M3 · Entrenamiento — sesiones y series (`training`)

### RF-030 · Crear sesión vacía [MUST]

Pantalla principal → botón **«Empezar a entrenar»** → `POST /workout-sessions {}`
(nombre por defecto «Sesión del {fecha locale}», `started_at=now`). Respuesta con
navegación a la vista «sesión en vivo». Regla RN-08. Criterio: si hay plantillas,
se muestra antes un modal «¿vacía o desde plantilla?» con lista de rutinas (RF-031).

### RF-031 · Iniciar sesión desde plantilla [MUST]

`POST /routines/{id}/start` (o `POST /workout-sessions {routineId}`): copia ejercicios,
orden, descansos, grupos de superset y series objetivo (pendientes, RN-24). La plantilla
no se modifica por editar la sesión.

### RF-032 · Gestionar ejercicios de la sesión [MUST]

En sesión en vivo: añadir ejercicio (RF-025), reordenar (drag & drop y botones ↑/↓
accesibles), eliminar (con confirmación si tiene series → las series se pierden),
editar descanso del ejercicio (RN-02) y crear/editar superset (RN-10: seleccionar
2–5 adyacentes → «agrupar»; etiquetas A1, A2…). Todo persistido inmediatamente (RN-22).

### RF-033 · Registrar una serie [MUST]

Por cada ejercicio en vivo: formulario serie = `{reps, peso, RIR opc., tipo, nota opc.}`
+ botón ✓. Al confirmar: POST inmediato, la serie pasa a «hecha» (visual), y se abre
el temporizador (RF-039) salvo última serie del ejercicio. Por defecto la nueva serie
pre-rellena los valores de la anterior del mismo ejercicio. Series objetivo pendientes
(RN-24): al confirmar sustituyen el placeholder.

```gherkin
Escenario: registrar serie normal
  Dado sesión activa con ejercicio "Press banca" sin series
  Cuando confirmo {reps: 8, peso: 80, tipo: NORMAL}
  Entonces la serie aparece como hecha con volumen 640 kg
  Y el temporizador arranca con el descanso del ejercicio (90 s por defecto)
  Y la siguiente serie se pre-rellena con {reps: 8, peso: 80}

Escenario: series objetivo desde plantilla
  Dado sesión iniciada desde plantilla con 3 series objetivo {4×8 @ 100 kg, RIR 2}
  Cuando confirmo la primera con {reps: 8, peso: 100}
  Entonces quedan 2 pendientes y 1 hecha, y el contador de progreso del ejercicio muestra 1/3
```

### RF-034 · Tipos de serie [MUST]

Selector de tipo: `WARMUP`, `NORMAL` (defecto), `FAILURE`, `DROP` (RN-11). El tipo
afecta a color/chip en UI y a los cálculos (RN-03/04/05). El tipo es editable en la
serie (RF-037).

### RF-035 · Notas [MUST]

Nota por serie (≤ 500) y nota por ejercicio-en-sesión (≤ 500) y por sesión (≤ 500).
Editables mientras la sesión está abierta; en histórico editables según RF-062.

### RF-036 · Editar/eliminar serie [MUST]

En vivo: tocar una serie hecha → editar (reps/peso/RIR/tipo/nota) o eliminar, con
reordenación implícita (posición recalculada). Persistencia inmediata. Nunca se permite
una serie con reps vacío.

### RF-037 · Performance anterior [MUST]

Al enfocar un ejercicio en vivo, bajo el formulario se muestra el **historial reciente**
de ese ejercicio (últimas 3 sesiones con series: reps×peso×tipo, fecha; mejor serie de
cada sesión en negrita) y el PR actual (tipo y valor). Dato: `GET /exercises/{id}/history?limit=3`.

### RF-038 · Temporizador de descanso [MUST]

Criterios:
1. Autoarranca al confirmar una serie (si no es la última del ejercicio; en superset,
   alterna entre ejercicios del grupo con su descanso respectivo).
2. UI flotante (chip abajo) con cuenta atrás, **+15 s**, **saltar** y ajuste directo.
3. Duración: descanso del ejercicio; si no, preferencia del usuario (default 90 s).
4. Al terminar: notificación web (Notification API, si concedida) + vibración
   (Vibration API, Android) + beep opcional (setting `timer.sound`).
5. Sigue corriendo con la pestaña en segundo plano o pantalla apagada
   (implementación con `setTimeout` + corrección por timestamp al volver a primer plano).
6. El estado del timer es local (no se persiste ni sincroniza).

### RF-039 · Cronómetro de sesión [MUST]

Duración visible desde `started_at` (mm:ss / h:mm:ss). Se pausa solo al finalizar.
En detalle de histórico se muestra la duración final (o `AUTO_CLOSED` con 24 h — RN-09).

### RF-040 · Finalizar sesión [MUST]

Botón «Finalizar» (deshabilitado si 0 series registradas; en ese caso solo «descartar»).
`POST /workout-sessions/{id}/finish`:
1. `finished_at=now`, `duration_s` calculado, `total_volume` (RN-04).
2. Recalculo de PRs de los ejercicios de la sesión (RN-05); los nuevos PRs se
   devuelven en la respuesta para el «resumen con fanfarria».
3. Publica evento de dominio `SessionCompleted`.
4. Navega a resumen: duración, volumen, nº series, PRs nuevos (cards), lista completa.

```gherkin
Escenario: primer PR de peso
  Dado que nunca he registrado "Sentadilla" y finalizo una sesión con 5×100 kg
  Cuando se procesa el finish
  Entonces la respuesta incluye newPersonalRecords: [{exercise, type: MAX_WEIGHT, value: 100}, {type: MAX_E1RM, value: 126.67}]
  Y la lista de PRs del ejercicio muestra ambos con la fecha de hoy
```

### RF-041 · Descartar sesión [MUST]

«Descartar» con confirmación tipo (escribir/confirmación doble si tiene series) →
DELETE lógico → 204. No computa nada.

### RF-042 · Recuperar sesión activa [MUST]

Al abrir la app con sesión activa (RN-08/22): banner «tienes una sesión en curso» →
volver a la vista en vivo con todo el estado (ejercicios, series hechas y pendientes,
notas). Si la app se abre > 24 h después: la sesión ya fue auto-cerrada (RN-09).

---

## M4 · Rutinas / plantillas (`training`)

### RF-050 · CRUD de rutinas [MUST]

Lista de rutinas (nombre, nº ejercicios, última vez usada) + crear/editar: nombre
(≤ 80, único por usuario — 409 `ROUTINE_NAME_TAKEN`), notas, ejercicios ordenados con
`{series objetivo (RN-24), descanso, superset}`. El builder reutiliza RF-025 y la UI
de series objetivo. Límites RN-12.

### RF-051 · Duplicar rutina [MUST]

Acción «duplicar» → copia con nombre «{nombre} (copia)»; edit recomendado inmediato.

### RF-052 · Reordenar y eliminar en plantilla [MUST]

Como RF-032 (drag & drop + botones accesibles + agrupar superset).

### RF-053 · Arrancar sesión desde rutina [MUST]

Ver RF-031. En móvil, acceso prioritario desde home: lista de rutinas → ▶.

### RF-054 · Límites y validaciones de plantilla [MUST]

Guardar bloqueado si: 0 ejercicios, > 30 ejercicios, > 15 series objetivo por ejercicio,
nombre vacío/duplicado, series objetivo sin reps (1–1000). Mensajes de error concretos
por campo (i18n).

---

## M5 · Progreso (`progress`)

### RF-060 · Historial de sesiones [MUST]

Lista paginada (20/pág) ordenada por `finished_at` desc: fecha, nombre, nº ejercicios,
series, volumen, badge PR, duración. Filtros: rango de fechas y ejercicio (dropdown
RF-021). Ítems: sesiones `FINISHED` + `AUTO_CLOSED` (badge distinto).

### RF-061 · Detalle de sesión [MUST]

Vista completa: metadatos, lista de ejercicios con sus series (tipo, reps, peso, RIR,
nota), notas de sesión, volumen por ejercicio, PRs logrados ese día. Readonly con
acciones: editar (RF-062) y eliminar (confirmación → recalculo PRs).

### RF-062 · Editar sesión pasada [SHOULD]

Edición limitada: notas, nombre, y corrección de series (reps/peso/RIR/tipo). Al
guardar → recalculo idempotente de volumen y PRs (RN-05). No se puede reabrir una
sesión terminada ni cambiar sus ejercicios (añadir/ quitar queda para v2).

### RF-063 · Página de PRs [MUST]

Lista por ejercicio (orden: reciente): MAX_WEIGHT y MAX_E1RM con `{valor, fecha}`,
historial de PRs anteriores del ejercicio (collapsible). Filtro por ejercicio.
Unidad de peso según preferencia (RN-01).

### RF-064 · Estadísticas — panel [MUST]

Rango selector: 4 / 12 / 24 semanas. Cards:
- **Volumen por semana** (barras; tooltips con desglose; eje en kg o lb según RN-01)
- **Sesiones por semana** (barras)
- **Racha actual** (número grande + explicación RN-06) y mejor racha histórica
- **Comparativa del periodo**: volumen total, sesiones, series vs. periodo anterior
  (%, flecha ▲▼, color semántico)
- **Distribución por grupo muscular** (últimas 4 semanas, barras apiladas o donut)

### RF-065 · e1RM por ejercicio [MUST]

En detalle de ejercicio (RF-024): gráfica de línea del mejor e1RM por sesión (últimas
52 semanas). Ejes localizados; serie sin datos si reps > 12 siempre (RN-03).

### RF-066 · Resumen semanal por email [SHOULD]

Lunes 08:00 (zona usuario): volumen, sesiones, PRs de la semana anterior + racha.
Opt-in en perfil (`email.weekly-summary`, default off). Flag global para poder
desactivar todo el envío.

### RF-067 · Comparador de periodos [COULD]

Dos rangos arbitrarios → tabla volumen/sesiones/series/PRs con deltas. v1 básico
incluido en RF-064 (periodo vs. anterior); comparador libre → backlog v2 si no cabe.

---

## M6 · Cuerpo (`body`)

### RF-070 · Registrar peso corporal [MUST]

Form rápido desde home y desde sección Cuerpo: `{peso, fecha (default hoy), nota}` →
toast de confirmación. RN-23. Si ya existe registro ese día → se propone sobrescribir
(mismo día = una medición por campo).

### RF-071 · Registrar medidas [MUST]

Mismo formulario, secciones: peso, pecho, cintura, cadera, brazo, muslo (cm, opcionales,
RN-23). Una medición = una fila con los campos informados.

### RF-072 · Evolución corporal [MUST]

Gráficas (peso; cada perímetro seleccionable; rango 1/3/6/12 meses) + tabla del histórico
reciente. Valores en kg/cm o lb/pulgadas según RN-01.

### RF-073 · Editar/eliminar mediciones [MUST]

Lista → editar (todos los campos) / eliminar con confirmación. Recalcula gráficas.

---

## M7 · Plataforma (`platform`)

### RF-080 · Ajustes de la app [MUST]

Sección Ajustes: preferencias (RF-007), gestión de sesión (RF-004), zona de peligro
(RF-010), idioma, tema, sonido timer, descanso por defecto. Persistidas en perfil;
el tema y el idioma aplican sin recargar (signals).

### RF-081 · Documentación interactiva de la API [MUST]

Swagger UI pública en `/api/docs` (springdoc, OpenAPI 3.1) con servidores por entorno,
esquemas completos y ejemplos. Es parte de la demo para P2.

### RF-082 · Feature flags [MUST]

Flags leídas al arranque (properties por entorno) + override global en tabla
`feature_flags` (clave, enabled, descripción): `email.welcome`, `email.security`,
`email.weekly-summary`, `email.session-autoclosed`, `email.account-deleted`,
`timer.sound-default`, `demo-account`. Exposición vía `GET /config/flags` (público,
solo claves sin secretos). Cambio en caliente al refrescar config (sin redeploy).

### RF-083 · Health checks [MUST]

`/actuator/health/liveness` y `/readiness` (públicos, sin detalles sensibles en prod);
readiness verifica BD. `/actuator/health` completo solo para Prometheus/red interna.

### RF-084 · Emails transaccionales [MUST como infraestructura]

MVP envía: bienvenida, verificación, recuperación, seguridad (reutilización refresh),
nuevo login OAuth (SHOULD), auto-cierre (SHOULD), resumen semanal (SHOULD), baja.
Plantillas HTML+texto simples bilingües (es/en según locale del usuario), remitente
`Ferro <no-reply@...>`. Dev = Mailpit (web UI en :8025); prod = Brevo SMTP (300/día
gratis). Envío asíncrono tras commit (Spring Modulith event publication).

### RF-085 · Observabilidad [MUST]

Actuator + Micrometer `/actuator/prometheus`; dashboard Grafana provisionado
(latencia p95 por endpoint, rps, errores 5xx, latencia SQL, pool Hikari, JVM, negocio:
`ferro_sessions_started_total`, `ferro_sessions_finished_total`, `ferro_prs_total`,
`ferro_sets_logged_total`, `ferro_emails_sent_total{template}`). Logs JSON estructurados
con `correlationId` (filtro servlet) en stdout.

### RF-086 · Rate limiting global [MUST]

Ver RN-20; implementación bucket4j por IP/usuario; respuesta 429 + `Retry-After`
(60 s). Excluye health checks.

### RF-087 · Cuenta demo [SHOULD]

`demo@ferro.app / FerroDemo26` con 12 semanas de datos sintéticos realistas
(generador seed) para que P2 pruebe sin registrarse. Solo en entorno prod-demo;
señalizada con banner «cuenta demo — datos ficticios». Flag `demo-account`.

### RF-088 · Onboarding mínimo [SHOULD]

Primera visita tras registro: 3 tarjetas deslizables (1. crea tu rutina,
2. empieza a entrenar, 3. mira tu progreso) + seed opcional: botón «crear mis rutinas
de ejemplo» (PPL ×3) para probar en 30 segundos. Skip persistido.

---

## Trazabilidad resumen

| Objetivo (doc 01) | Historias |
|---|---|
| O1 registro rápido | RF-030…042, RF-033, RF-038 |
| O2 cálculo automático | RF-040, RF-063…067, RN-03…07 |
| O3 PWA móvil | RF-080, doc 03 §PWA |
| O4 first-run | RF-001, RF-088, RF-050 |
| O5 producción | RF-081…086, doc 07 M7 |
