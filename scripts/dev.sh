#!/bin/bash
set -e

echo "Owlsburg OPS – Dev-Modus starten"
echo ""

cd "$(dirname "$0")/.."

docker compose -f docker-compose.yml -f docker-compose.dev.yml --env-file .env.dev up -d postgres minio

echo ""
echo "PostgreSQL auf localhost:5432"
echo "MinIO S3 auf localhost:9000 (Console: localhost:9002)"
echo ""
echo "Jetzt starten:"
echo "  Backend:  cd backend && JAVA_HOME=\$(/usr/libexec/java_home -v 21) ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev"
echo "  Frontend: cd frontend && npx next dev --port 4201"
