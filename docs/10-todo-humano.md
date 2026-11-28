# 10 · TODO humano · cierre del MVP

> Este documento contiene **todo lo que no puede completar un agente** desde el repositorio:
> cuentas, credenciales, dispositivos, decisiones legales y validación real. El estado del
> código se verifica con `make verify`; este checklist cierra E1–E7 y `v1.0.0`.

## Estado de partida

- M0–M6 completados; `v0.7.0` etiquetada.
- M7 preparado: deploy workflow, backups, runbook, métricas, flags, Swagger, k6 y PIT.
- Pendiente: despliegue real, validación Android, dogfooding y cierre legal/operativo.

---

## A · Cuentas y credenciales externas

> No pegues claves privadas en el chat ni en el repositorio. Configúralas como secretos.

### A.1 VM y red

- [ ] Crear VM Oracle Cloud Always Free (amd64 o ARM, Ubuntu 24.04, 2 vCPU/4 GB mínimo).
- [ ] Guardar la **clave SSH privada** y la IP pública.
- [ ] Abrir solo 22/80/443 en el firewall de Oracle; no exponer PostgreSQL.
- [ ] Instalar Docker Engine + compose plugin y añadir el usuario a `docker`.
- [ ] Clonar el repo en `/opt/ferro`.

### A.2 DNS y TLS

- [ ] Elegir dominio/subdominio para la API, p. ej. `api.ferro.example`.
- [ ] Crear cuenta DuckDNS o usar el registrador del dominio.
- [ ] Apuntar el registro A/AAAA a la IP de la VM.
- [ ] Configurar `FERRO_DOMAIN` y `OPENAPI_SERVER_URL` en `infra/.env`.
- [ ] Verificar que Caddy obtiene certificado y que HTTPS responde.

### A.3 GitHub Actions

Configurar en el repositorio:

| Tipo | Nombre | Valor |
|---|---|---|
| Secret | `DEPLOY_HOST` | IP o DNS de la VM |
| Secret | `DEPLOY_USER` | usuario SSH |
| Secret | `DEPLOY_SSH_KEY` | clave privada SSH |
| Secret | `GHCR_TOKEN` | token con `read:packages` |
| Variable | `DEPLOY_ENABLED` | `true` cuando la VM esté lista |

- [ ] Verificar que el workflow `Deploy` publica `ghcr.io/<owner>/ferro-api` multiarch.
- [ ] Desplegar una vez desde un tag y comprobar `/actuator/health/readiness`.

### A.4 SMTP

- [ ] Crear cuenta Brevo o proveedor SMTP equivalente.
- [ ] Verificar el dominio remitente y obtener usuario/clave SMTP.
- [ ] Configurar `MAIL_HOST`, `MAIL_PORT`, `MAIL_USER`, `MAIL_PASSWORD`, `MAIL_FROM`.
- [ ] Enviar un email de verificación real y comprobar la bandeja.

### A.5 OAuth real

- [ ] Crear app OAuth de Google con redirect URI:
      `https://<frontend>/auth/oauth/callback/google`.
- [ ] Crear app OAuth de GitHub con redirect URI equivalente.
- [ ] Configurar `GOOGLE_CLIENT_ID/SECRET/REDIRECT_URI` y `GITHUB_*`.
- [ ] Probar login Google y GitHub, incluido GitHub con email privado.
- [ ] Revisar consentimiento de pantalla de OAuth y política de privacidad.

### A.6 Frontend público

- [ ] Elegir hosting: Vercel (recomendado) u otro.
- [ ] Conectar el repo y configurar `apiBaseUrl`/dominio del backend.
- [ ] Configurar CORS y CSRF con el dominio real.
- [ ] Verificar CSP, HSTS y cabeceras del hosting.

---

## B · Operación y continuidad

- [ ] Instalar UptimeRobot o equivalente apuntando a `/actuator/health/readiness`.
- [ ] Configurar alerta a email si falla 2 comprobaciones seguidas.
- [ ] Configurar cron de backup diario: `infra/backup.sh /var/backups/ferro`.
- [ ] Configurar copia semanal a Object Storage con retención de 4 semanas.
- [ ] Ejecutar un **restore real** en una BD vacía y comparar conteos de `users`,
      `workout_sessions` y `session_sets`.
