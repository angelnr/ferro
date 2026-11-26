# 07 · Roadmap de implementación (8 semanas)

| Versión | 1.0 · 2026-11-09 · inicio previsto: semana del 16-nov-2026 |
| Cadencia | Semanas naturales; ~10–12 h/semana estimadas (proyecto personal) |

Principios de trabajo durante toda la ejecución:

- **Trunk-based con PRs pequeños**: `main` siempre verde (CI) y desplegable.
- Conventional Commits; cada PR con tests; revisión personal con checklist (ADR cumplido,
  RNF no roto, docs actualizadas).
- Si una implementación contradice un RF/RN/ADR: se para, se actualiza el documento o se
  cambia el diseño — nunca divergencia silenciosa (RNF-49).
- Los hitos son acumulativos; cada uno termina con demo manual grabada (gif para README).

---

## M0 · Semana 1 — Fundaciones del monorepo

**Objetivo:** esqueleto completo ejecutable con calidad desde el día 1.

**Estado:** ✅ completado · tag `v0.1.0` (2026-11-26)

Entregables:
- [x] `backend/`: Maven + Spring Boot 4.1.x + Java 21; paquetes por módulo (§4 doc 04)
      con `shared` mínimo; `FerroApplication` con Modulith.
- [x] `frontend/`: Angular 22 (standalone + signals + zoneless + Vitest), Tailwind 4,
      ESLint/Prettier, estructura por features, SignalStore instalado, i18n base.
- [x] `infra/docker-compose.dev.yml`: postgres 18 (127.0.0.1:5432), mailpit
      (8025/1025), prometheus (9090), grafana (3003) + dashboard provisioning.
- [x] Flyway V1–V5 (esquema completo de doc 05, sin seeds de ejercicios aún).
- [x] Reglas ArchUnit (doc 04 §4) + test de verificación Modulith + `mvn verify` verde.
- [x] CI GitHub Actions: backend (build+test+checkstyle+gitleaks), frontend
      (build+test+lint), en paralelo; badge en README.
- [x] `Dockerfile` multi-stage + `infra/docker-compose.prod.yml` (borrador funcional).
- [x] README: quickstart real (`docker compose up -d && mvn spring-boot:run`).
- [x] Registro del puerto 3003 en `~/srv/PUERTOS.md` del servidor de desarrollo.

**DoD:** desde cero: `docker compose -f infra/docker-compose.dev.yml up -d`,
`mvn spring-boot:run` (health 200), `ng serve` (página base), CI verde, `git tag v0.1.0`.
**Riesgo:** sobretamaño del esqueleto → regla: M0 no implementa ni un endpoint de negocio.

## M1 · Semanas 1–2 — Identidad (`identity`)

**Objetivo:** entrar, registrarse (email+OAuth), tener sesión segura.

**Estado:** ✅ completado · tag `v0.2.0` (2026-11-26)

Entregables:
- [x] RF-001…011 completos (registro, login, refresh+rotación, logout, verificación,
      recuperación, OAuth Google y GitHub, perfil, cambio/borrado cuenta).
- [x] Emails: bienvenida, verificación, recuperación, seguridad (RF-084) vía Mailpit.
- [x] Seguridad transversal: rate limit (bucket4j), CORS allowlist, CSP, cookies
      refresh, bloqueo progresivo (ADR-0006).
- [x] Tests: unit dominio (VO Email/Password, rotación) + integración Testcontainers
      (flujos completos, anti-IDOR básico) + E2E Playwright registro/login (con OAuth
      mockeado).
- [x] Swagger parcial de `/auth` + `/users`.

**DoD:** flujo E2E verde; cobertura `identity` ≥ 80 %; emails visibles en Mailpit;
reutilización de refresh provoca revocación total (test). `git tag v0.2.0`.
**Riesgo:** OAuth GitHub sin email público → cubrir RN-17 con test específico.

## M2 · Semanas 2–3 — Catálogo (`catalog`)

**Estado:** ✅ completado · tag `v0.3.0` (2026-11-26)

