# Infraestructura de desarrollo

Servicios locales para M0. Todo escucha solo en `127.0.0.1` y no se despliega tal cual
en producción.

```bash
docker compose -f infra/docker-compose.dev.yml up -d
docker compose -f infra/docker-compose.dev.yml ps
docker compose -f infra/docker-compose.dev.yml logs -f postgres
docker compose -f infra/docker-compose.dev.yml down
```

| Servicio | URL / puerto | Credenciales |
|---|---|---|
| PostgreSQL 18 | `127.0.0.1:5432` · BD `ferro` | `ferro` / `ferro` |
| Mailpit (SMTP) | `127.0.0.1:1025` | sin auth |
| Mailpit (UI) | http://127.0.0.1:8025 | — |
| Prometheus | http://127.0.0.1:9090 | — |
| Grafana | http://127.0.0.1:3003 | `admin` / `ferro` |

Grafana provisiona el datasource de Prometheus y el dashboard `Ferro · API` desde
`infra/grafana/`. Prometheus scrapea `host.docker.internal:8080/actuator/prometheus`;
el backend debe estar corriendo en el host (o con `--network host`).

## Reset de base de datos

Flyway es forwards-only y `clean` está deshabilitado. Para empezar de cero en local:

```bash
docker compose -f infra/docker-compose.dev.yml down -v
docker compose -f infra/docker-compose.dev.yml up -d postgres
```

Las migraciones viven en `backend/src/main/resources/db/migration/` y las aplica el
backend al arrancar (o el test de Testcontainers en CI).

## Producción (borrador M0)

```bash
cp infra/.env.example infra/.env
# edita infra/.env: POSTGRES_PASSWORD, JWT_SECRET (>=32 bytes), FERRO_DOMAIN e IMAGE_TAG
docker compose -f infra/docker-compose.prod.yml up -d --build
```

- `api`: imagen multi-stage de `backend/Dockerfile` (JRE 21, usuario no root, healthcheck).
- `postgres`: Postgres 18 sin puerto publicado; solo accesible por la red de Compose.
- `caddy`: TLS automático y reverse proxy a `api:8080`.
- El frontend se despliega aparte en Vercel (ADR-0009); este compose no lo sirve.

Es un borrador funcional, no el runbook final de M7: faltan backups, métricas de
producción, límites de recursos y pruebas de restore.
