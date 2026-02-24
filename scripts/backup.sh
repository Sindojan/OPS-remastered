#!/bin/bash
set -e

cd "$(dirname "$0")/.."

TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="./backups/${TIMESTAMP}"
mkdir -p "${BACKUP_DIR}"

echo "Owlsburg OPS – Backup starten"
echo "Ziel: ${BACKUP_DIR}"
echo ""

# PostgreSQL Dump
echo "PostgreSQL Backup..."
docker compose exec -T postgres pg_dump \
  -U "${DB_USER:-owlsburg_app}" \
  -d "${DB_NAME:-owlsburg_ops}" \
  --format=custom \
  > "${BACKUP_DIR}/database.dump"
echo "  database.dump erstellt"

# MinIO Daten (Volume kopieren)
echo "MinIO Backup..."
docker compose exec -T minio sh -c 'tar -cf - /data' \
  > "${BACKUP_DIR}/minio_data.tar"
echo "  minio_data.tar erstellt"

echo ""
echo "Backup abgeschlossen: ${BACKUP_DIR}"
ls -lh "${BACKUP_DIR}/"
