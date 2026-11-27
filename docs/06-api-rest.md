# 06 · API REST (contrato)

| Versión | 1.0 · 2026-11-09 · la implementación genera OpenAPI 3.1 idéntica a este contrato (RNF-50) |

## 0 · Convenciones generales

| Tema | Regla |
|---|---|
| Base | `/api/v1` — versionado por URL; solo cambios aditivos durante MVP |
| Formato | JSON UTF-8; `Content-Type: application/json` |
| Fechas | ISO-8601 con offset (RN-21): `2026-11-09T18:30:00+01:00` |
| Idioma | `Accept-Language: es|en` para errores; locale de perfil para nombres de ejercicio (RN-19) |
| Autenticación | `Authorization: Bearer <JWT>` (salvo endpoints públicos marcados 🔓) |
| Refresh | cookie `ferro_refresh` (`httpOnly; Secure; SameSite=None`) gestionada por `/auth/refresh` y `/auth/logout` |
| Errores | **RFC 7807** `application/problem+json`: `{type, title, status, detail, code, errors?}` — `code` interno estable (tabla §8), `errors` = mapa campo→[mensajes] en validaciones |
| Paginación | `?page=0&size=20` (size ≤ 100) → envelope `{content:[…], page, size, totalElements, totalPages}` |
| Orden | Documentado por endpoint; default el más útil (`finished_at desc`, relevancia trgm, `position asc`) |
| Rate limit | RN-20; 429 + `Retry-After: 60` |
| Idempotencia | No requerida en MVP (los POST son creaciones naturales sin retries peligrosos) |
| CORS | allowlist estricta (RNF-20) |

## 1 · Identidad

| Método · Path | Auth | Descripción |
|---|---|---|
| POST `/auth/register` | 🔓 | Crea cuenta (RF-001). 201 `{user, accessToken}` + cookie refresh |
| POST `/auth/login` | 🔓 | RF-002. 200 `{user, accessToken}` + cookie refresh |
| POST `/auth/refresh` | 🔓 (cookie) | RF-003. 200 `{user, accessToken}` + cookie refresh nueva |
| POST `/auth/logout` | 🔓 (cookie) | RF-004. 204, revoca cookie |
| POST `/auth/logout-all` | 🔑 | Revoca todos los refresh. 204 |
| GET `/auth/oauth/{provider}/authorize-url` | 🔓 | URL de inicio OAuth (PKCE state gestionado) |
| POST `/auth/oauth/{provider}/callback` | 🔓 `{code, state}` | RF-005/006. 200 `{user, accessToken, needsEmail?}` + cookie |
| POST `/auth/email-verification/resend` | 🔑 | 202 |
| POST `/auth/email-verification/confirm` | 🔓 `{token}` | 204 |
| POST `/auth/password-reset/request` | 🔓 `{email}` | 202 siempre (RF-009) |
| POST `/auth/password-reset/confirm` | 🔓 `{token, newPassword}` | 204 |
| GET `/auth/me` | 🔑 | 200 `{user, preferences, flags?}` |
| PATCH `/users/me` | 🔑 | RF-007. 204 |
| PATCH `/users/me/password` | 🔑 `{current, next}` | RF-008. 204 |
| DELETE `/users/me` | 🔑 `{confirmPassword?}` | RF-010. 204 |

### Ejemplo — registro

```http
POST /api/v1/auth/register
{
  "displayName": "Ángel",
  "email": "p1@ferro.dev",
  "password": "Ferro1234",
  "healthConsent": true,
  "locale": "es",
  "unitSystem": "KG"
}
```

```json
201 {
  "user": {
    "id": "01936a1e-…", "displayName": "Ángel", "email": "p1@ferro.dev",
    "status": "UNVERIFIED", "locale": "es", "unitSystem": "KG", "theme": "DARK",
    "timeZone": "Europe/Madrid", "defaultRestSeconds": 90
  },
  "accessToken": "eyJhbGciOi…"
}
Set-Cookie: ferro_refresh=…; HttpOnly; Secure; SameSite=None; Path=/api/v1/auth; Max-Age=2592000
```

### Ejemplo — problema de validación

```json
422 {
  "type": "https://ferro.dev/problems/validation",
  "title": "La petición no es válida",
  "status": 422,
  "code": "VALIDATION_FAILED",
  "errors": {
    "password": ["Debe tener al menos 10 caracteres", "Debe incluir una mayúscula"],
    "email": ["no es un email válido"]
  }
}
```

