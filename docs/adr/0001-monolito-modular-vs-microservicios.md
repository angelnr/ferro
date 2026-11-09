# ADR-0001 · Monolito modular en lugar de microservicios

**Estado:** Aceptada · 2026-11-09 · Decidida con el cliente (ronda técnica)

## Contexto

Ferro es una aplicación multiusuario de tamaño personal/small (RNF-51: ~100 usuarios
activos/día, 20 rps pico) construida por una sola persona como proyecto de portfolio,
con horizonte de 8 semanas para el MVP. El cliente preguntó explícitamente si plantearla
como microservicios (posiblemente para «sumar» en el CV) y alojarla en Oracle Cloud.

## Decisión

Construir **un monolito modular**: un único despliegue Spring Boot con módulos internos
por bounded context (doc 04 §4), límites verificados por Spring Modulith + ArchUnit, y
comunicación inter-módulos por API pública y eventos de dominio. Un solo contenedor +
un solo PostgreSQL en producción.

## Alternativas descartadas

| Alternativa | Por qué no |
|---|---|
| Microservicios (auth/training/analytics + gateway) | Para este dominio y equipo: sobrecoste operativo ×4 (despliegues, red, observabilidad distribuida) sin ninguno de sus beneficios (escala independiente, equipos paralelos, despliegue parcial). En entrevistas invita a la pregunta «¿por qué microservicios?» sin respuesta defendible; un monolito modular con ADR demuestra más criterio |
| Monolito en capas clásico (controller-service-repository) | Más rápido de escribir, pero no modela el dominio (no hay lugar para invariantes de `WorkoutSession`), no diferencia lectura/escritura y es indistinguible de un CRUD tutorial |
| Serverless (Cloud Run / Lambda) | Frío inaceptable para interacción de gym (timer), ecosistema Java serverless inmaduro en Vercel (el cliente lo planteó; ver ADR-0004) |

## Consecuencias

- **Positivas:** un solo pipeline CI/CD; transacciones ACID reales entre módulos
  (p. ej. finish + PRs en una transacción); depuración local trivial con Compose;
  la modularidad impuesta (Modulith/ArchUnit) deja el camino abierto a extraer
  `progress`/emails si algún día hace falta (límites ya respetados).
- **Negativas:** no hay aislamiento de fallos por módulo; escalar = escalar todo.
- **Mitigaciones:** RNF-51 declara el dimensionado honesto; los eventos de dominio con
  publicación transaccional (ADR-0002) evitan acoplamiento temporal; el arranque
  compartido es un contenedor único de < 500 MB.
