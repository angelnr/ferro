# 08 · Backlog v2 (fuera del MVP, deliberadamente)

Nada de esta lista entra en el MVP aunque «quede tiempo»: el MVP termina en M7 con los
RF MUST/SHOULD de doc 02. Prioridad de v2 (orden aproximado de valor):

| # | Feature | Por qué importa | Dependencias |
|---|---|---|---|
| 1 | **Import/export completo** (CSV/JSON + importador de exportaciones de Hevy/Strong) | Cierre del círculo «tus datos son tuyos» y killer feature para captar usuarios reales de Hevy | ninguno |
| 2 | **Calendario de planificación** (RF desestimado: programar sesiones futuras por día) | Segundo uso más pedido en trackers | rutinas |
| 3 | **Offline-first con sincronización** (cola local + resolución de conflictos por serie, LWW por recurso) | El gimnasio sin cobertura; gran tema técnico para entrevistas | arquitectura de eventos ya lista |
| 4 | **Compartir resumen** (link público e imagen generada del entreno) | Viralidad mínima sin red social completa | sesiones |
| 5 | **Programas multi-semana con progresión** (plantillas encadenadas + sobrecarga sugerida basada en e1RM/RIR) | Diferenciador de coaching | stats |
| 6 | RPE además de RIR | completitud de fatiga percibida | — |
| 7 | Notificaciones push (Web Push + VAPID) | retención (PWA Android) | PWA |
| 8 | Apps nativas con Capacitor (iOS notificaciones reales) | iOS limita PWA | PWA |
| 9 | Vídeos/GIFs de técnica por ejercicio | onboarding de ejercicios | catálogo |
| 10 | Cardio/HIIT (distancia, ritmo, FC con zonas) | abre el dominio polideportivo | modelo de series por tipo de métrica |
| 11 | Export open API de estadísticas (webhooks) | integraciones | eventos |
| 12 | Admin mínimo (gestión flags, users read-only) | operación más allá del autor | flags ya |
| 13 | Postgres exporter + dashboard SQL | observabilidad completa | Prometheus |
| 14 | k6 en CI con perfil de carga | escalabilidad evidenciada | smoke M7 |
| 15 | «Coach» con IA (sugerencias de progresión) | actualidad, pero solo con datos reales suficientes | varios |
| 16 | Zonas horarias configurables por usuario (travel) | pulido | time_zone ya en modelo |
| 17 | RTL/idiomas adicionales | alcance i18n listo (@ngx-translate) | i18n M6 |
| 18 | 2FA (TOTP) | seguridad avanzada | identity |

Registro de decisiones de scope:

- 2026-11-09 · Encuesta de alcance con el cliente: fuera del MVP → social, calendario,
  import/export, offline-first, cardio, RPE (respuesta del 2026-11-09 a la ronda 1).
- 2026-11-09 · El export JSON individual se posterga de RF-011 a v2; el derecho de
  acceso RGPD se cubre en MVP con dump manual a petición (RNF-22).