## 2 · Catálogo

| Método · Path | Auth | Descripción |
|---|---|---|
| GET `/exercises` | 🔑 | RF-021: `q, muscle, equipment, archived=false, page, size`. Custom del usuario marcados `mine` |
| GET `/exercises/{id}` | 🔑 | RF-024: ficha + stats usuario (PRs, volumen total, sesiones, e1RM serie 52 sem.) |
| POST `/exercises` | 🔑 | RF-022: custom → 201 |
| PATCH `/exercises/{id}` | 🔑 | RF-023: solo dueño; 409 `EXERCISE_IN_USE` → usar `archived=true` |
| GET `/exercises/{id}/history` | 🔑 | RF-037: `limit=3` últimas sesiones con sus series |

```json
GET /api/v1/exercises?q=press banca
200 {
  "content": [
    {
      "id": "…", "name": "Press de banca", "muscleGroup": "CHEST",
      "secondaryMuscles": ["TRICEPS","SHOULDERS"], "equipment": "BARBELL",
      "mechanics": "COMPOUND", "bodyweight": false, "isCustom": false, "mine": false
    }
  ], "page": 0, "size": 20, "totalElements": 1, "totalPages": 1
}
```

## 3 · Rutinas

| Método · Path | Auth | Descripción |
|---|---|---|
| GET `/routines` | 🔑 | Lista (`name, exerciseCount, lastUsedAt`), paginada |
| POST `/routines` | 🔑 | RF-050 → 201 |
| GET `/routines/{id}` | 🔑 | Detalle con ejercicios y series objetivo |
| PUT `/routines/{id}` | 🔑 | Reemplazo completo del contenido (atomic) |
| DELETE `/routines/{id}` | 🔑 | 204 (las sesiones ya iniciadas no se tocan) |
| POST `/routines/{id}/duplicate` | 🔑 | RF-051 → 201 |
| POST `/routines/{id}/start` | 🔑 | RF-031 → 201 `WorkoutSessionDto` (equivale a POST session con routineId) |

## 4 · Sesiones y series

| Método · Path | Auth | Descripción |
|---|---|---|
| POST `/workout-sessions` | 🔑 | RF-030/031: `{routineId?}` → 201. 409 `SESSION_ALREADY_ACTIVE` (RN-08) |
| GET `/workout-sessions/active` | 🔑 | RF-042 → 200 sesión activa o 204 vacío |
| GET `/workout-sessions` | 🔑 | Historial real en `GET /progress/sessions` |
| GET `/workout-sessions/{id}` | 🔑 | RF-061 detalle completo |
| PATCH `/workout-sessions/{id}` | 🔑 | `{name?, notes?}` (también en histórico) |
| POST `/workout-sessions/{id}/finish` | 🔑 | RF-040 → 200 `{session, newPersonalRecords[]}` |
| DELETE `/workout-sessions/{id}` | 🔑 | RF-041 → 204 (DISCARDED) |
| POST `/workout-sessions/{id}/exercises` | 🔑 | `{exerciseId, position?}` → 201 |
| PATCH `/workout-sessions/{id}/exercises/{entryId}` | 🔑 | `{position?, restSeconds?, supersetGroup?, notes?}` (reordenaciones atómicas) |
| DELETE `/workout-sessions/{id}/exercises/{entryId}` | 🔑 | 204 (con sus series) |
| POST `/workout-sessions/{id}/exercises/{entryId}/sets` | 🔑 | RF-033 `{reps, weight, rir?, setType, notes?, targetSetId?}` → 201 |
| PATCH `…/sets/{setId}` | 🔑 | RF-036/062 `{reps?, weight?, rir?, setType?, notes?}` |
| DELETE `…/sets/{setId}` | 🔑 | 204 |

### Ejemplo — registrar serie

```http
POST /api/v1/workout-sessions/{sid}/exercises/{eid}/sets
{ "reps": 8, "weight": 80.0, "rir": 2, "setType": "NORMAL" }
```

```json
201 {
  "id": "…", "position": 2, "setType": "NORMAL",
  "reps": 8, "weight": 80.0, "rir": 2,
  "volume": 640.0, "e1rm": 101.33,
  "loggedAt": "2026-11-09T18:42:10+01:00"
}
```

### Ejemplo — finalizar sesión

