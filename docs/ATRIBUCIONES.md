# Atribuciones de imágenes de ejercicios

Las imágenes reales del catálogo proceden de **RepDB · Exercise Dataset (free tier)**:

- Proyecto: https://github.com/RepDB/exercise-dataset
- Web: https://repdb.co
- Licencia: free tier, uso en aplicaciones con atribución visible.
- Imágenes WebP: `frontend/public/exercises/`.

Los ejercicios sin una coincidencia fiable tienen una **imagen WebP generada por Ferro**
con el nombre del ejercicio y su grupo muscular. No son fotografías de ejecución.

Atribución requerida en la aplicación:

> Exercise data by RepDB (repdb.co)

La atribución está incluida en este documento y enlazada desde el README. Antes de
publicar comercialmente, revisa los términos vigentes de RepDB y conserva esta página.

## Pipeline de imágenes

```bash
python3 scripts/exercise-images.py          # genera CSV y HTML de revisión
python3 scripts/exercise-images.py --apply  # aplica auto + approved=yes
```

- `docs/exercise-image-review.csv`: una fila por ejercicio; cambia `approved` a `yes/no`.
- `docs/exercise-image-review.html`: contact sheet para revisar candidatos.
- Las coincidencias exactas se aprueban automáticamente; las dudosas nunca se aplican
  sin revisión.
