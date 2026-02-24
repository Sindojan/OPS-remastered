# Owlsburg OPS – Deployment

## Entwicklung starten

```bash
./scripts/dev.sh
```

Startet PostgreSQL + MinIO als Docker Container. Danach:

```bash
# Backend (separates Terminal)
cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Frontend (separates Terminal)
cd frontend && npx next dev --port 4201
```

- Frontend: http://localhost:4201
- Backend: http://localhost:8080
- MinIO Console: http://localhost:9002

## Produktion deployen

```bash
# 1. Konfiguration erstellen
cp .env.example .env
# Werte in .env ausfuellen (sichere Passwoerter!)

# 2. Starten
./scripts/prod.sh
```

- Anwendung: http://localhost (Port 80)
- Alle Services hinter Nginx Reverse Proxy

## Monitoring starten (optional)

```bash
docker compose -f docker-compose.monitoring.yml up -d
```

- Grafana: http://localhost:3030 (admin / admin)
- Prometheus: http://localhost:9090

## Backup

```bash
./scripts/backup.sh
# Backups in ./backups/<timestamp>/
```

## Restore

```bash
./scripts/restore.sh ./backups/20260224_120000
```

## Logs anzeigen

```bash
docker compose logs -f backend
docker compose logs -f frontend
docker compose logs -f nginx
```

## Services Status

```bash
docker compose ps
```