- [ ] Medir RTO y anotarlo en `infra/RUNBOOK.md`.
- [ ] Probar `docker compose ... kill` y reinicio; verificar `restart: unless-stopped`.
- [ ] Comprobar logs JSON y `X-Correlation-Id` en producción.
- [ ] Revisar que Grafana/Prometheus solo son accesibles por túnel SSH.
- [ ] Ejecutar `infra/k6/load.js` y `infra/k6/write.js` contra producción.
- [ ] Revisar PIT semanal en GitHub Actions y el reporte de mutación.

---

## C · Validación de producto

### C.1 PWA en Android

- [ ] Instalar la PWA desde Chrome en un Android real.
- [ ] Verificar manifest, iconos, splash y modo standalone.
- [ ] Probar sin red: debe abrir shell y mostrar banner offline, sin pantalla blanca.
- [ ] Probar temporizador con pantalla bloqueada y vuelta a primer plano.
- [ ] Probar vibración, beep y notificación si están permitidas.
- [ ] Repetir en iOS/Safari y documentar limitaciones.

### C.2 Dogfooding

- [ ] Usar Ferro para tus entrenamientos reales durante **4 semanas**.
- [ ] Registrar rutinas, sesiones, PRs, peso y medidas.
- [ ] Anotar fricciones y bugs en issues.
- [ ] Corregir los bugs que impidan uso diario.
- [ ] Guardar evidencia/fechas para E6.

### C.3 Accesibilidad manual

- [ ] Navegar toda la app solo con teclado.
- [ ] Probar con lector de pantalla (TalkBack/VoiceOver NVDA).
- [ ] Revisar contraste en tema claro y oscuro.
- [ ] Revisar target táctil ≥44 px en móvil.
- [ ] Probar zoom 200 % y `prefers-reduced-motion`.

### C.4 Navegadores

La matriz Chromium/Firefox/WebKit está automatizada. Aun así:

- [ ] Probar Safari/iOS real (no solo WebKit de escritorio).
- [ ] Probar Chrome Android real.
- [ ] Revisar fecha/hora/números con locale `en`.

---

## D · Legal y privacidad

- [ ] Revisar `docs/PRIVACIDAD.md`/página `/privacy` con asesoramiento legal.
- [ ] Rellenar el email de contacto del responsable.
- [ ] Firmar/aceptar el DPA de Brevo y guardar evidencia.
- [ ] Revisar base legal y consentimiento de datos de salud (art. 9 RGPD).
- [ ] Definir política de retención de backups y borrado.
- [ ] Probar el export de datos desde Ajustes.
- [ ] Probar el borrado de cuenta y verificar que no quedan datos.
- [ ] Revisar cookies: solo refresh httpOnly; no analytics sin consentimiento.
- [ ] Revisar términos de uso y aviso legal si se publica abiertamente.

---

## D.1 · Imágenes de ejercicios

- [ ] Revisar `docs/exercise-image-review.html` y aprobar candidatos dudosos en
      `docs/exercise-image-review.csv`.
- [ ] Ejecutar `python3 scripts/exercise-images.py --apply`.
- [ ] Revisar visualmente el catálogo completo y sustituir placeholders por imágenes
      reales cuando haya una coincidencia segura.

## E · Cierre del MVP

- [ ] Añadir URL pública y credenciales demo al README.
- [ ] Actualizar capturas si la UI cambia.
- [ ] Revisar E1–E7 en `README.md`.
- [ ] Actualizar `docs/07-roadmap.md` y `infra/RUNBOOK.md` con fechas reales.
- [ ] Ejecutar `make verify` y `make e2e` en el commit final.
- [ ] Verificar CI, Lighthouse y E2E en GitHub Actions.
- [ ] Crear tag `v1.0.0`.
- [ ] Publicar/difundir (CV, LinkedIn, comunidades) con URL y demo.

---

## F · Fuera del MVP

No es necesario para `v1.0.0`; ver `docs/08-backlog-v2.md`:

- Import/export CSV/JSON completo.
- Offline-first con sincronización.
- Calendario, programas multi-semana, RPE, push, apps nativas, cardio, IA.
- RF-067 comparador libre (COULD).
