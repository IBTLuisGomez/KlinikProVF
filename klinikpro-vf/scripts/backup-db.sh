#!/usr/bin/env bash
# Backup de la base de datos Postgres del contenedor docker-compose (klinikpro-v1-db).
# Uso: ./scripts/backup-db.sh   (desde klinikpro-vf/, o desde cualquier lado si pasas la ruta completa)
# Requiere: el contenedor "klinikpro-v1-db" corriendo (docker compose up db).
set -euo pipefail

CONTAINER="${DB_CONTAINER:-klinikpro-v1-db}"
DB_NAME="${POSTGRES_DB:-klinikpro}"
DB_USER="${POSTGRES_USER:-klinik}"
OUT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)/scripts/backups"
mkdir -p "$OUT_DIR"

TIMESTAMP="$(date +%Y%m%d_%H%M%S)"
OUT_FILE="$OUT_DIR/klinikpro_${TIMESTAMP}.sql"

echo "Respaldando $DB_NAME (contenedor $CONTAINER) -> $OUT_FILE"
docker exec "$CONTAINER" pg_dump -U "$DB_USER" "$DB_NAME" > "$OUT_FILE"
echo "Listo: $OUT_FILE ($(du -h "$OUT_FILE" | cut -f1))"

# Retención simple: conserva los últimos 14 respaldos, borra el resto.
KEEP=14
ls -1t "$OUT_DIR"/klinikpro_*.sql 2>/dev/null | tail -n +$((KEEP + 1)) | xargs -r rm --
