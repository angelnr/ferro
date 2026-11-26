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

**Estado:** 🚧 casi completo · seed de 207 ejercicios, búsqueda con filtros/paginación,
CRUD/archivado custom, detalle, página de catálogo con creación inline y E2E. Falta el
combobox reutilizable para rutinas y estadísticas reales del detalle.

Entregables:
- [ ] `docs/seed-ejercicios.csv` (~200 filas) + validador en CI + Flyway V2 seed.
- [ ] Búsqueda trgm + filtros + paginación; custom CRUD + archivado (RF-020…024).
- [ ] Componente selector/combobox accesible (RF-025) con debounce y creación inline.
- [ ] Detalle de ejercicio con stats «sin datos» + CTA.

**DoD:** búsqueda «press banca»/«banca»/«BENCH» funciona (test); límite 100 custom
testado; E2E crear-rutina-aura sin fricción. `v0.3.0`.

## M3 · Semanas 3–5 — Training core (`training`) ⭐ corazón del producto

Entregables (RF-030…042):
- [ ] Sesión vacía + única-activa (RN-08 garantizado por índice parcial, test incluido).
- [ ] Ejercicios en sesión: añadir/reordenar/eliminar/supersets/notas (RF-032).
- [ ] Series: registro inmediato (RN-22), tipos (RN-11/34), edición/eliminación,
      pre-relleno, series objetivo pendientes cuando venga de plantilla (se activa en M4).
- [ ] Performance anterior al registrar (RF-037).
- [ ] Temporizador descanso (RF-038: autoarranque, +15 s, skip, background-safe,
      notificación+vibración) y cronómetro (RF-039).
- [ ] Finish: volumen, PRs (RN-05, devueltos para el resumen), `SessionCompleted`.
- [ ] Descartar, recuperar activa (RF-042), auto-cierre 24 h (RN-09, job + test de
      reloj con Clock inyectado).
- [ ] UI en vivo mobile-first pulida (dark, target 44 px, feedback optimista RNF-05).

**DoD:** E2E maestro «entrenamiento completo»: crear sesión → 3 ejercicios → supersets →
12 series → timer → finish → PRs visibles. Cobertura `training` ≥ 80 %. Latencia finish
≤ 800 ms (métrica). `v0.4.0` — **desde aquí Ferro ya es usable a diario (dogfooding E6)**.
**Riesgo:** es el hito más grande → subdividir PRs por feature; si hay retraso, lo que
no puede moverse es RF-033/040.

## M4 · Semana 5 — Rutinas (`training`)

Entregables: RF-050…054 (CRUD + duplicar + builder series objetivo + arrancar desde
plantilla + límites RN-12). Integración con RF-031 (pre-carga pendientes).

**DoD:** E2E «rutina → sesión»: crear PPL, arrancar, las series objetivo aparecen
pendientes y al confirmar se completan. 409 duplicado nombre testado. `v0.5.0`.

## M5 · Semana 6 — Progreso y Cuerpo (`progress`, `body`)

Entregables: RF-060…073 (historial paginado, detalle, edición limitada con recálculo,
página PRs, panel stats con rango 4/12/24, streak RN-06, e1RM por ejercicio,
comparativa; peso y medidas con gráficas y edición). Gráficas con librería (ECharts
`ngx-echarts` o Chart.js — decidir en PR con spike de 1 h, criterio: bundle y a11y).

**DoD:** stats cuadran con cálculo manual del E2E maestro (fixtures deterministas);
recálculo tras editar sesión pasada testado (PR baja → desaparece de activos).
`v0.6.0`.

## M6 · Semana 7 — PWA, i18n, a11y, pulido

Entregables:
- [ ] PWA: manifest, service worker app shell (ADR-0009), instalable Android,
      banner «sin conexión» (RNF-13), iconos + splash.
- [ ] i18n ES/EN completo (runtime, ADR-0008) incluidos emails y errores API.
- [ ] a11y: axe-core en E2E sin violaciones críticas; foco/teclado revisado.
- [ ] Tema claro opcional; onboarding RF-088; cuenta demo + datos sintéticos (RF-087).
- [ ] Lighthouse CI: perf ≥ 90, BP ≥ 95, budget bundle (RNF-06).

**DoD:** instalada en Android real y usada en el gimnasio (E6 sigue); Lighthouse
gate verde en CI. `v0.7.0`.

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
