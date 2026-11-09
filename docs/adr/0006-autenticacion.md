# ADR-0006 · Autenticación: JWT corto + refresh rotado en cookie httpOnly + OAuth2 PKCE

**Estado:** Aceptada · 2026-11-09 · decidida con el cliente (ronda 2: email+JWT + OAuth Google/GitHub)

## Contexto

SPA en `ferro.vercel.app` + API en `ferro-api.duckdns.org` (**dominios distintos** →
no hay cookies de primer plano «gratis»). Requisitos: usable como PWA, seguro contra
XSS/CSRF, sin dependencies externas de identidad (Keycloak se descartó por ops), y con
OAuth social (Google OIDC; GitHub OAuth2 sin id_token). RGPD art. 9 aplicado en registro.

## Decisión

1. **Registro/login email+contraseña** con **Argon2id** (2¹⁶ KiB, t=2, p=1 — guía
   OWASP) y política RN-14; bloqueo progresivo RF-002.
2. **Access token JWT 15 min** (`HS256`, `sub`,`jti`,`iat`,`exp`,`typ`) devuelto en
   el cuerpo y guardado **solo en memoria** (signal del store). Nunca localStorage.
3. **Refresh token opaco 30 días** (256 bits aleatorios) entregado y recibido en
   cookie `ferro_refresh` del dominio de la API: `HttpOnly; Secure; SameSite=None;
   Path=/api/v1/auth`. En BD solo su SHA-256. **Rotación en cada uso** con cadena
   `replaced_by`; reutilización de uno revocado → revocación total + email de
   seguridad (RN-15).
4. **CSRF del refresh:** el endpoint valida `Origin` contra allowlist (no basta
   SameSite=None); CORS con `credentials` solo para esos endpoints (RNF-20).
5. **OAuth2 Authorization Code + PKCE** contra Google (OIDC, validación de id_token
   por JWKS) y GitHub (user/emails APIs). Vinculación por email verificado (RN-17);
   GitHub sin email → paso de email propio + verificación.
6. Logout: revoca el refresh de la cookie; «todos los dispositivos» revoca todos.
7. Borrado de cuenta: re-autenticación + cascada transaccional (RN-18).

## Alternativas descartadas

| Alternativa | Por qué no |
|---|---|
| Refresh en localStorage | Cualquier XSS lo roba; con cookie httpOnly el JS de la página nunca lo toca |
| Solo JWT de larga vida sin refresh | O login cada 15 min, o token largo roble en memoria/XSS: peor en ambos frentes |
| Cookies de sesión de servidor (sticky) | Exige sesiones en BD + CSRF en TODA la API; JWT sin estado encaja mejor con la API REST del contrato |
| Keycloak (OIDC completo) | Contenedor + realm + tuning ≈ 1 semana de ops para un MVP; el email+JWT+OAuth propio es también el material de entrevista que el cliente busca |
| OAuth-only (sin contraseña) | Fricción para usuarios sin Google/GitHub y dependencia total de terceros |

## Consecuencias

- **Positivas:** superficie XSS mínima (nada sensible legible por JS); detección de
  robo de refresh por rotación; sin estado en el 95 % de las peticiones.
- **Negativas:** `SameSite=None` es requisito duro (cross-site) → de ahí la
  validación de `Origin`; el logout «real» del access ya emitido no es posible
  (caduca solo): aceptado por ventana de 15 min.
- **Notas de implementación:** el JWT usa HS256 con secreto ≥ 256 bits por entorno
  (rotable sin BD); el `jti` permite añadir denylist si v2 lo pide; todos los flujos
  tienen test de integración dedicado (DoD M1).