Entregables:
- [ ] `docs/seed-ejercicios.csv` (~200 filas) + validador en CI + Flyway V2 seed.
- [ ] Búsqueda trgm + filtros + paginación; custom CRUD + archivado (RF-020…024).
- [ ] Componente selector/combobox accesible (RF-025) con debounce y creación inline.
- [ ] Detalle de ejercicio con stats «sin datos» + CTA.

**DoD:** búsqueda «press banca»/«banca»/«BENCH» funciona (test); límite 100 custom
testado; E2E crear-rutina-aura sin fricción. `v0.3.0`.

## M3 · Semanas 3–5 — Training core (`training`) ⭐ corazón del producto

**Estado:** ✅ completado · tag `v0.4.0` (2026-11-26)

Entregables (RF-030…042):
- [x] Sesión vacía + única-activa (RN-08 garantizado por índice parcial, test incluido).
- [x] Ejercicios en sesión: añadir/reordenar (botones y drag & drop)/eliminar/supersets
      A1/A2/notas/descanso (RF-032).
- [x] Series: registro inmediato (RN-22), tipos (RN-11/34), notas, edición/eliminación y
      pre-relleno. Las series objetivo pendientes se activan en M4.
- [x] Performance anterior al registrar (RF-037): últimas 3 sesiones, mejor serie y PR actual.
- [x] Temporizador descanso (RF-038: autoarranque, alternancia en supersets, preferencia,
      +15 s, skip, notificación+vibración+beep) y cronómetro (RF-039).
- [x] Finish: volumen, PRs (RN-05, devueltos para el resumen), resumen en pantalla.
- [x] Descartar con confirmación, recuperar activa (RF-042), auto-cierre 24 h (RN-09).
- [x] UI en vivo mobile-first (dark, panel de descanso sticky, feedback de tipos).

**DoD:** E2E maestro «entrenamiento completo»: crear sesión → 3 ejercicios → supersets →
12 series → timer → finish → PRs visibles. Cobertura `training` ≥ 80 % (83,6 % líneas).
`v0.4.0` — **desde aquí Ferro ya es usable a diario (dogfooding E6)**. La latencia de
finish se medirá con datos reales durante el dogfooding, no con la API mockeada del E2E.
**Riesgo:** es el hito más grande → subdividir PRs por feature; si hay retraso, lo que
no puede moverse es RF-033/040.

## M4 · Semana 5 — Rutinas (`training`)

**Estado:** ✅ completado · tag `v0.5.0` (2026-11-26)

Entregables (RF-050…054):
- [x] CRUD de rutinas con nombre único (409 `ROUTINE_NAME_TAKEN`), notas, lista con
      `exerciseCount` y `lastUsedAt`.
- [x] Builder con ejercicios ordenados, descanso, notas, supersets A1/A2 y series objetivo
      (tipo, reps, peso y RIR).
- [x] Duplicar rutina como `«{nombre} (copia)»`, con sufijo si el nombre ya existe.
- [x] Reordenar con botones y drag & drop; eliminar ejercicios con confirmación.
- [x] Arrancar desde rutina (lista, Home y `POST /routines/{id}/start` o
      `POST /workout-sessions {routineId}`) copiando ejercicios y series objetivo pendientes.
- [x] Límites RN-12/RN-24: 1–30 ejercicios, ≤15 series objetivo, reps 1–1000, nombre,
      descanso 15–600 s y supersets 2–5 adyacentes.

**DoD:** E2E «rutina → sesión»: crear PPL, arrancar, las series objetivo aparecen
pendientes y al confirmar se completan. 409 duplicado nombre testado. `v0.5.0`.

## M5 · Semana 6 — Progreso y Cuerpo (`progress`, `body`)

**Estado:** ✅ completado · tag `v0.6.0` (2026-11-26)

Entregables (RF-060…073):
- [x] Historial paginado con filtros por fecha/ejercicio, duración, volumen, series,
      badge PR y distinción `AUTO_CLOSED`.
