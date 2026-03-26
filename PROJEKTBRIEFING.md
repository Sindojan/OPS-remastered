# Owlsburg OPS – Projektbriefing

**Stand:** 25. März 2026
**Projekt:** Agentenbasierte Operations-Plattform für Auto-Sitz-Manufaktur
**Repository:** https://github.com/Sindojan/OPS-remastered

---

## 1. Projektidee

Owlsburg OPS ist eine vollständige ERP/MES-Plattform, die klassische Betriebsführung mit KI-gestützten Agenten verbindet. Die Plattform verwaltet Produktion, Maschinen, Lager, Personal, Kunden und Wissensdatenbank – und stellt jedem Benutzer einen KI-Assistenten (Agent) zur Seite, der über natürliche Sprache auf alle Betriebsdaten zugreifen und Aktionen ausführen kann.

**Kernprinzip:** Alle Geschäftslogik bleibt deterministisch. KI-Agenten greifen ausschließlich über eine kontrollierte Tool-Registry auf die Domänen-Services zu – niemals direkt auf die Datenbank.

---

## 2. Tech Stack

| Schicht | Technologie |
|---------|-------------|
| Frontend | Next.js 16, React 19, shadcn/ui, Tailwind CSS v4, TypeScript |
| Backend | Spring Boot 3.5, Java 21, Maven |
| Datenbank | PostgreSQL mit Row Level Security (Multi-Tenancy) |
| Dateispeicher | MinIO (S3-kompatibel) |
| Auth | JWT (stateless), Spring Security |
| LLM | Anthropic Claude API (Opus/Sonnet/Haiku) |
| Deployment | Docker Compose, Nginx Reverse Proxy |

---

## 3. Erledigte Funktionalität

### 3.1 Plattform-Grundlagen
- **Multi-Tenancy:** Single-Schema mit PostgreSQL Row Level Security – jede Tabelle ist automatisch tenant-isoliert
- **Authentication:** JWT mit Access/Refresh Token, Login Rate-Limiting, Rollen-System (SYSTEM_ADMIN, ADMIN, MANAGER, TEAM_LEAD, WORKER)
- **Modulares Feature-System:** 10 Module (core + 9 togglebar), pro Tenant aktivierbar/deaktivierbar
- **Design System:** "Industrial Precision" – Kontrollraum-Ästhetik, Dark/Light Mode, deutsche Lokalisierung

### 3.2 Domain-Module (vollständig implementiert)

| Modul | Funktionsumfang |
|-------|----------------|
| **Produktion** | Jobs mit Status-Maschine, Stationen, Schichten, Qualitätsprüfungen |
| **Maschinen** | CRUD, Wartungsplanung, Störungserfassung |
| **Lager & Material** | Artikel, Bestandsführung, Lagerbewegungen, Lieferanten |
| **Personal** | Mitarbeiter-Verwaltung, Zeiterfassung (Clock-In/Out), Abwesenheiten, My-Day Dashboard |
| **Kunden** | Kundenstamm, Kontakte, Adressen, Preisgruppen |
| **Teile & Prozesse** | Stücklisten (BOM), Arbeitspläne, Kalkulation (Soll/Ist) |
| **Wissensdatenbank** | Artikel mit Markdown-Editor, Kategorien, Tags, Volltextsuche |
| **Posteingang** | Konversationen, Nachrichten, Tagging |
| **Dokumente** | Upload/Download via MinIO, Vorschau, Metadaten, Verknüpfungen |

### 3.3 KI-Agent-System

Das Herzstück der Plattform – ein hierarchisches Multi-Agent-System:

- **CEO Agent** (Claude Opus): Zentraler Ansprechpartner, delegiert an Lead-Agents
- **8 Lead Agents** (Claude Sonnet): Spezialisiert auf je eine Domäne (Produktion, Maschinen, Lager, Personal, Support etc.)
- **Sub-Agents:** Dynamisch spawnable für komplexe Aufgaben

**Technische Features:**
- SSE-Streaming für Echtzeit-Chat im Browser
- 40+ Tools (13 Domain + 20 Lead + 7 Agent-Infra)
- ReAct-Loop mit Tool-Calling und paralleler Delegation
- Agent Memory (4-Typ-System: Semantic, Episodic, Procedural, Run)
- Inter-Agent Message Bus mit Hierarchie-Enforcement
- Token-Tracking und Kosten-Berechnung pro Nachricht
- Budget-Limits pro Agent (tägliches Token-Budget)
- Per-Tenant Agent-Konfiguration (Model, Temperature, Tools, Prompt)

### 3.4 Frontend Views

- **My-Day Dashboard:** Rollenbasiertes Dashboard mit Clock-In/Out, offene Jobs, KPIs
- **Agent Console:** Sidebar-Chat-Panel mit SSE-Streaming, Session-Management, Tool-Call-Visualisierung
- **Agent Hierarchie-View:** Live-Visualisierung aller Agents mit SVG-Verbindungslinien, Activity-Status, Monitor-Panel
- **Domain-Views:** Vollständige CRUD-Oberflächen für alle 9 Module
- **Settings:** Benutzer, Firma, Agents, Budget, Benachrichtigungen, Module, LLM-Konfiguration
- **Reports:** KPI-Dashboard mit Charts und CSV-Export
- **Login:** Auth-Flow mit Route Guards, rollenbasierte Redirects

### 3.5 Systemverwaltung (SYSTEM_ADMIN)

