#!/bin/bash
set -e

cd "$(dirname "$0")/.."

if [ -z "$1" ]; then
  echo "Verwendung: ./scripts/restore.sh <backup-ordner>"
  echo "Beispiel:   ./scripts/restore.sh ./backups/20260224_120000"
  echo ""
  echo "Vorhandene Backups:"
  ls -d ./backups/*/ 2>/dev/null || echo "  Keine Backups gefunden"
  exit 1
fi

BACKUP_DIR="$1"

if [ ! -f "${BACKUP_DIR}/database.dump" ]; then
  echo "FEHLER: ${BACKUP_DIR}/database.dump nicht gefunden"
  exit 1
fi

echo "Owlsburg OPS – Restore aus ${BACKUP_DIR}"
echo "ACHTUNG: Bestehende Daten werden ueberschrieben!"
read -p "Fortfahren? (j/N) " confirm
if [ "$confirm" != "j" ]; then
  echo "Abgebrochen."
  exit 0
fi

# PostgreSQL Restore
echo ""
echo "PostgreSQL Restore..."
docker compose exec -T postgres pg_restore \
  -U "${DB_USER:-owlsburg_app}" \
  -d "${DB_NAME:-owlsburg_ops}" \
  --clean --if-exists \
  < "${BACKUP_DIR}/database.dump"
echo "  Datenbank wiederhergestellt"

# MinIO Restore
if [ -f "${BACKUP_DIR}/minio_data.tar" ]; then
  echo "MinIO Restore..."
  docker compose exec -T minio sh -c 'rm -rf /data/* && tar -xf - -C /' \
    < "${BACKUP_DIR}/minio_data.tar"
  echo "  MinIO-Daten wiederhergestellt"
fi

echo ""
echo "Restore abgeschlossen. Backend neu starten empfohlen:"
echo "  docker compose restart backend"
