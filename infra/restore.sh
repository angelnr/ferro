#!/usr/bin/env bash
# Restaura un backup .dump.gz. DESTRUCTIVO: pide confirmación explícita.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
COMPOSE="$ROOT/infra/docker-compose.prod.yml"
FILE="${1:?Uso: ./infra/restore.sh /ruta/ferro-....dump.gz}"

read -r -p "Esto reemplaza la base de datos de producción. Escribe RESTORE: " answer
[[ "$answer" == "RESTORE" ]] || { echo "Cancelado"; exit 1; }

gunzip -c "$FILE" | docker compose -f "$COMPOSE" exec -T postgres \
  pg_restore -U "${POSTGRES_USER:-ferro}" -d "${POSTGRES_DB:-ferro}" --clean --if-exists --no-owner
echo "Restore completado desde $FILE"
