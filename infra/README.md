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
