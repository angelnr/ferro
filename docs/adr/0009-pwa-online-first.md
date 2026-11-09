# ADR-0009 · PWA online-first con app shell cacheada (sin offline-first)

**Estado:** Aceptada · 2026-11-09 · decidida con el cliente (ronda 1)

## Contexto

Uso principal: gimnasio con cobertura móvil habitual pero no garantizada. El
offline-first completo (registrar series sin red y sincronizar después con resolución
de conflictos) fue explícitamente descartado por el cliente para el MVP por complejidad.

## Decisión

1. **PWA instalable** (manifest, iconos, splash, display standalone) — RNF-32.
2. **Service worker con estrategia app-shell**: precache del shell (index, CSS/JS
   del arranque, iconos, traducciones) → la app abre sin red y muestra el estado
   «sin conexión» con reintentos (RNF-13). Los **datos siempre viajan a la API**
   (RN-22: persistencia inmediata por serie).
3. Datos de navegación reciente (historial, rutinas) cacheados en memoria del store
   durante la sesión viva (ya cargados al entrar), sin persistencia deliberada en
   IndexedDB en MVP.
4. El estado del timer es local y funciona con pantalla bloqueada (RF-038.5), que es
   el único componente realmente crítico sin red.

## Alternativas descartadas

| Alternativa | Por qué no |
|---|---|
| Offline-first con cola IndexedDB + sync + resolución de conflictos (LWW por recurso, reordenaciones) | Complejidad alta (estado sincronizable, UI de conflictos, tests de convergencia) para un MVP de 8 semanas; riesgo de pérdida de datos del usuario real; ya priorizada como v2 (#3 del backlog) |
| Sin PWA (solo web) | El caso de uso es el móvil; la instalabilidad y el timer en pantalla de bloqueo requieren PWA |

## Consecuencias

- **Positivas:** modelo mental simple y robusto (una fuente de verdad: la API);
  ningún conflicto posible en MVP; la PWA ya es instalable y rápida.
- **Negativas:** sin red no se pueden registrar series (solo consultar lo ya
  cargado) — limitación conocida y comunicada en la UI del estado offline.
- **Puente a v2:** RN-22 (persistencia por serie) y la API granular por recurso
  hacen viable la cola offline futura sin rediseñar el contrato.
