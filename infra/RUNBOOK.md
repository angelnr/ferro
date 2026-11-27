# Runbook de producción · Ferro

> Los valores marcados como **externo** requieren acceso a la VM, DNS o secretos reales.

## 1 · Provisionar la VM

1. VM Oracle Cloud (Ampere/amd64) con Ubuntu 24.04, 2 vCPU/4 GB mínimo.
2. DNS `api.<dominio>` apuntando a la IP pública (DuckDNS vale para demo).
3. Abrir en el firewall solo 22/80/443; Postgres y Grafana quedan privados.
4. Instalar Docker Engine + compose plugin y añadir el usuario al grupo `docker`.
5. Clonar el repo en `/opt/ferro` y crear `infra/.env` desde `.env.example`:
   `POSTGRES_PASSWORD`, `JWT_SECRET` (>=32 bytes), `FERRO_DOMAIN`, `IMAGE_TAG`,
   `MAIL_*` de Brevo, OAuth, `GRAFANA_PASSWORD`, `FERRO_DEMO_ENABLED=true` en prod-demo.

## 2 · Desplegar

```bash
cd /opt/ferro
docker compose -f infra/docker-compose.prod.yml pull
docker compose -f infra/docker-compose.prod.yml up -d
docker compose -f infra/docker-compose.prod.yml ps
curl -fsS https://api.<dominio>/actuator/health/readiness
```

Flyway aplica migraciones al arrancar. El workflow `deploy.yml` hace pull+up por SSH
si están configurados `DEPLOY_HOST`, `DEPLOY_USER`, `DEPLOY_SSH_KEY` y `GHCR_TOKEN`.

## 3 · Observabilidad

- Prometheus: `docker compose ... logs -f prometheus` (scrapea `api:8080`).
- Grafana: túnel SSH `ssh -L 3000:127.0.0.1:3000 ubuntu@vm` → http://localhost:3000.
- Logs JSON en stdout; cada petición lleva `X-Correlation-Id`.
- Smoke: `k6 run -e FERRO_BASE_URL=https://api.<dominio> infra/k6/smoke.js`.

## 3.1 · Prueba de carga

```bash
k6 run -e FERRO_BASE_URL=https://api.<dominio> infra/k6/load.js
k6 run -e FERRO_BASE_URL=https://api.<dominio> infra/k6/write.js
```

## 4 · Backups y restore

```bash
# cron diario 03:30 UTC
30 3 * * * /opt/ferro/infra/backup.sh /var/backups/ferro
# restore destructivo, probar siempre en staging primero
/opt/ferro/infra/restore.sh /var/backups/ferro/ferro-....dump.gz
```

Prueba de restore obligatoria: crear una BD vacía, restaurar el último dump y comparar
`count(*)` de `users`, `workout_sessions` y `session_sets`.

## 5 · Upgrade y rollback

1. `docker compose ... pull` con el nuevo `IMAGE_TAG`.
2. `docker compose ... up -d` (Flyway forwards-only; nunca editar migraciones).
3. Verificar readiness y smoke.
4. Rollback: volver `IMAGE_TAG` anterior y `up -d`; si hay migración incompatible,
   restaurar el backup previo con `restore.sh`.

## 6 · Pendientes externos

- Instalación PWA en Android real (`v0.7.0` quedó etiquetada con esta validación pendiente).
- Restore real y prueba de kill de contenedores (RNF-14) en la VM.
- Rotación de secretos y alertas UptimeRobot.
