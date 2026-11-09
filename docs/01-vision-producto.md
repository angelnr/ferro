# 01 · Visión de producto

| Campo | Valor |
|---|---|
| Producto | **Ferro** — tracker de entrenamientos de fuerza |
| Versión del documento | 1.0 · 2026-11-09 |
| Estado | Requisitos cerrados · código a partir de M0 |
| Propietario | Ángel (@angelnr) |

---

## 1. Idea y nombre

**Ferro** (latín *ferrum*, «hierro») es una aplicación web multiusuario para registrar
entrenamientos de fuerza e hipertrofia y analizar el progreso. Inspirada funcionalmente
en [Hevy](https://hevyapp.com), de la que adopta el núcleo (log de series, temporizador,
rutinas, PRs, estadísticas) y de la que **prescinde deliberadamente** del componente social.

## 2. Problema y motivación

Problema de usuario: las apps comerciales (Hevy, Strong) guardan tus datos en su nube
con modelos freemium, sin exportación completa y con el valor centrado en la socialización.
Ferro cubre el núcleo de tracking con una app propia, open source y auto-alojada.

Motivación secundaria (explícita): **proyecto de portfolio**. Ferro existe también para
demostrar en entrevistas técnicas capacidad de ingeniería end-to-end: requisitos,
arquitectura hexagonal + DDD, testing exhaustivo, CI/CD, observabilidad y despliegue real
en la nube. Esta doble motivación está reflejada en los criterios de éxito (§7).

## 3. Objetivos de producto (SMART)

1. **O1** — Un usuario puede registrar una sesión completa (≥3 ejercicios, ≥12 series)
   en ≤ 4 minutos de interacción efectiva, incluidos supersets y temporizador.
2. **O2** — Los PRs, el volumen y la racha se calculan automáticamente al finalizar una
   sesión, sin acción manual del usuario.
3. **O3** — La app es instalable en móvil (PWA) y usable en el gimnasio con cobertura
   regular de datos (sin modo offline-first en MVP).
4. **O4** — Un usuario nuevo puede registrarse, crear su primera rutina y completar su
   primera sesión el mismo día, sin tutorial.
5. **O5** — Despliegue real y público en producción (Vercel + Oracle Cloud) con CI/CD
   automático desde `main`, observable (métricas + dashboards) y con backups diarios.

## 4. Objetivos de portfolio (qué debe demostrar)

| Área | Evidencia |
|---|---|
| Ingeniería de requisitos | SRS completa con criterios de aceptación, glosario y backlog v2 |
| Arquitectura | Monolito modular + hexagonal + DDD táctico + Spring Modulith + 9 ADRs |
| Backend | Java 21, Spring Boot 4.1, REST + OpenAPI, seguridad (JWT, OAuth2, Argon2, rate limit) |
| Frontend | Angular 22 moderno (signals, zoneless, Signal Forms, SignalStore, PWA, i18n) |
| Datos | PostgreSQL + Flyway + SQL analítico, modelo pensado (uuidv7, índices parciales, pg_trgm) |
| Testing | 5 niveles: unit, integración Testcontainers, arquitectura ArchUnit, mutación PIT, E2E Playwright |
| DevOps | Docker multi-stage, Compose, GitHub Actions → GHCR → deploy, Prometheus/Grafana |
| Producto | App real usable por terceros, con demo pública y Swagger UI |

## 5. Usuarios y personas

**P1 · «Ángel, el levantador»** — 30 años, entrena 4–5 días/semana (PPL/upper-lower) en
gimnasio con cobertura móvil irregular. Usa Hevy a diario y echa de menos: control de sus
datos, exportación, estadísticas más claras. Es el usuario de referencia de cada historia
de usuario. Nivel técnico: usuario final, sin conocimientos de desarrollo.

**P2 · «La entrevistadora»** — Tech lead / engineering manager de una empresa europea.
Llega al repo desde el CV. En 10 minutos quiere ver: qué hace la app (README + demo),
cómo está decidida la arquitectura (ADRs), cómo se garantiza calidad (tests + CI) y si el
código es limpio. No es usuaria de la app: es lectora del repo.