- [x] Detalle de sesión readonly con edición de nombre/notas y corrección de series;
      recálculo idempotente de volumen y PRs; borrado con limpieza de PRs.
- [x] Página de PRs con actuales, histórico colapsable y filtro por ejercicio.
- [x] Panel de estadísticas 4/12/24 semanas: volumen/sesiones por semana, racha,
      comparativa y distribución muscular. Gráficas ligeras en SVG/CSS, sin dependencia nueva.
- [x] e1RM por ejercicio en detalle, 52 semanas, solo series con reps ≤ 12 (RN-03).
- [x] Cuerpo: alta/edición/borrado de peso y perímetros, una medición por día con
      confirmación de sobrescritura, evolución SVG y tabla reciente.
- [x] RF-066 y RF-067 quedan para M7/backlog (SHOULD/COULD).

**DoD:** stats contrastadas con fixtures del E2E; recálculo tras editar/eliminar una
sesión pasada testado (PR baja/desaparece). `v0.6.0`.

## M6 · Semana 7 — PWA, i18n, a11y, pulido

**Estado:** ✅ código completo · validación en Android físico pendiente de ejecución manual.

Entregables:
- [x] PWA: manifest, iconos/splash, service worker app shell (ADR-0009), banner offline
      con reintento y registro solo en producción.
- [x] i18n runtime ES/EN, `lang` dinámico y gate de paridad de claves.
- [x] i18n de errores API (`Accept-Language` + catálogo ES/EN) y emails bilingües.
- [x] A11y: skip-link, foco visible, `prefers-reduced-motion`, navegación por teclado
      y axe-core en E2E (sin violaciones críticas).
- [x] Tema claro aplicado sobre la UI; onboarding de 3 tarjetas + rutinas de ejemplo.
- [x] Cuenta demo (`demo@ferro.app` / `FerroDemo26`) con 12 semanas sintéticas,
      activable con `ferro.demo.enabled` y banner de datos ficticios.
- [x] Lighthouse CI de rendimiento/best-practices configurado y verde (job `lighthouse`).

**DoD:** Lighthouse gate verde en CI y gates automatizados completos. La instalación en
Android real queda como verificación manual del usuario; no se puede certificar desde CI. `v0.7.0`.

## M7 · Semana 8 — Producción y publicación

Entregables:
- [ ] VM Oracle (eu-madrid-1) + DuckDNS + Caddy TLS + compose prod + runbook
      `infra/RUNBOOK.md` (setup, restore backup, upgrade, rollback).
- [ ] GitHub Actions deploy: imagen GHCR (multi-arch amd64/arm64) + SSH deploy +
      migraciones automáticas; secretos GitHub configurados.
- [ ] Prometheus/Grafana en prod con dashboards provisionados; UptimeRobot.
- [ ] Backups: cron pg_dump diario + semanal Object Storage + **prueba real de
      restore** (RNF-11) documentada con tiempos.
- [ ] Resumen semanal email (RF-066) + job PIT semanal (RNF-43) + smoke k6 (RNF-01/02).
- [ ] README final: capturas/gif del flujo completo, arquitectura, badges reales,
      enlace demo + cuenta demo; ADR post-mortem «lecciones» opcional.

**DoD:** E1–E7 de la visión verificados uno a uno; deploy desde `main` en un click
probado dos veces; `git tag v1.0.0`. **Publicación**: repo público ya (desde M0),
ahora difusión (CV, LinkedIn, comunidades).

---

## Mapa criterios de éxito → hito

| Criterio (doc 01 §7) | Se verifica en |
|---|---|
| E1 MVP en producción | M7 |
| E2 RF MUST con E2E | M6 (suite completa) |
| E3 cobertura core ≥ 80 % | M1 (identity), M3 (training) — gates CI desde entonces |
| E4 PIT ≥ 70 % dominio | job semanal desde M3, verificado M7 |
| E5 0 violaciones ArchUnit | M0 en adelante |
| E6 dogfooding 4 semanas | empieza M3 (fin de M7 cumple) |
| E7 README que vende | M7 |