Separate Admin-Oberfläche für plattformweite Verwaltung:

- **Firmenübersicht:** Alle Tenants mit Statistiken, Token-Verbrauch, Modulen
- **Firmendetail:** 7 Tabs (Übersicht, Statistiken, Admins, Module, Token, Agenten, LLM)
- **System-Agenten:** Live-Hierarchie-View mit eigenem System-CEO + 8 System-Leads
- **System-Chat:** Agent-Panel im Sidebar (gleiche Komponente wie Tenant-Chat)
- **API-Credentials:** Verwaltung externer API-Schlüssel

### 3.6 Externe Integrationen

- **Odoo-Integration:** 8 Odoo-Tools für den Agent (Partners, Products, Sale Orders, Purchase Orders, Manufacturing, Stock, Employees), Proxy-Service, Konfigurations-UI

### 3.7 Deployment & DevOps

- Docker Compose Setup (Produktion + Development)
- Nginx Reverse Proxy mit SSE-Support
- TLS/HTTPS Konfiguration
- 28 Flyway-Migrationen (V1–V28)
- ~600 Java-Dateien, ~50 React-Komponenten

### 3.8 Security & Stabilisierung

- Vollständiger Security-Audit mit 6 spezialisierten Agents
- `@PreAuthorize` auf allen state-changing Endpoints (27 Annotations nachgerüstet)
- Tenant-Isolation: Defense-in-Depth mit `findByIdAndTenantId()` zusätzlich zu RLS
- Document Upload: Filename-Sanitization gegen Path-Traversal
- Chat History Limit gegen Token-Overflow
- N+1 Query Fixes in Activity-Services
- Open-Redirect-Fix im Login
- Select-Component Crashes gefixt (Radix UI)

---

## 4. Geplante Features & nächste Schritte

### 4.1 Kurzfristig (nächste Sprints)

| Feature | Beschreibung | Priorität |
|---------|-------------|-----------|
| **V29 Migration** | RLS auf Join-Tables (knowledge_article_tags), QualityDefect Entity-Fix, Incident Severity | Hoch |
| **Rate-Limiting Fix** | X-Forwarded-For statt Proxy-IP für korrektes Rate-Limiting hinter Nginx | Hoch |
| **Integration Tests** | Automatisierte Tests für Authorization (403 bei falscher Rolle) | Hoch |
| **Agent Monitoring Dashboard** | Echtzeit-Metriken, Token-Verbrauch-Trends, Fehlerrate pro Agent | Mittel |

### 4.2 Mittelfristig (Feature-Roadmap)

| Feature | Beschreibung |
|---------|-------------|
| **Benachrichtigungssystem** | Push-Notifications bei Agent-Events, Job-Statusänderungen, Maschinenausfällen |
| **Reporting 2.0** | Erweiterte Reports mit Drill-Down, Zeitraumvergleiche, PDF-Export |
| **Audit-Log** | Lückenlose Protokollierung aller Datenänderungen (wer, wann, was) |
| **Workflow-Engine** | Konfigurierbare Genehmigungsprozesse (z.B. Abwesenheitsanträge, Bestellfreigaben) |
| **Mobile Responsive** | Optimierung für Tablet/Mobile (My-Day, Clock-In/Out, Agent-Chat) |
| **Bulk-Import** | CSV/Excel-Import für Stammdaten (Mitarbeiter, Artikel, Kunden) |
| **Agent Skill Training** | Fine-Tuning der Agent-Prompts basierend auf tatsächlicher Nutzung |

### 4.3 Langfristig (Vision)

| Feature | Beschreibung |
|---------|-------------|
| **Predictive Maintenance** | ML-basierte Vorhersage von Maschinenausfällen |
| **Produktionsplanung KI** | Automatische Schichtplanung und Job-Zuweisung durch Agents |
| **Lieferanten-Portal** | Self-Service-Portal für Lieferanten (Bestellungen, Lieferscheine) |
| **Kunden-Portal** | Auftragsstatus-Tracking für Endkunden |
| **Multi-Language** | Englisch, weitere Sprachen |
| **On-Premise LLM** | Lokales LLM als Alternative zu Cloud-API (Datenschutz) |

---

## 5. Kennzahlen

| Metrik | Wert |
|--------|------|
| Backend-Dateien | ~600 Java |
| Frontend-Komponenten | ~50 React |
| API-Endpoints | 60+ |
| DB-Migrationen | 28 (V1–V28) |
| JPA Repositories | ~70 |
| Agent-Tools | 40+ |
| Domain-Module | 10 (1 Core + 9 togglebar) |
| Rollen | 6 (SYSTEM_ADMIN, ADMIN, MANAGER, TEAM_LEAD, WORKER, AGENT_SYSTEM) |

---

## 6. Architektur-Highlights

- **Multi-Tenancy ohne Kompromisse:** PostgreSQL Row Level Security auf jeder Tabelle, automatisch enforced
- **Deterministisches Domain-Modell:** KI greift nur über definierte Tool-Schnittstellen zu, keine "halluzinierten" Datenbank-Operationen
- **Hierarchisches Agent-System:** CEO delegiert an Leads, Leads nutzen Domain-Tools, Sub-Agents für komplexe Aufgaben
- **Echtzeit-Transparenz:** Live-Activity-Status aller Agents via SSE, sichtbar in Hierarchie-View
- **Modulares Design:** Features pro Tenant aktivierbar, deaktivierte Module blockieren API + UI + Agent-Tools gleichzeitig
