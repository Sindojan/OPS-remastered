#!/bin/bash
set -e

echo "Owlsburg OPS – Produktion starten"
echo ""

cd "$(dirname "$0")/.."

if [ ! -f .env ]; then
  echo "FEHLER: .env Datei fehlt."
  echo "Kopiere .env.example nach .env und fulle die Werte aus."
  exit 1
fi

docker compose build
docker compose up -d

echo ""
echo "Owlsburg OPS laeuft auf Port ${HTTP_PORT:-80}"
docker compose ps
