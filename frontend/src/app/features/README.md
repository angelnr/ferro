# Features (frontend)

Un directorio por funcionalidad de negocio, creado cuando empiece su milestone:

```text
features/
├── auth/          # M1 · identity
├── workouts/      # M3 · training (sesión en vivo)
├── routines/      # M4 · training
├── progress/      # M5 · progress
└── body/          # M5 · body
```

Convención: componentes standalone, estado con SignalStore en `*.store.ts`, acceso al API
en `*.api.ts`, tests junto al fichero (`*.spec.ts`). Lo transversal vive en `core/`
(interceptores, guards, i18n, tema) y lo reutilizable sin dominio en `shared/`.