```json
POST …/finish → 200 {
  "session": { "id": "…", "durationSeconds": 3612, "totalVolume": 12840.0, "status": "FINISHED", "finishedAt": "…" },
  "newPersonalRecords": [
    { "exerciseId": "…", "exerciseName": "Press de banca", "prType": "MAX_WEIGHT", "value": 100.0,
      "previousValue": 97.5, "achievedAt": "…" },
    { "prType": "MAX_E1RM", "value": 126.67, "previousValue": null }
  ]
}
```

## 5 · Progreso

| Método · Path | Auth | Descripción |
|---|---|---|
| GET `/progress/prs` | 🔑 | RF-063: `exerciseId?` — activos + histórico agrupado |
| GET `/progress/summary` | 🔑 | RF-064: `weeks=4|12|24` → cards (volumen/sem, sesiones/sem, streak, mejor racha, comparativa, distribución músculos) |
| GET `/progress/streak` | 🔑 | `{current, best}` (RN-06) |
| GET `/progress/volume` | 🔑 | `from, to, granularity=week` (serie para gráfica) |
| GET `/exercises/{id}/history` | 🔑 | (ya en §2) — alimenta e1RM por sesión (RF-065) |

## 6 · Cuerpo

| Método · Path | Auth | Descripción |
|---|---|---|
| GET `/body-measurements` | 🔑 | `from, to, page` |
| GET `/body-measurements/{id}` | 🔑 | Detalle (404 si es ajeno) |
| POST `/body-measurements` | 🔑 | RF-070/071 `{measuredAt, weight?, chest?, waist?, hip?, arm?, thigh?, notes?}` → 201 |
| PATCH `/body-measurements/{id}` | 🔑 | RF-073 |
| DELETE `/body-measurements/{id}` | 🔑 | 204 |
| GET `/body-measurements/stats` | 🔑 | RF-072: `months=1|3|6|12` → series por campo |

## 7 · Plataforma

| Método · Path | Auth | Descripción |
|---|---|---|
| GET `/config/flags` | 🔓 | RF-082: flags públicas (sin secretos) |
| GET `/actuator/health/liveness` · `/readiness` | 🔓 | RF-083 |
| GET `/actuator/prometheus` | 🔒 red interna | RF-085 |
| GET `/api/docs` (Swagger UI) · `/api/api-docs` | 🔓 | RF-081 — pública como parte de la demo |

## 8 · Códigos de error internos (`code`)

| code | HTTP | Cuándo |
|---|---|---|
| `VALIDATION_FAILED` | 422 | Bean Validation |
| `INVALID_CREDENTIALS` | 401 | login |
| `ACCOUNT_LOCKED` | 403 | bloqueo progresivo |
| `EMAIL_UNVERIFIED` | 403 | RN-16 |
| `EMAIL_TAKEN` | 409 | registro |
| `ROUTINE_NAME_TAKEN` | 409 | RF-050 |
| `SESSION_ALREADY_ACTIVE` | 409 | RN-08 |
| `EXERCISE_IN_USE` | 409 | editar custom con historial |
| `LIMIT_REACHED` | 422 | RN-12/100 custom |
| `REFRESH_INVALID` | 401 | refresh revocado/expirado |
| `VERIFICATION_INVALID` | 400 | token de verificación inválido, caducado o usado |
| `PASSWORD_RESET_INVALID` | 400 | token de recuperación inválido, caducado o usado |
| `OAUTH_FAILED` | 401 | state/code inválido o proveedor no válido |
| `OAUTH_EMAIL_REQUIRED` | 422 | proveedor sin email verificado (GitHub privado) |
| `OAUTH_NOT_CONFIGURED` | 503 | proveedor sin client-id/secret |
| `INVALID_FILTER` | 400 | filtro de catálogo no válido |
| `RATE_LIMITED` | 429 | RN-20 |
| `NOT_FOUND` | 404 | recurso inexistente **o ajeno** (anti-IDOR, RNF-18) |
| `INTERNAL_ERROR` | 500 | no filtrar detalles |

`title/detail` localizados (RN-19); `code` estable para la UI y los tests.

## 9 · Reglas de seguridad por endpoint (resumen)

- Todo recurso de usuario (`routines`, `workout-sessions`, `body-measurements`,
  custom `exercises`…) exige titularidad: dueño o 404 `NOT_FOUND` (nunca 403).
- `PATCH/PUT` con cuerpo vacío → 422, no no-op silencioso.
- Máx. body 64 KB (rechazo 413) — notes acotadas hacen esto imposible de alcanzar
  legítimamente.
- Los ids en path son UUID; formato inválido → 400 (sin tocar BD).
