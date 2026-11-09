# 03 · Requisitos no funcionales (RNF)

| Versión | 1.0 · 2026-11-09 · vinculante para diseño, código e infraestructura |

Cada RNF indica **objetivo medible** y **método de verificación** (V).

## 1 · Rendimiento

| ID | Requisito | Objetivo | V |
|---|---|---|---|
| RNF-01 | Latencia API (p95) GET típicos | ≤ 300 ms | Grafana dashboard desde Prometheus |
| RNF-02 | Latencia API (p95) escrituras (POST serie) | ≤ 400 ms | Ídem + test de carga k6 en M7 |
| RNF-03 | Cálculo de finish de sesión (incluye PRs) | ≤ 800 ms p95 | E2E timing + métricas |
| RNF-04 | Frontend: LCP en móvil 4G | ≤ 2,5 s (app shell) | Lighthouse CI ≥ 90 perf / ≥ 95 BP |
| RNF-05 | Frontend: interacción serie→persistida | ≤ 150 ms feedback visual local (optimista) | E2E + Vitest |
| RNF-06 | Bundle inicial JS | ≤ 300 KB gzip (sin contar lazy charts) | budget angular.json + CI |
| RNF-07 | Búsqueda de ejercicios (debounce 250 ms) | ≤ 250 ms p95 servidor | pg_trgm + métricas |
| RNF-08 | Arranque en frío del contenedor API | ≤ 45 s | deploy + health |

## 2 · Disponibilidad y continuidad

| ID | Requisito | Objetivo | V |
|---|---|---|---|
| RNF-09 | Disponibilidad objetivo | 99 % (best-effort single-VM; no hay SLA comercial) | UptimeRobot histórico público |
| RNF-10 | RPO (pérdida máx. de datos) | 24 h | pg_dump diario 00:05 UTC+1 |
| RNF-11 | RTO (restauración) | ≤ 4 h manual | runbook probado en M7 (restore sobre compose) |
| RNF-12 | Backups | diario (7 retención local) + semanal a Object Storage (4 retención) | cron + verificación tamaño |
| RNF-13 | Degradación elegante | si la API no responde, la app muestra estado offline con reintentos, no pantalla blanca | E2E mock |
| RNF-14 | Reinicio de VM/contenedores | política `restart: unless-stopped` + Caddy/api/pg en el mismo compose | prueba de kill en M7 |

## 3 · Seguridad y privacidad

