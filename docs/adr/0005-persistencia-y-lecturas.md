# ADR-0005 · Persistencia: JPA para escrituras, SQL nativo para lecturas analíticas

**Estado:** Aceptada · 2026-11-09 · decidida con el cliente (ronda 2)

## Contexto

El dominio tiene una asimetría clara: **escrituras pequeñas y transaccionales**
(registrar una serie) frente a **lecturas analíticas** (volumen por semana, e1RM por
sesión, distribución por grupo muscular). El cliente descartó jOOQ (más superficie)
y eligió «JPA + SQL para stats».

## Decisión

1. **Escrituras y lecturas por agregado** → Spring Data JPA (Hibernate 7): mapeo del
   agregado `WorkoutSession` con sus `SessionExercise`/`SessionSet` (fetch joins
   controlados, N+1 verificado con logs dev — RNF-52).
2. **Lecturas analíticas y listados** → SQL nativo (`JdbcTemplate` /
   `@Query(nativeQuery)`) con **proyecciones a DTO/interface**, sin pasar entidades
   JPA: volumen por semana (`date_trunc('isoweeks', …)`), streak (window functions),
   distribución por grupo muscular, e1RM por sesión.
3. **Unidades canónicas SI** en BD (RN-01); conversión solo en el borde de la API.
4. **Flyway forwards-only** (RNF-40): el esquema de doc 05 es la especificación.
5. **UUID v7 en BD** (`uuidv7()`, PG 18) — no gestionados por la app.
6. **`pg_trgm`** para búsqueda tolerante de ejercicios; **Caffeine** (TTL 10 min)
   para el catálogo global inmutable (RNF-53).
7. `total_volume` denormalizado en `workout_sessions` recalculado en escritura
   (doc 05 §6.1) para listados baratos.

## Alternativas descartadas

| Alternativa | Por qué no |
|---|---|
| jOOQ 3.20 para todas las lecturas | Suma CV (type-safe SQL), pero añade una segunda mentalidad de persistencia para ~8 queries analíticas que caben en SQL nativo con tests de integración |
| CQRS con proyecciones materializadas | No hay volumen; las mismas tablas sirven; sería teatro de arquitectura |
| MyBatis | Menos común en el mercado objetivo del cliente |
| Lecturas vía JPA/JPQL para todo | Volumen por semana o streak en JPQL: frágil y lento; SQL es la herramienta correcta y además demuestra saber SQL de verdad |

## Consecuencias

- **Positivas:** cada lado usa la herramienta óptima; el SQL analítico queda
  cubierto por Testcontainers con fixtures deterministas; menos fricción de
  mapping en listados.
- **Negativas:** dos estilos de acceso a datos; el SQL nativo es texto
  (sin tipado) → los tests de integración son obligatorios para no romperlo.
- **Mitigaciones:** convención de paquetes `infrastructure/readmodel/` para
  separar lecturas; queries en archivos con nombre (no strings sueltos en el código).
