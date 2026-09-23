#!/usr/bin/env bash
# Restaura un respaldo generado por backup-db.sh dentro del contenedor
# klinikpro-v1-db. ¡CUIDADO! esto sobreescribe los datos actuales.
# Uso: ./scripts/restore-db.sh scripts/backups/klinikpro_20260101_120000.sql
set -euo pipefail

if [ $# -ne 1 ]; then
  echo "Uso: $0 <archivo.sql>" >&2
  exit 1
fi

BACKUP_FILE="$1"
CONTAINER="${DB_CONTAINER:-klinikpro-v1-db}"
DB_NAME="${POSTGRES_DB:-klinikpro}"
DB_USER="${POSTGRES_USER:-klinik}"

if [ ! -f "$BACKUP_FILE" ]; then
  echo "No existe: $BACKUP_FILE" >&2
  exit 1
fi

read -r -p "Esto SOBREESCRIBE la base '$DB_NAME' en el contenedor '$CONTAINER'. ¿Continuar? [y/N] " CONFIRM
if [ "$CONFIRM" != "y" ] && [ "$CONFIRM" != "Y" ]; then
  echo "Cancelado."
  exit 1
fi

cat "$BACKUP_FILE" | docker exec -i "$CONTAINER" psql -U "$DB_USER" "$DB_NAME"
echo "Restaurado desde $BACKUP_FILE"