| ID | Requisito | Objetivo | V |
|---|---|---|---|
| RNF-15 | Transporte | TLS 1.2+ obligatorio (Caddy, Let's Encrypt, HSTS) | SSL Labs A / test |
| RNF-16 | Contraseñas | Argon2id (2¹⁶ KiB, t=2, p=1) + lista comunes | test unitario encoder |
| RNF-17 | Tokens | ver RN-15; access 15 min, refresh 30 d rotado, cookie httpOnly SameSite=None Secure para refresh | tests integración |
| RNF-18 | OWASP Top 10 mitigaciones base: inyección (JPA paramétrica), XSS (Angular + CSP), CSRF (Origin check en refresh + CORS estricto), IDOR (todo recurso scopeado a user_id — test automático por endpoint), SSRF (no hay fetch de URLs de usuario), rate limiting (RN-20) | suite específica en tests integración | E2E + revisión |
| RNF-19 | Cabeceras | CSP (default-src 'self'; img-src 'self' data:; connect-src api), X-Content-Type-Options, Referrer-Policy, Permissions-Policy mínima | test de cabeceras |
| RNF-20 | CORS | allowlist exacta de orígenes (localhost:4200 dev, https://ferro.vercel.app, dominio prod) — nunca `*` con credenciales | test integración |
| RNF-21 | Secretos | fuera del repo: `.env` (dev) y variables de entorno (prod); nunca en logs; `.env.example` documentado | gitleaks en CI |
| RNF-22 | RGPD (datos de salud, art. 9) | consentimiento explícito en registro (almacenado con timestamp), minimización (solo lo definido en doc 05), derecho de acceso/borrado (RF-010 + export manual del propio usuario vía dump a petición), retención: backups 4 sem. | revisión legal-light + doc |
| RNF-23 | RGPD (general) | aviso de privacidad bilingüe en el registro; región de procesamiento UE (Oracle eu-madrid-1 + Vercel EU edge); DPA aceptado con Brevo | doc + configuración |
| RNF-24 | Auditoría | log inmutable-lite de eventos de seguridad: login OK/KO, refresh reutilizado, borrado cuenta, cambio contraseña (JSON, sin PII innecesaria) | logs JSON en stdout |

## 4 · i18n / L10n / a11y

| ID | Requisito | Objetivo | V |
|---|---|---|---|
| RNF-25 | Idiomas UI | ES (default) y EN, conmutable en runtime sin recargar | Vitest + E2E EN/ES |
| RNF-26 | Formatos | fechas/horas/números nativos por locale (`Intl`), semana ISO, zona Europe/Madrid | unit tests |
| RNF-27 | Unidades | kg/cm canónicos; presentación lb/pulgadas según perfil (RN-01) | unit tests conversiones |
| RNF-28 | Accesibilidad | WCAG 2.1 AA: navegación teclado completa (tabindex/order), foco visible, contraste ≥ 4.5:1 (tema oscuro incluido), labels/ARIA en formularios, combobox RF-025 ARIA 1.2, target táctil ≥ 44 px, `prefers-reduced-motion` | axe-core en E2E + revisión manual |
| RNF-29 | Idioma API | errores y emails localizados (RN-19) | tests integración |
| RNF-30 | Dirección de texto | LTR en MVP; sin RTL (backlog v2) | — |

## 5 · Compatibilidad

| ID | Requisito | Objetivo |
|---|---|---|
| RNF-31 | Navegadores | últimas 2 versiones de Chrome, Edge, Firefox, Safari (móvil y escritorio) |
| RNF-32 | PWA instalable | Android/Chrome completa (manifest, service worker, iconos, splash); iOS/Safari: añadir a inicio de pantalla con limitaciones documentadas (sin notificaciones) |
| RNF-33 | Resoluciones | 360×640 → 1920×1080; layout mobile-first |
| RNF-34 | Multi-dispositivo | sesión abierta en varios dispositivos; última escritura gana por recurso (sin conflictos estructurales en MVP online) |

## 6 · Observabilidad y operabilidad

| ID | Requisito | Objetivo |
|---|---|---|
| RNF-35 | Métricas | RF-085; retención 15 días; dashboard reprovisionable (IaC en `infra/`) |
| RNF-36 | Logs | JSON estructurado stdout: `ts, level, logger, msg, correlationId, userId(plantado), path, status`; sin PII (email solo hasheado) |
| RNF-37 | Trazabilidad | `correlationId` generado en filtro (header `X-Correlation-Id` respetado) y propagado a logs |
| RNF-38 | Alertas | UptimeRobot → email si health falla 2×5 min; sin PagerDuty (MVP) |
| RNF-39 | Config | `application-{dev,prod}.yml` + env vars; documentado en `infra/README` (se escribe en M0) |
| RNF-40 | Migraciones | Flyway **forwards-only**; nunca se edita una migración aplicada; naming `V{n}__{snake_case}.sql` |

## 7 · Mantenibilidad y calidad

| ID | Requisito | Objetivo | V |
|---|---|---|---|
| RNF-41 | Cobertura backend módulos core | ≥ 80 % líneas (`identity`, `training`, `progress`) | JaCoCo gate CI |
| RNF-42 | Cobertura backend global | ≥ 70 % | JaCoCo |
| RNF-43 | Mutantes dominio | PIT ≥ 70 % en paquetes `..domain..` | job semanal + gate blando |
| RNF-44 | Arquitectura | 0 violaciones ArchUnit (dependencias hexagonal + límites Modulith) | test en CI (build rojo) |
| RNF-45 | Frontend tests | componentes/directivas clave cubiertos (Vitest) + stores; ≥ 70 % de stores | Vitest coverage |
| RNF-46 | E2E | happy paths críticos (registro→rutina→sesión→PR→stats; OAuth mock; body) | Playwright en CI |
| RNF-47 | Commits | Conventional Commits + squash-merge; `main` siempre verde y desplegable |
| RNF-48 | Estilo | Java: Spring Java Format (o palantir) + checkstyle CI; TS: ESLint/Prettier Angular presets |
| RNF-49 | Docs vivas | cada milestone actualiza README/ADRs si cambia una decisión; ADR nuevo por decisión relevante |
| RNF-50 | API estabilidad | `/api/v1` sin breaking changes durante MVP; aditivos permitidos; errores siempre RFC 7807 |

## 8 · Escalabilidad (objetivo honesto)

| ID | Requisito |
|---|---|
| RNF-51 | Dimensionado para **~100 usuarios activos/día y picos de 20 rps** en la VM ARM (2 OCPU/12 GB). Sin autoscaling: si crece, escalado vertical o extracción de módulos según ADR-0001 |
| RNF-52 | Paginación obligatoria en toda lista (≤ 100/pág); N+1 prohibido en JPA (verificación con `hibernate.query.plan` en logs dev + revisión PR) |
| RNF-53 | Caché de catálogo global (Caffeine, TTL 10 min) — es immutable en runtime |

## 9 · Legal y licencias

| ID | Requisito |
|---|---|
| RNF-54 | Código propio bajo MIT; dependencias OSS compatibles (tabla de licencias revisada en M0: Spring Apache-2.0, Angular MIT, PostgreSQL-like, Grafana AGPLv3 **solo como contenedor no modificado** — redistribución perjudicial ninguna, uso interno) |
| RNF-55 | Marca: «Ferro» como nombre de proyecto personal; comprobar disponibilidad de dominio si algún día se publica ampliamente (no bloquea MVP) |
| RNF-56 | Datos sintéticos demo: generados, sin PII real |