## 6. Alcance

### Dentro del MVP (IN)

- Multiusuario con registro abierto: email+contraseña y OAuth2 (Google, GitHub).
- Log de entrenamientos de fuerza: ejercicios, series (reps, peso, RIR), tipos de serie
  (calentamiento, normal, fallo, drop), supersets, notas.
- Temporizador de descanso configurable y cronómetro de sesión.
- Catálogo de ~200 ejercicios pre-cargados (ES/EN) + ejercicios personalizados.
- Rutinas/plantillas reutilizables; iniciar sesión desde plantilla.
- Historial, PRs automáticos, 1RM estimado (Epley), volumen, racha, comparativas.
- Mediciones corporales: peso y perímetros con gráficas.
- PWA instalable (app shell cacheada, online para datos), i18n ES/EN, tema oscuro.
- Plataforma: Swagger UI, health checks, métricas Prometheus + Grafana, emails
  transaccionales (bienvenida, recuperación, resumen semanal), feature flags, backups.

### Fuera del MVP (OUT — ver [08-backlog-v2](./08-backlog-v2.md))

- Red social (feed, amigos, compartir) · calendario de planificación · import/export
  CSV/JSON · offline-first con sincronización · programas multi-semana con progresión
  automática · vídeos de ejercicios · apps nativas (Capacitor) · notificaciones push ·
  entrenamientos de cardio/HII/polideportivos · RPE · coach con IA.

## 7. Criterios de éxito

| # | Criterio | Medible |
|---|---|---|
| E1 | MVP desplegado en producción y usable por terceros | URL pública + cuenta demo con datos |
| E2 | Todos los RF marcados MUST implementados y verificados por E2E | Suite Playwright verde en CI |
| E3 | Cobertura ≥ 80 % en módulos core (`training`, `identity`) | JaCoCo en CI, badge en README |
| E4 | Score de mutación PIT ≥ 70 % en el dominio | Pipeline de calidad semanal |
| E5 | Sin violaciones de arquitectura | Regla ArchUnit en CI, build en rojo si se rompe |
| E6 | El autor usa Ferro para sus propios entrenamientos ≥ 4 semanas | Dogfooding real (la mejor demo) |
| E7 | README con capturas, demo y arquitectura que «vende» en < 10 min | Revisión con persona ajena |

## 8. Riesgos y mitigaciones

| Riesgo | Prob. | Impacto | Mitigación |
|---|---|---|---|
| Oracle reclama la VM idle (<20 % uso 7 días) | Media | Alto | Monitor externo (UptimeRobot) golpeando `/actuator/health` cada 5 min + revisar actividad en Grafana; region home = eu-madrid-1 |
| Capacidad ARM agotada en la región al crear la VM | Media | Medio | Reintentar / otra AD; plan B documentado (Hetzner ~4 €/mes) |
| Credenciales OAuth mal configuradas | Media | Medio | Proveedores configurados en M1 con checklist en el runbook de despliegue |
| Scope creep (añadir social/offline «porque sí») | Alta | Alto | Backlog v2 cerrado; todo lo que no está en 02-RF con [MUST] está fuera del MVP |
| Datos de salud (art. 9 RGPD) | Baja | Alto | Consentimiento explícito en registro, minimización, borrado real de cuenta, región UE, cifrado TLS |
| Solo ~18 GB libres en el servidor de desarrollo | Baja | Medio | Docker ya podado (16,5 GB) + papelera externa vaciada (43 GB); imágenes base compartidas |
| Abandono por falta de tiempo | Media | Alto | Roadmap de 8 semanas con hitos independientes; M0–M3 dejan ya una demo usable |

## 9. Definiciones y no-ambigüedad

Términos de dominio (Sesión, Serie, PR, Volumen, RIR, e1RM…) con definición única en
[09-glosario](./09-glosario.md). Las reglas de negocio numéricas (fórmulas, límites,
redondeos) están en § «Reglas de negocio» de [02-requisitos-funcionales](./02-requisitos-funcionales.md)
y son normativas para código y tests.
