# ADR-0002 · DDD táctico + arquitectura hexagonal por módulo, con Spring Modulith

**Estado:** Aceptada · 2026-11-09 · Decidida con el cliente (ronda técnica)

## Contexto

El cliente preguntó si «OOP o DDD» (no son excluyentes: DDD es una disciplina de
modelado que se expresa con OOP) y qué profundidad aplicar. El valor de portfolio exige
que el diseño del dominio sea visible y verificable, no solo un estilo de paquetes.

## Decisión

1. **DDD táctico completo dentro de cada módulo**: agregados con invariantes
   (`WorkoutSession` es la raíz de sesiones/series y protege RN-02/08/12), value
   objects (Email, Password, Weight, Reps, Rir, SetType…), eventos de dominio,
   repositorios como puertos del dominio y casos de uso en `application`.
2. **Arquitectura hexagonal** por módulo: `domain` (puro, sin Spring salvo
   anotaciones de metadato inexistentes — cero dependencias), `application` (puertos
   y casos de uso), `infrastructure` (adaptadores JPA/JWT/SMTP/OAuth), `interfaces`
   (REST + DTOs). Reglas §4 de doc 04, verificadas por **ArchUnit** (build rojo) y
   **Spring Modulith** (límites entre módulos y verificación de eventos).
3. **Publicación de eventos transaccional** vía Modulith event publication registry
   (tabla `event_publication`): los efectos (emails, recálculos asíncronos) nunca se
   pierden si la app cae justo después del commit — un *outbox* de facto sin añadir
   infraestructura extra.
4. Un **solo artefacto Maven** con paquetes por módulo (no multi-module Maven):
   más simple de construir y suficiente para forzar límites; `shared` mínimo.

## Alternativas descartadas

| Alternativa | Por qué no |
|---|---|
| DDD-lite (paquetes por feature + entidades anémicas) | El dominio de Ferro tiene invariantes reales (sesión única, límites, tipos de serie); anemizarlo desperdicia el material de entrevista |
| Multi-module Maven (ferro-domain, ferro-app…) | Duplica ceremony de build sin beneficio real en un monolito; los límites ya los fuerzan Modulith+ArchUnit |
| CQRS completo con buses y proyecciones | El volumen de escritura es mínimo; SQL para lecturas (ADR-0005) cubre el 95 % del beneficio |
| Event sourcing | Persistir series como stream suena tentador, pero complica queries y versionado de eventos para cero beneficio de producto aquí |

## Consecuencias

- **Positivas:** dominio testeable sin Spring (unit tests puros, rápidos, base del
  PIT de RNF-43); invariantes centralizadas (imposible registrar una sesión cerrada
  «por accidente» desde otro módulo); el diseño es el diferenciador del CV.
- **Negativas:** más archivos y ceremonia que un CRUD; exige disciplina para no
  esquivar el dominio con atajos JPA.
- **Mitigaciones:** el DoD de M3 exige cobertura ≥ 80 % del módulo `training`;
  ArchUnit impide la deriva; `shared` se mantiene deliberadamente pequeño.
