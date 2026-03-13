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

---

## Produktion deployen (Docker Compose)

### Voraussetzungen

- Docker + Docker Compose (v2)
- Min. 4 GB RAM, 2 CPU Cores
- Für HTTPS: TLS-Zertifikat (fullchain.pem + privkey.pem)

### 1. Umgebungsvariablen konfigurieren

```bash
cp .env.example .env
```

`.env` bearbeiten und sichere Werte setzen:

```env
DB_PASSWORD=<sicheres-passwort>
MINIO_ROOT_USER=owlsburg-admin
MINIO_ROOT_PASSWORD=<sicheres-passwort>
JWT_SECRET=<openssl rand -base64 48>
ENCRYPTION_KEY=<openssl rand -base64 48>
CORS_ALLOWED_ORIGINS=http://mein-server.example.com
API_URL=http://mein-server.example.com

# System-Admin Credentials (optional)
SYSTEM_ADMIN_EMAIL=philipp.ebert@strate-software.com
SYSTEM_ADMIN_PASSWORD=<sicheres-passwort>

# System-Agent LLM API-Key (optional)
SYSTEM_LLM_API_KEY=sk-ant-...
```

Secrets generieren:
```bash
openssl rand -base64 48  # fuer JWT_SECRET
openssl rand -base64 48  # fuer ENCRYPTION_KEY
```

**Wichtig:** `API_URL` ist die externe URL des Servers (ohne `/api` Suffix).

#### Optionale Env-Vars

| Variable | Beschreibung | Default |
|----------|-------------|---------|
| `SYSTEM_ADMIN_EMAIL` | E-Mail des System-Admins | `philipp.ebert@strate-software.com` |
| `SYSTEM_ADMIN_PASSWORD` | Neues Passwort fuer System-Admin | nicht gesetzt (Migration-Default bleibt aktiv) |
| `SYSTEM_LLM_API_KEY` | Anthropic API-Key fuer System-Agents | nicht gesetzt (muss manuell ueber UI konfiguriert werden) |

- **`SYSTEM_ADMIN_PASSWORD`**: Wenn gesetzt, werden E-Mail + Passwort des System-Admins bei jedem Start ueberschrieben. Wenn nicht gesetzt, bleiben die Default-Credentials aus der Migration aktiv.
- **`SYSTEM_LLM_API_KEY`**: Wird nur beim ersten Start gesetzt, wenn noch keine LLM-Config in der DB existiert. Bereits manuell konfigurierte Keys werden nie ueberschrieben.

### 2. Starten (HTTP)

```bash
docker compose up -d
```

Erster Start dauert etwas laenger (Images bauen, DB-Migrationen).

Status pruefen:
```bash
docker compose ps
docker compose logs -f backend  # Migrations-Log beobachten
```

Health-Check:
```bash
curl http://localhost/actuator/health
# Erwartete Antwort: {"status":"UP"}
```

### 3. Erster Login

**System-Admin:**
- E-Mail: `philipp.ebert@strate-software.com` (oder `SYSTEM_ADMIN_EMAIL`)
- Passwort: `N0n3Xx.Blender` (oder `SYSTEM_ADMIN_PASSWORD` wenn gesetzt)
- Zugriff auf Systemverwaltung

**Default Tenant-Admin:**
- E-Mail: `software@sindojan.de`
- Passwort: `root1234`
- Zugriff auf Owlsburg OPS Plattform

**Passwoerter nach dem ersten Login aendern!** (oder via `.env` konfigurieren)

### 4. Company einrichten (System-Admin)

1. Als System-Admin einloggen
2. Systemverwaltung → Firmen → Company-Details pruefen
3. **LLM-Tab:** Anthropic API-Key pruefen (automatisch gesetzt wenn `SYSTEM_LLM_API_KEY` in `.env`)
4. **Module-Tab:** Gewuenschte Module aktivieren
5. **Agenten-Tab:** Agent-Status pruefen

### 5. Agent-System testen

1. Als Tenant-Admin einloggen (software@sindojan.de)
2. Agent-Button in der Sidebar klicken (CEO-Chat)
3. Nachricht senden → CEO antwortet mit Tool-Calls und Delegationen

---

## HTTPS / TLS Setup

### Zertifikate bereitstellen

```bash
mkdir -p docker/nginx/ssl
cp /pfad/zu/fullchain.pem docker/nginx/ssl/fullchain.pem
cp /pfad/zu/privkey.pem docker/nginx/ssl/privkey.pem
```

### .env anpassen

```env
CORS_ALLOWED_ORIGINS=https://mein-server.example.com
API_URL=https://mein-server.example.com
```

### Mit TLS starten

```bash
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d
```

HTTP-Requests werden automatisch auf HTTPS umgeleitet.

---

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

## Troubleshooting

### Backend startet nicht
```bash
docker compose logs backend
```
Haeufige Ursachen: DB-Passwort falsch, Port belegt.

### Frontend zeigt "Netzwerkfehler"
- `API_URL` in `.env` pruefen – muss die externe URL sein (ohne `/api`)
- `CORS_ALLOWED_ORIGINS` muss die Frontend-URL enthalten

### Chat streamt nicht
- Nginx hat SSE-Locations fuer `/api/chat/` und `/api/system/chat/`
- `proxy_buffering off` muss gesetzt sein

### Migrationen pruefen
```bash
docker compose exec postgres psql -U owlsburg_app -d owlsburg_ops -c "SELECT * FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 5;"
```

## Updates deployen

```bash
git pull
docker compose build
docker compose up -d
```

Backend-Migrationen laufen automatisch beim Start.
