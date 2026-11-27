#!/usr/bin/env bash
# Backup diario de PostgreSQL. Uso: ./infra/backup.sh [directorio]
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
COMPOSE="$ROOT/infra/docker-compose.prod.yml"
DEST="${1:-/var/backups/ferro}"
STAMP="$(date -u +%Y%m%dT%H%M%SZ)"
KEEP_DAYS="${FERRO_BACKUP_KEEP_DAYS:-14}"

mkdir -p "$DEST"
docker compose -f "$COMPOSE" exec -T postgres \
  pg_dump -U "${POSTGRES_USER:-ferro}" -d "${POSTGRES_DB:-ferro}" -Fc \
  | gzip > "$DEST/ferro-$STAMP.dump.gz"

find "$DEST" -name 'ferro-*.dump.gz' -type f -mtime "+$KEEP_DAYS" -delete
ln -sfn "$DEST/ferro-$STAMP.dump.gz" "$DEST/latest.dump.gz"
echo "Backup: $DEST/ferro-$STAMP.dump.gz"
