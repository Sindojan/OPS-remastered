# Owlsburg OPS – Architektur-Dokumentation (IST-Stand)

> **Stand:** 2026-02-25
> **Zweck:** Single Source of Truth für Architekten, Projektleiter und LLM-Agenten.
> **Hinweis:** Diese Datei beschreibt ausschließlich den im Code implementierten Zustand. Nichts wurde erfunden oder angenommen.

---

## Inhaltsverzeichnis

1. [Projektkontext & Ziel](#1-projektkontext--ziel)
2. [Tech Stack](#2-tech-stack)
3. [Projektstruktur & Namensgebung](#3-projektstruktur--namensgebung)
4. [Frontend-Architektur & Navigation](#4-frontend-architektur--navigation)
5. [Backend-Architektur & Domänen](#5-backend-architektur--domänen)
6. [Agent-/LLM-Integration](#6-agent-llm-integration)
7. [Datenbank & Row-Level Security](#7-datenbank--row-level-security)
8. [Authentifizierung & Rollenmodell](#8-authentifizierung--rollenmodell)
9. [Settings & Konfiguration](#9-settings--konfiguration)
10. [Infrastruktur & Deployment](#10-infrastruktur--deployment)
11. [Bekannte technische Schulden / TODOs](#11-bekannte-technische-schulden--todos)
12. [Abweichungen zum Masterplan](#12-abweichungen-zum-masterplan)
13. [Offene Fragen / Unsicherheiten](#13-offene-fragen--unsicherheiten)

---

## 1. Projektkontext & Ziel

**Owlsburg OPS** ist eine agentenbasierte Operations-Plattform für eine Auto-Sitz-Manufaktur. Die Plattform verbindet klassische ERP-Domänen (Produktion, Lager, Mitarbeiter, Kunden, etc.) mit einer KI-Agent-Infrastruktur, die über eine Tool Registry auf die Domänen-Services zugreift.

**Kernprinzipien:**
- Deterministische Geschäftslogik – kein LLM in der Domänenlogik
- Modularer Monolith (Package-by-Domain)
- Single-Schema PostgreSQL mit Row-Level Security ab Tag 1
- Agents greifen ausschließlich über die Tool Registry auf Services zu
- Token-Verbrauch und Kosten werden pro AgentRun erfasst

---

## 2. Tech Stack

### Frontend (aus `package.json`)

| Bibliothek | Version | Zweck |
|---|---|---|
| Next.js | 16.1.6 | Framework (App Router, `output: "standalone"`) |
| React / React DOM | 19.2.3 | UI-Library |
| radix-ui | 1.4.3 | Headless UI-Primitives (Basis für shadcn/ui) |
| @radix-ui/react-progress | 1.1.8 | Fortschrittsbalken |
| Tailwind CSS | v4 | Styling (oklch Farbraum, CSS Custom Properties) |
| lucide-react | 0.575.0 | Icons |
| recharts | 3.7.0 | Charts (LineChart, Sparklines) |
| next-themes | 0.4.6 | Dark/Light/System Theme |
| class-variance-authority | 0.7.1 | Varianten-basierte Klassenkomposition |
| clsx + tailwind-merge | 2.1.1 / 3.5.0 | `cn()` Utility |
| sonner | 2.0.7 | Toast-Benachrichtigungen |
| react-hook-form | 7.71.2 | Formular-State |
| @hookform/resolvers + zod | 5.2.2 / 4.3.6 | Schema-Validierung |
| @uiw/react-md-editor | 4.0.11 | Markdown-Editor (Knowledge) |
| react-markdown + remark-gfm | 10.1.0 / 4.0.1 | Markdown-Rendering |
| react-pdf | 10.4.0 | PDF-Vorschau |
| rehype-highlight | 7.0.2 | Code-Syntax-Highlighting |

**Kein** `@tanstack/react-query` – eigene Custom Hooks (`useApi`, `usePagedApi`, `useMutation`).

**Dev-Dependencies:** TypeScript 5, ESLint 9, `tw-animate-css`, `shadcn` 3.8.5.

### Backend (aus `pom.xml`)

| Dependency | Version |
|---|---|
| Spring Boot (Parent) | 3.5.0 |
| Java | 21 |
| spring-boot-starter-web | managed |
| spring-boot-starter-data-jpa | managed |
| spring-boot-starter-security | managed |
| spring-boot-starter-validation | managed |
| spring-boot-starter-actuator | managed |
| micrometer-registry-prometheus | managed (runtime) |
| flyway-core + flyway-database-postgresql | managed |
| postgresql JDBC | managed (runtime) |
| minio | 8.5.7 |
| bucket4j (jdk17-core) | 8.14.0 |
| jjwt-api / jjwt-impl / jjwt-jackson | 0.12.6 |
| lombok | managed (optional, nur für Entity-Annotations) |

### Datenbank & Infrastruktur

| Komponente | Technologie |
|---|---|
| Datenbank | PostgreSQL 16 (Alpine, Docker) |
| Multi-Tenancy | Single-Schema mit Row-Level Security |
| Dateispeicher | MinIO (S3-kompatibel) |
| Reverse Proxy | nginx (Alpine, Docker) |
| Monitoring | Prometheus + Grafana + Loki + Promtail |
| Container | Docker Compose (Prod + Dev + Monitoring) |

---

## 3. Projektstruktur & Namensgebung

### Verzeichnisstruktur

```
sindojan_ops_remastered/         # Lokaler Ordnername (GitHub: OPS-remastered)
├── CLAUDE.md                    # Projektdokumentation für Agents
├── DEPLOYMENT.md                # Deployment-Anleitung
├── docker-compose.yml           # Produktion (5 Services)
├── docker-compose.dev.yml       # Dev (nur Postgres + MinIO)
├── docker-compose.monitoring.yml # Monitoring-Stack
├── docker/                      # Dockerfiles + nginx config
│   ├── backend.Dockerfile
│   ├── frontend.Dockerfile
│   └── nginx/nginx.conf
├── scripts/                     # dev.sh, prod.sh, backup.sh, restore.sh
├── frontend/                    # Next.js 16 App
│   ├── app/                     # App Router (Route Groups)
│   ├── components/              # UI + Layout + Shared + Domain
│   ├── contexts/                # AuthContext
│   ├── hooks/                   # Custom Hooks (API, Primary Agent)
│   ├── lib/                     # Utilities (api-client, i18n, format)
│   └── types/                   # TypeScript Typen
└── backend/                     # Spring Boot 3.5.0
    ├── pom.xml
    └── src/main/java/com/owlsburg/ops/
        ├── common/              # BaseEntity, TenantContext, Exceptions
        ├── config/              # Security, JPA, Flyway, CORS, MinIO, RLS
        ├── auth/                # JWT, Login, User-CRUD, Rollen
        ├── tenant/              # Tenant-Management, System Admin
        ├── customers/           # Kunden, Kontakte, Adressen
        ├── production/          # Jobs, Stationen, Schichten, QA
        ├── machines/            # Maschinen, Wartung, Störungen
        ├── people/              # Mitarbeiter, Zeiterfassung, Abwesenheiten
        ├── inventory/           # Lager, Artikel, Bestand, Lieferanten
        ├── bom/                 # Stücklisten, Arbeitspläne, Kalkulation
        ├── documents/           # Dokumente (Meta in DB, Binär in MinIO)
        ├── knowledge/           # Wissensdatenbank
        ├── inbox/               # Conversations, Nachrichten
        ├── events/              # Domain Events, Scheduled Triggers
        └── agentinfra/          # Agent Templates, Instances, Runs, Steps
            ├── llm/             # LLM Provider, Config, Anthropic
            ├── tools/impl/      # 13 Domain Tools
            ├── execution/       # ReAct-Loop, Orchestrator
            └── events/          # Event-Routing, Scheduler
```

### Namenskonsistenz

Das Rebranding von **Sindoflow** zu **Owlsburg** ist in allen Live-Code-Dateien vollständig abgeschlossen:
- Java: `com.owlsburg.ops.*`
- Frontend: `owlsburg_token`, `owlsburg_user`, `owlsburg_refresh_token` (localStorage)
- Docker: Container-Namen `owlsburg-*`
- Prometheus: Job `owlsburg-backend`

Verbleibende fossile Referenzen nur in:
- `CLAUDE.md` (historische Commit-Beschreibung „Rebranding Sindoflow → Owlsburg")
- `folder_structure.txt` (auto-generierte Datei, nicht aktiv genutzt)
- Docker-Container-Name für Postgres: `sindoflow-postgres` (in DB-Drop-Anweisungen in CLAUDE.md referenziert, Compose selbst nutzt `owlsburg-postgres`)

---

## 4. Frontend-Architektur & Navigation

### 4.1 Next.js Struktur

- **App Router** mit Route Groups
- **Root Layout** (`app/layout.tsx`): `ThemeProvider` → `AuthProvider` → `Toaster`
- Fonts: DM Sans (Body) + JetBrains Mono (Mono/Daten)
- `lang="de"`, `suppressHydrationWarning`
- Path-Alias: `@/*` → Frontend-Root

### 4.2 Route Groups

#### `app/login/` – Öffentlich
Eigenes Minimal-Layout ohne AppShell. Zwei-Panel-Design (Branding links, Formular rechts). Nach Login rollenbasierte Weiterleitung. Unterstützt `?redirect=` Query-Parameter.

#### `app/(app)/` – Auth-geschützt (Hauptanwendung)
- **Layout:** Client Component, prüft `useAuth()` State
- Nicht authentifiziert → `/login?redirect=<aktueller Pfad>`
- `SYSTEM_ADMIN` → `/system/companies`
- `WORKER`/`TEAM_LEAD` können `/settings` nicht erreichen
- Rendert `AppShell` (Drei-Panel-Layout)

#### `app/(system)/` – System Admin
- **Layout:** Nur `SYSTEM_ADMIN` erlaubt, sonst Redirect zu `/login`
- Rendert `SystemShell` (eigenes Shell-Design mit Shield-Icon)

### 4.3 Routen & Seiten

| Route | Beschreibung | Besonderheiten |
|---|---|---|
| `/` | Redirect zu `/agents` | – |
| `/my-day` | Rollen-Dashboard | Clock-In/Out Timer, aktive Jobs, KPI-Cards (kritischer Bestand, Maschinen-Störungen, Abwesenheiten, offene Inbox), Primary Agent Button |
| `/agents` | Agent Console | **Platzhalter** – zeigt nur Überschrift „Agent Console" |
| `/agents/hierarchy` | Agent-Hierarchie | Eigene Seite (Inhalt nicht detailliert analysiert) |
| `/production` | Produktionsübersicht | Job-Liste mit Status-Filtern |
| `/production/jobs/[id]` | Job-Detail | Detailansicht einzelner Job |
| `/production/planner` | Produktionsplaner | Planungsansicht |
| `/production/stations/[id]` | Stations-Detail | Stationsdetails |
| `/machines` | Maschinenübersicht | Maschinen-Liste |
| `/machines/[id]` | Maschinen-Detail | Detail mit Wartungseinträgen und Störungen |
| `/inventory` | Lagerübersicht | Artikel, Bestand, Lieferanten |
| `/inventory/articles/[id]` | Artikel-Detail | Einzelartikel |
| `/inventory/suppliers/[id]` | Lieferanten-Detail | Einzellieferant |
| `/customers` | Kundenliste | Liste mit KPIs |
| `/customers/[id]` | Kunden-Detail | 5 Tabs: Übersicht, Ansprechpartner, Adressen, Preisgruppen, Historie |
| `/parts-and-processes` | Teile & Prozesse | Übersicht |
| `/parts-and-processes/parts/[id]` | Teil-Detail | Einzelteil |
| `/parts-and-processes/bom/[id]` | BOM-Detail | Baumstruktur |
| `/parts-and-processes/processes/[id]` | Prozessplan-Detail | Arbeitsplan |
| `/employees` | Mitarbeiterliste | Liste |
| `/people/[id]` | Mitarbeiter-Detail | **Hinweis:** Route ist `/people/[id]`, nicht `/employees/[id]` |
| `/inbox` | Posteingang | Split-Layout, Konversationsliste |
| `/inbox/[id]` | Konversation | Chat mit Nachrichten |
| `/reports` | Berichte | KPI Dashboard mit recharts, CSV Export |
| `/knowledge` | Wissensdatenbank | Artikel-Liste, Suche |
| `/knowledge/articles/[id]` | Artikel-Detail | Markdown gerendert |
| `/knowledge/articles/[id]/edit` | Artikel bearbeiten | MDEditor |
| `/knowledge/articles/new` | Neuer Artikel | Erstellen |
| `/knowledge/documents` | Dokumente | Dokumentenliste |
| `/knowledge/documents/[id]` | Dokument-Detail | PDF-Vorschau |
| `/settings` | Einstellungen | 8 Tabs (rollenabhängig sichtbar, siehe Abschnitt 9) |
| `/system/companies` | Firmen-Verwaltung | SYSTEM_ADMIN: Company CRUD |
| `/system/companies/[id]` | Firmen-Detail | Tabs: Übersicht, Benutzer, Statistiken |

### 4.4 Wichtige Shared Components

#### AppShell (`components/layout/app-shell.tsx`)
Drei-Panel-Layout:
- **Sidebar** (links, collapsible auf 60px Icon-only mit Tooltips)
- **Topbar** (oben: Breadcrumb, Agent-Button, Theme-Toggle, User-Dropdown)
- **Content** (Mitte, Dot-Grid Hintergrund)
- **AgentPanel** (rechts, 380px, togglebar)

#### Sidebar (`components/layout/sidebar.tsx`)
Navigations-Sektionen:
- **Betrieb:** Mein Tag, Konsole, Produktion, Maschinen, Lager, Teile & Prozesse
- **Kommunikation:** Posteingang, Berichte
- **Organisation:** Kunden, Mitarbeiter, Wissensdatenbank, Einstellungen

Rollenbasiert: `WORKER`/`TEAM_LEAD` sehen „Einstellungen" nicht. `SYSTEM_ADMIN` bekommt Shield-Link zu `/system/companies`.

#### DataTable (`components/shared/data-table.tsx`)
Generische Tabelle `DataTable<T extends { id: string }>`:
- Suche (nach konfigurierbarem `searchKey`)
- Spalten-Sichtbarkeit (Popover)
- Sortierung (Klick auf Header, Asc/Desc/Off Zyklus)
- Client-seitige Paginierung (konfigurierbare `pageSize`)
- Multi-Select mit Bulk-Actions
- Pro-Zeile: Primary Action + Dropdown Row Actions + optionale Agent Action (Bot-Icon)
- Skeleton Loading State
- Anpassbarer Empty State

#### KpiCard (`components/shared/kpi-card.tsx`)
- Props: `label`, `value`, optional `unit`, optional `trend` (up/down/neutral mit Icon + Wert, farbcodiert), optional `sparkline` (Mini-recharts LineChart)
- Hover-Schatten, Gradient-Akzentlinie oben

#### PageHeader (`components/shared/page-header.tsx`)
- Optionale Breadcrumb-Navigation (Chevron-getrennt)
- `h1` Titel, optionale Beschreibung, optionaler Actions-Slot rechts

#### DomainStatusBadge (`components/shared/domain-status-badge.tsx`)
6 Varianten: success/warning/error/info/neutral/primary. Monospace-Text, farbiger Punkt (optional mit Puls-Animation). Helper-Funktionen für alle Domain-Status-Mappings (Jobs, Maschinen, Konversationen, Prioritäten, Severity, etc.).

#### StatusBadge (`components/shared/status-badge.tsx`)
Agent-spezifisch: idle/busy/degraded/quarantine/success/error/warning.

#### ConfirmationDialog (`components/shared/confirmation-dialog.tsx`)
Generisch: `title`, `description`, `onConfirm`, `onCancel`, `variant` (default/destructive).

#### SkeletonVariants (`components/shared/skeleton-variants.tsx`)
Vorgefertigte Skeleton-Layouts für verschiedene Seitentypen.

### 4.5 Auth-Flow im Frontend

1. **App-Start:** `AuthProvider` mountet, liest localStorage (`owlsburg_token`, `owlsburg_user`). Bei vorhandenem Token: JWT-Expiry prüfen. Wenn gültig: User-State setzen + `/api/users/me` aufrufen zur DB-Validierung. Bei Fehler: Storage leeren.
2. **Route-Guard:** `(app)/layout.tsx` (Client Component) prüft Auth-State. Loading → Spinner. Nicht authentifiziert → `/login?redirect=`. `SYSTEM_ADMIN` → `/system/companies`. Rollenblockierte Pfade → Default-Route.
3. **Login-Seite:** Bereits authentifizierte User werden sofort weitergeleitet (rollenbasiert). Formular sendet an `POST /api/auth/login`. Erfolg: Tokens + User in localStorage, State-Update, Redirect.
4. **API 401:** `apiClient` leert Auth-State und leitet hart zu `/login` weiter bei jeder 401-Response.

**Rollenbasierte Default-Routen:**

| Rolle | Default-Route | Blockierte Pfade |
|---|---|---|
| WORKER | `/my-day` | `/settings` |
| TEAM_LEAD | `/production` | `/settings` |
| MANAGER | `/production` | keine |
| ADMIN | `/production` | keine |
| SYSTEM_ADMIN | `/system/companies` | alle `(app)` Routen |

### 4.6 Agent-Panel (rechte Sidebar)

**`usePrimaryAgent` Hook** (`hooks/use-primary-agent.ts`):
- Ruft `useApi<MeResponse>("/api/users/me")` auf
- Gibt `{ agent: PrimaryAgentInfo, loading, error }` zurück
- `PrimaryAgentInfo`: `{ id, name, role }`

**Topbar Agent-Button** (`components/layout/topbar.tsx`):
- Wird nur gerendert wenn `agent || agentLoading` truthy ist
- Zeigt Agent-Name, toggelt AgentPanel

**AgentPanel** (`components/layout/agent-panel.tsx`):
- 380px rechte Sidebar, nur sichtbar wenn `open === true`
- Header: Bot-Icon + Agent-Name, Gradient-Akzentlinie, Schließen-Button
- Body: **Aktuell nur Empty State** (Sparkles-Icon, „Agent Panel / Context-aware AI assistance")
- **Agent-Chat ist noch nicht implementiert**

### 4.7 API-Client & Datenfetching

**ApiClient** (`lib/api-client.ts`):
- Singleton, Base URL aus `NEXT_PUBLIC_API_URL` (Default: `http://localhost:8080`)
- JWT aus localStorage als `Authorization: Bearer` Header
- 401 → Auth-Cleanup + Redirect zu `/login`
- Methoden: `get`, `put`, `post`, `patch`, `delete`, `upload` (multipart)

**Custom Hooks** (`hooks/api/`):
- `useApi<T>(path)` – Einzelressource, `{ data, loading, error, refetch }`
- `usePagedApi<T>(path)` – Paginierte Ressource (unwrapped Spring `Page<T>`), gibt `T[]` zurück
- `useMutation<TReq, TRes>()` – Mutations-Hook, `{ mutate, loading, error }`

**Domain-spezifische Hooks:** `use-jobs.ts`, `use-machines.ts`, `use-inventory.ts`, `use-people.ts`, `use-inbox.ts`, `use-customers.ts`, `use-bom.ts`, `use-knowledge.ts`, `use-documents.ts`, `use-settings.ts`

### 4.8 Internationalisierung

**Zentrale Übersetzungen** (`lib/i18n.ts`):
- `statusLabels`: 70+ Enum-Werte → deutsche Anzeigestrings
- `roleLabels`: Rollen → deutsch
- `planLabels`: Firmen-Pläne → deutsch
- `t(key)` Helper: Sucht in allen Maps, Fallback auf Key

**Gesamte UI ist auf Deutsch.** Neue Views werden direkt auf Deutsch erstellt.

---

## 5. Backend-Architektur & Domänen

### 5.1 Package-Struktur

Package-by-Domain unter `com.owlsburg.ops`:

```
OpsApplication.java
├── common/          # BaseEntity, TenantContext, EncryptionService, GlobalExceptionHandler
├── config/          # SecurityConfig, FlywayConfig, RlsTenantInterceptor, AsyncConfig, MinioConfig, JpaConfig
├── auth/            # JWT, Login, User-CRUD, Rollen, Notifications
├── tenant/          # Tenant-Management, System Admin, TenantConfig
├── customers/       # Kunden + Kontakte + Adressen + Preisgruppen
├── production/      # Jobs + Stationen + Schichten + QA
├── machines/        # Maschinen + Wartung + Störungen
├── people/          # Mitarbeiter + Zeiterfassung + Abwesenheiten
├── inventory/       # Artikel + Bestand + Lager + Lieferanten
├── bom/             # Stücklisten + Arbeitspläne + Kalkulation
├── documents/       # Dokumente (DB + MinIO)
├── knowledge/       # Wissensdatenbank (Artikel, Kategorien, Tags)
├── inbox/           # Conversations + Messages + Tags + Links
├── events/          # Domain Events + Scheduled Triggers
└── agentinfra/      # Agents, LLM, Tools, Execution Engine
```

Jedes Domain-Package folgt der Struktur: `Entity` → `Repository` → `Service` → `Controller` → `dto/`. Services werden ausschließlich über Controller angesprochen, nie direkt von anderen Controllern.

### 5.2 Domänen im Detail

#### Production

**Entities:**
- `JobEntity` – job_number, customer_id, title, status, priority, quantity, deadline, assigned_station_id, shift_id, started_at, completed_at
- `JobStatusHistoryEntity` – job_id, from_status, to_status, changed_by, reason
- `StationEntity` – name, capacity_per_shift, status
- `ShiftEntity` – name, start_time, end_time, days_of_week, capacity_hours
- `QualityCheckEntity` – job_id, check_type, result (PASS/FAIL/PARTIAL), defect_count
- `QualityDefectEntity` – quality_check_id, defect_type, severity

**Status-Maschine (Jobs):** DRAFT → RELEASED → IN_PRODUCTION → ON_HOLD → COMPLETED / CANCELLED

**Endpoints:**
- `JobController` (`/api/jobs`): CRUD, Status-Transition (`PATCH /{id}/status`), Zuweisung (`PATCH /{id}/assign`)
- `StationController` (`/api/stations`): CRUD
- `ShiftController` (`/api/shifts`): CRUD
- `QualityCheckController` (`/api/quality-checks`): CRUD pro Job

#### Machines

**Entities:**
- `MachineEntity` – machine_number, type, station_id, status, capacity_per_hour, manufacturer, model
- `MaintenanceIntervalEntity` – machine_id, type (TIME_BASED/HOURS_BASED), interval_days/hours, next_due_at
- `MaintenanceRecordEntity` – machine_id, interval_id, performed_by, duration_minutes, status (PLANNED/IN_PROGRESS/DONE/SKIPPED)
- `MachineIncidentEntity` – machine_id, reported_by, type, severity, resolution_notes

**Maschinen-Status:** AVAILABLE / IN_USE / MAINTENANCE / BLOCKED / DECOMMISSIONED

**Endpoints:**
- `MachineController` (`/api/machines`): CRUD
- `MaintenanceController` (`/api/maintenance`): Records erstellen/auflisten, abschließen
- `MachineIncidentController` (`/api/machines/{id}/incidents`): Melden, lösen

#### People

**Entities:**
- `EmployeeEntity` – employee_number, user_id, first_name, last_name, role, status, hire_date, station_id
- `EmployeeQualificationEntity` – qualification, certified_at, expires_at
- `TimeEntryEntity` – employee_id, type (CLOCK_IN/CLOCK_OUT/JOB_START/JOB_END), job_id, timestamp
- `AbsenceEntity` – employee_id, type (VACATION/SICK/OTHER), from_date, to_date, status (PENDING/APPROVED/REJECTED)

**Endpoints:**
- `EmployeeController` (`/api/employees`): CRUD, Clock-In/Out
- `TimeEntryController` (`/api/time-entries`): Einträge, MyDay-View
- `AbsenceController` (`/api/absences`): Anfragen, Genehmigen/Ablehnen

#### Inventory

**Entities:**
- `ArticleEntity` – article_number, name, category_id, unit_id, min_stock, reorder_point, status
- `ArticleCategoryEntity` – name, parent_id (hierarchisch)
- `WarehouseEntity` / `WarehouseLocationEntity` – name, location, aisle/rack/shelf
- `StockEntity` – article_id, warehouse_location_id, quantity, reserved_quantity
- `StockMovementEntity` – article_id, from/to location, quantity, type (INBOUND/OUTBOUND/TRANSFER/CORRECTION)
- `SupplierEntity` – name, email, status
- `SupplierArticleEntity` / `SupplierPriceListEntity` – Lieferanten-Artikel-Verknüpfung mit Preisen

**Endpoints:**
- `ArticleController` (`/api/articles`): CRUD
- `StockController` (`/api/stock`): Bestand, Bewegungen, kritische Artikel
- `SupplierController` (`/api/suppliers`): CRUD

#### Customers

**Entities:**
- `CustomerEntity` – company_name, customer_number, short_name, tax_id, status
- `CustomerContactEntity` – first_name, last_name, email, phone, position, is_primary
- `CustomerAddressEntity` – type (BILLING/SHIPPING/BOTH), street, zip, city, country
- `CustomerPriceGroupEntity` – name, discount_percent, valid_from, valid_until

**Endpoints:**
- `CustomerController` (`/api/customers`): CRUD + Sub-Ressourcen (Kontakte, Adressen, Preisgruppen)

#### BOM (Stücklisten & Kalkulation)

**Entities:**
- `PartEntity` – part_number, name, type (PRODUCT/COMPONENT/RAW_MATERIAL), unit_id, status
- `BomVersionEntity` – part_id, version_number, status (DRAFT/ACTIVE/ARCHIVED)
- `BomItemEntity` – bom_version_id, component_part_id, quantity, position
- `ProcessPlanEntity` – part_id, version_number, name, status
- `ProcessStepEntity` – process_plan_id, step_number, station_id, machine_id, setup_time_minutes, processing_time_minutes
- `CostRateEntity` – type, rate_per_hour, currency
- `CalculationEntity` – part_id, bom_version_id, process_plan_id, material_cost, labor_cost, total_cost
- `JobCalculationEntity` – job_id, calculation_id, actual vs. planned Kosten, variance_percent

**Endpoints:**
- `PartController` (`/api/parts`): CRUD
- `BomController` (`/api/bom`): BOM-Versionen + Items
- `ProcessPlanController` (`/api/process-plans`): Arbeitspläne + Schritte
- `CalculationController` (`/api/calculations`): Kalkulationen, Historie

#### Inbox

**Entities:**
- `ConversationEntity` – subject, customer_id, status (OPEN/IN_PROGRESS/WAITING/RESOLVED/ARCHIVED), priority, sla_due_at, assigned_to, source (EMAIL/MANUAL/AGENT)
- `ConversationMessageEntity` – conversation_id, content, sender_type (USER/AGENT/CUSTOMER)
- `ConversationTagEntity` – conversation_id, tag
- `ConversationLinkEntity` – conversation_id, linked_type, linked_id

**Endpoints:**
- `ConversationController` (`/api/conversations`): CRUD, Nachrichten hinzufügen, Status ändern, Zuweisen, Taggen

#### Knowledge

**Entities:**
- `KnowledgeArticleEntity` – title, slug (unique per tenant), content (TEXT), excerpt, status (DRAFT/PUBLISHED/ARCHIVED), category_id, author_id; ManyToMany tags
- `KnowledgeCategoryEntity` – name, color
- `KnowledgeTagEntity` – name (unique per tenant)

**Endpoints:**
- `KnowledgeArticleController` (`/api/knowledge/articles`): CRUD, Publish, Archive, Paginiert + Filter
- `KnowledgeCategoryController` (`/api/knowledge/categories`): CRUD
- `KnowledgeTagController` (`/api/knowledge/tags`): CRUD
- `KnowledgeSearchController` (`/api/knowledge/search`): Volltextsuche

#### Documents

**Entities:**
- `DocumentEntity` – title, file_key (MinIO), file_name, mime_type, file_size_bytes, version, status, category_id, excerpt
- `DocumentLinkEntity` – document_id, linked_type, linked_id

**Services:** `DocumentService` (Upload, CRUD, Metadaten), `DocumentStorageService` (MinIO-Wrapper: Upload, Download, Delete, Presigned URLs)

**Endpoints:**
- `DocumentController` (`/api/documents`): Multipart Upload, paginierte Liste, Vorschau (15-min Presigned URL), Download (302 Redirect), Metadaten-Update, Löschen, Verknüpfungen

#### Events

**Entities:**
- `DomainEventEntity` – event_type, source_type, source_id, payload (JSONB), processed
- `ScheduledTriggerEntity` – instance_id, cron_expression, last_run_at, next_run_at, active

**Endpoints:**
- `DomainEventController` (`/api/events`): Erstellen, Auflisten
- `ScheduledTriggerController` (`/api/scheduled-triggers`): CRUD

### 5.3 Vollständige REST-API Übersicht

| Bereich | Basis-Pfad | Auth-Anforderung |
|---|---|---|
| Auth | `/api/auth/**` | public |
| Health | `/actuator/health` | public |
| Users | `/api/users` | ADMIN/MANAGER |
| User Me | `/api/users/me` | authenticated |
| Tenants | `/api/admin/tenants` | ADMIN |
| Tenant Config | `/api/tenant` | authenticated |
| System Companies | `/api/system/companies` | SYSTEM_ADMIN |
| Customers | `/api/customers` | authenticated |
| Jobs | `/api/jobs` | authenticated |
| Stations | `/api/stations` | authenticated |
| Shifts | `/api/shifts` | authenticated |
| Quality Checks | `/api/quality-checks` | authenticated |
| Machines | `/api/machines` | authenticated |
| Maintenance | `/api/maintenance` | authenticated |
| Employees | `/api/employees` | authenticated |
| Time Entries | `/api/time-entries` | authenticated |
| Absences | `/api/absences` | authenticated |
| Articles | `/api/articles` | authenticated |
| Stock | `/api/stock` | authenticated |
| Suppliers | `/api/suppliers` | authenticated |
| Parts | `/api/parts` | authenticated |
| BOM | `/api/bom` | authenticated |
| Process Plans | `/api/process-plans` | authenticated |
| Calculations | `/api/calculations` | authenticated |
| Documents | `/api/documents` | authenticated |
| Knowledge Articles | `/api/knowledge/articles` | authenticated |
| Knowledge Categories | `/api/knowledge/categories` | authenticated |
| Knowledge Tags | `/api/knowledge/tags` | authenticated |
| Knowledge Search | `/api/knowledge/search` | authenticated |
| Conversations | `/api/conversations` | authenticated |
| Agent Templates | `/api/agent-templates` | authenticated |
| Agent Instances | `/api/agent-instances` | authenticated |
| Agent Runs | `/api/agent-runs` | authenticated |
| Budget Overview | `/api/budget/overview` | ADMIN/MANAGER |
| LLM Config | `/api/settings/llm` | authenticated |
| Role-Agent Defaults | `/api/settings/role-agent-defaults` | ADMIN/MANAGER |
| Events | `/api/events` | authenticated |
| Triggers | `/api/scheduled-triggers` | authenticated |

### 5.4 Service-Layer

- Services sind sauber pro Domäne getrennt
- Controller → Service → Repository Kette wird strikt eingehalten
- Cross-Domain-Zugriffe laufen über Service-Injections (z.B. `JobCalculationService` nutzt `CalculationRepository`)
- `@Transactional` wird auf Service-Methoden verwendet (besonders wichtig für Lazy-Loading von ManyToMany-Beziehungen)
- Keine Service-zu-Service-Aufrufe über Controller-Layer

---

## 6. Agent-/LLM-Integration

### 6.1 LLM-Client

**Klassen:**
- `LlmProvider` (Interface): `chat(LlmRequest, apiKey): LlmResponse`, `listModels(apiKey): List<String>`
- `AnthropicLlmProvider` (Implementierung): REST-Aufrufe gegen `https://api.anthropic.com/v1/messages`, API-Version `2023-06-01`
- `LlmProviderRegistry`: Spring-injizierte `List<LlmProvider>`, Lookup nach Provider-Name

**Modell-Konfiguration:**
- API Key pro Tenant, AES-256-GCM verschlüsselt in `tenant_llm_config` Tabelle
- Default-Modell: `claude-sonnet-4-20250514`
- Override pro Agent-Instance möglich (JSONB `config.model`)
- Modell-Auflösung: Instance Config > Tenant LLM Config Default > Hardcoded Fallback
- Modell-Liste: GET `https://api.anthropic.com/v1/models` mit 1h In-Memory-Cache

**API-Endpoints für LLM-Konfiguration:**
- `GET /api/settings/llm` – Config-Status
- `PUT /api/settings/llm` – Config speichern
- `GET /api/settings/llm/models` – Modelle auflisten (mit gespeichertem Key)
- `POST /api/settings/llm/models` – Modelle auflisten (mit übergebenem Key, für Validierung)

### 6.2 Agent-Infrastruktur

**Entities:**

| Entity | Tabelle | Beschreibung |
|---|---|---|
| `AgentTemplateEntity` | `agent_templates` | Name, Rolle, base_prompt (TEXT), allowed_tools (JSONB), trigger_types (JSONB), max_tokens_per_run, daily_token_budget, status, version |
| `AgentInstanceEntity` | `agent_instances` | template_id, name, parent_instance_id, type (PERSISTENT/EPHEMERAL), status (INACTIVE/ACTIVE/QUARANTINE/TERMINATED), config (JSONB) |
| `AgentRunEntity` | `agent_runs` | instance_id, trigger_type (CHAT/BUTTON/EVENT/SCHEDULE), input_context (JSONB), output (JSONB), status (PENDING/RUNNING/SUCCESS/FAILED/CANCELLED), tokens_used, cost_usd, error_message |
| `AgentRunStepEntity` | `agent_run_steps` | run_id, step_number, type (LLM_CALL/TOOL_CALL), tool_name, input/output (JSONB), tokens_used, duration_ms |
| `AgentIncidentEntity` | `agent_incidents` | instance_id, type, description, resolved_at |
| `AgentEventSubscriptionEntity` | `agent_event_subscriptions` | instance_id, event_type, active |
| `RoleAgentDefaultEntity` | `role_agent_defaults` | role → agent_instance_id |

**Seed-Daten (V4 Migration):**
8 Agent Templates + 8 zugehörige Instances:
1. CEO Agent (Überblick, Delegation)
2. Production Lead (Jobs, Stationen, Kapazität)
3. Support Lead (Konversationen)
4. Supply Lead (Bestand, kritische Artikel)
5. People Lead (Mitarbeiter, Zeiterfassung)
6. Machine Lead (Maschinen, Wartung)
7. Knowledge Lead (Wissensdatenbank)
8. Finance Lead (Kalkulationen, Budget)

3 Event Subscriptions: Supply←STOCK_CRITICAL, Machine←MACHINE_INCIDENT, Support←CONVERSATION_NEW
3 Scheduled Triggers: CEO 06:00, Production Lead 22:00, Supply Lead 07:00 (täglich)

### 6.3 Tool Registry

**Interface:** `AgentTool` – `getName()`, `getDescription()`, `getInputSchema()` (JSON Schema), `getPermission()`, `execute(ToolExecutionContext, String): ToolResult`

**Registrierung:** `AgentToolRegistry` sammelt alle `AgentTool` Spring Beans, gibt pro Template nur die erlaubten Tools zurück (basierend auf `allowedTools` JSONB-Array).

**13 implementierte Domain Tools:**

| Tool-Name | Klasse | Funktion |
|---|---|---|
| `get_kpi_summary` | `GetKpiSummaryTool` | Aggregierte KPIs: Jobs nach Status, kritischer Bestand, Maschinen-Verteilung, Mitarbeiter, offene Konversationen |
| `list_jobs` | `ListJobsTool` | Paginierte Job-Liste mit Filter |
| `get_job_details` | `GetJobDetailsTool` | Job-Detail inkl. Statushistorie |
| `get_stations` | `ListStationsTool` | Stationsliste |
| `get_capacity_overview` | `GetCapacityTool` | Stationskapazitäten |
| `get_stock_summary` | `ListCriticalStockTool` | Artikel unter Mindestbestand |
| `get_article_detail` | `GetArticleDetailTool` | Einzelartikel-Detail |
| `get_machine_overview` | `ListMachinesTool` | Maschinenliste mit Status |
| `get_maintenance_due` | `GetMaintenanceDueTool` | Überfällige und anstehende Wartungen |
| `get_employee_overview` | `ListEmployeesTool` | Mitarbeiterliste |
| `get_my_day` | `GetMyDayTool` | Heutige Zeiteinträge und Jobs eines Mitarbeiters |
| `get_open_conversations` | `ListConversationsTool` | Offene/aktive Konversationen |
| `get_conversation_detail` | `GetConversationDetailTool` | Konversation mit Nachrichten |

**Virtuelles Tool:** `delegate_to_agent` – nicht in der Registry, direkt im Execution Engine behandelt. Ermöglicht einem Agent, Aufgaben an einen anderen Agent zu delegieren.

**Alle Tools sind aktuell read-only** – sie lesen nur Daten aus den Domain-Services. Schreibende Tools existieren noch nicht.

### 6.4 ReAct Execution Engine

**AgentRunOrchestrator** (`@Async("agentExecutor")`):
- Einstiegspunkt: `triggerRun(instanceId, triggerType, triggerSource, inputContext)`
- Erstellt `AgentRunEntity` über `AgentRunService.startRun()`
- Delegiert an `AgentExecutionService.executeRun()` asynchron
- Bei Exception: Run als FAILED markieren + `AgentIncidentEntity` erstellen

**Thread-Pool:** `agentExecutor` – Core 4, Max 8, Queue 50. `TenantAwareTaskDecorator` propagiert TenantContext über Thread-Grenzen.

**AgentExecutionService.executeRun()** (ReAct-Loop):
1. Run, Instance, Template laden
2. Budget-Check (tägliches Token-Budget aus Template)
3. Erlaubte Tools aus Registry laden
4. Modell auflösen (Instance Config > LLM Config > Fallback `claude-sonnet-4-20250514`)
5. System-Prompt via `SystemPromptBuilder` erstellen
6. API Key entschlüsseln
7. `LlmToolDefinition` Liste + `delegate_to_agent` aufbauen
8. Conversation initialisieren: `[user(inputContext)]`
9. **Loop bis max 15 Iterationen:**
   - LLM aufrufen (`provider.chat()`)
   - `LLM_CALL` Step loggen
   - Bei `stop_reason == "end_turn"`: Loop beenden
   - Bei `stop_reason == "tool_use"`:
     - Assistant-Message mit Tool-Use hinzufügen
     - `delegate_to_agent`: `handleDelegation()` (max Tiefe 2, rekursiver Child-Run)
     - Sonstige: Tool ausführen über Registry
     - `TOOL_CALL` Step loggen
     - Tool-Ergebnis als User-Message hinzufügen
10. Kosten berechnen (hardcodiertes Pricing: Opus $15/$75, Haiku $0.80/$4.00, Sonnet $3/$15 pro 1M Tokens)
11. `runService.completeRun()` aufrufen

**SystemPromptBuilder:** Verkettet Template-BasePrompt + Kontext (Datum, Tenant, Rolle) + Tool-Liste mit Schemas + Delegate-Info + Regeln (deutsch antworten, max 15 Iterationen, etc.)

### 6.5 Agent Console & Chat

**Aktueller Stand:**

- `/agents` Route: **Platzhalter-Seite** mit nur einer Überschrift „Agent Console". Keine Chat-UI implementiert.
- AgentPanel (rechte Sidebar): **Empty State** mit Sparkles-Icon. Kein Chat, kein Input-Feld.
- **Kein WebSocket/STOMP/SSE** im gesamten Backend. Keine Real-Time-Push-Mechanismen.
- Der ReAct-Loop läuft asynchron (`@Async`), aber es gibt keinen Mechanismus, das Ergebnis live ans Frontend zu streamen.
- `AgentRunController` bietet `POST /api/agent-runs` zum Starten und `GET /api/agent-runs/{id}` zum Abfragen von Runs – der Client müsste also pollen.

**Agent Run API-Endpoints:**
- `POST /api/agent-runs` – Run starten
- `GET /api/agent-runs/{id}` – Run mit Steps abfragen
- `GET /api/agent-runs` – Runs filtern (instanceId, status)
- `PATCH /{id}/complete`, `PATCH /{id}/fail`, `PATCH /{id}/cancel` – Status-Übergänge
- `GET /budget/{instanceId}` – Budget-Check
- `POST /trigger` – Async Trigger

### 6.6 Event-Routing

**DomainEventProcessor** (`@Scheduled(fixedDelay=10000)`, alle 10s):
- Iteriert alle aktiven Tenants
- Pro Tenant: unverarbeitete `DomainEventEntity` Records finden
- Event-Type gegen `agent_event_subscriptions` matchen
- Async Run triggern für jede aktive abonnierte Instance
- Event als verarbeitet markieren

**ScheduledRunExecutor** (`@Scheduled(fixedDelay=60000)`, alle 60s):
- Trigger mit `next_run_at <= now` finden
- Async Run triggern wenn Instance ACTIVE
- `last_run_at` / `next_run_at` updaten

---

## 7. Datenbank & Row-Level Security

### 7.1 Schema & Migrationen

**Ein Schema:** `public` – alle Tabellen in einem Schema.

**Flyway-Migrationen (V1–V10):**

| Migration | Inhalt |
|---|---|
| V1 | `tenants`, `users`, `refresh_token_blacklist`. Seed: Default Tenant + Admin User |
| V2 | 30+ Domain-Tabellen mit ENUMs. Jede Tabelle hat `tenant_id UUID NOT NULL REFERENCES tenants(id)` + Index |
| V3 | `current_tenant_id()` PL/pgSQL Funktion + RLS Policies dynamisch auf allen Tabellen mit `tenant_id` (ausgenommen `users` und `tenants`) |
| V4 | 8 Agent Templates, 8 Instances, 3 Event Subscriptions, 3 Scheduled Triggers |
| V5 | `role_agent_defaults` Tabelle + RLS, `users.primary_agent_instance_id` |
| V6 | Role-Defaults: ADMIN/MANAGER→CEO, TEAM_LEAD→Prod Lead, WORKER→People Lead |
| V7 | Tenant-Extension (slug, plan, status), System Admin User Seed |
| V8 | `customer_number`, `short_name` auf `customers` + Unique Index |
| V9 | Knowledge-Tabellen, Dokument-Erweiterung, RLS |
| V10 | Tenant-Config-Felder (Logo, Kontakt, Adresse etc.), `user_notification_settings` + RLS |

**FlywayConfig:** Läuft programmatisch auf `ApplicationReadyEvent`, `baselineOnMigrate=true`, um Circular Dependency mit `TenantAwareDataSource` zu vermeiden.

### 7.2 Multi-Tenancy (RLS)

**Architektur:** Single-Schema, alle Tabellen in `public` mit `tenant_id` Spalte.

**Enforcement:**
1. `JwtAuthenticationFilter` extrahiert `tenantId` aus JWT und setzt `TenantContext.setCurrentTenant(tenantId)` (ThreadLocal)
2. `TenantAwareDataSource` (extends `DelegatingDataSource`) überschreibt `getConnection()` und führt auf jeder JDBC-Connection aus: `SELECT set_config('app.current_tenant', ?, false)`
3. PostgreSQL RLS Policy `tenant_isolation` nutzt `current_tenant_id()` Funktion: `current_setting('app.current_tenant', true)::UUID`

**Ausnahmen (kein RLS):**
- `tenants` Tabelle (enthält alle Tenants)
- `users` Tabelle (hat eigene tenant_id Logik, aber kein RLS – User werden über Service-Logik gefiltert)
- `SYSTEM_ADMIN` bypassed TenantContext komplett (kein `set_config` Call)

**BaseEntity:** Abstrakte Klasse mit `tenant_id` Feld, `@PrePersist` setzt `tenant_id` aus `TenantContext`.

**Async-Propagation:** `TenantAwareTaskDecorator` kopiert `TenantContext` beim Thread-Wechsel (wichtig für Agent-Execution Thread-Pool).

### 7.3 Indizes

Jede Domain-Tabelle hat einen Index auf `tenant_id`. Zusätzliche Unique-Indizes auf:
- `(tenant_id, customer_number)` auf `customers`
- `(tenant_id, slug)` auf `knowledge_articles`
- `(tenant_id, name)` auf `knowledge_tags`

---

## 8. Authentifizierung & Rollenmodell

### 8.1 Rollen

| Rolle | Beschreibung | Frontend-Zugang |
|---|---|---|
| `SYSTEM_ADMIN` | Globaler Systemadministrator, kein Tenant-Kontext | Nur `/system/companies` (SystemShell) |
| `ADMIN` | Tenant-Administrator | Alle Routen inkl. Settings |
| `MANAGER` | Manager | Alle Routen inkl. Settings |
| `TEAM_LEAD` | Teamleiter | Alle Routen außer Settings |
| `WORKER` | Arbeiter | Hauptsächlich `/my-day`, `/inbox`, Produktion, Maschinen, Lager etc. – kein Settings |
| `AGENT_SYSTEM` | System-Rolle für Agent-Aktionen | Nur Backend (kein Frontend-Login) |

### 8.2 Auth-Flow Backend

1. **Login:** `POST /api/auth/login` mit `{ email, password }` (kein `tenantId` im Request)
2. `AuthService` sucht User per Email, verifiziert BCrypt-Passwort
3. Access Token (24h) generiert: Claims `sub`, `email`, `tenantId`, `role`, `type=access`
4. Refresh Token (7d) generiert: Claims `sub`, `tenantId`, `type=refresh`
5. **JWT-Signierung:** HMAC-SHA mit konfiguriertem Secret
6. **Refresh:** `POST /api/auth/refresh` – altes Refresh Token wird geblacklistet (SHA-256 Hash), neues Paar generiert
7. **Logout:** `POST /api/auth/logout` – Refresh Token wird geblacklistet
8. **Rate Limiting:** Bucket4j, 10 Requests/Minute pro IP auf Login-Endpoint

### 8.3 JWT-Filter

`JwtAuthenticationFilter` (vor `UsernamePasswordAuthenticationFilter`):
1. `Authorization: Bearer <token>` Header extrahieren
2. Token validieren, `type=access` prüfen
3. `SYSTEM_ADMIN` → kein TenantContext gesetzt (bypassed RLS)
4. Alle anderen → `TenantContext.setCurrentTenant(tenantId)`
5. `UsernamePasswordAuthenticationToken` mit `ROLE_{role}` in SecurityContext setzen
6. `TenantContext` im `finally` Block clearen

### 8.4 System Admin

- **User:** `philipp.ebert@strate-software` / `N0n3Xx.Blender` (V7 Migration)
- **Rolle:** `SYSTEM_ADMIN` (kein Tenant zugeordnet)
- **API:** `SystemCompanyController` (`/api/system/companies`):
  - Alle Firmen auflisten, erstellen (generiert automatisch Admin-Passwort), bearbeiten
  - Firma suspendieren/aktivieren/löschen
  - Statistiken und Admin-Übersicht pro Firma
  - Admin-Passwort zurücksetzen

### 8.5 Default Admin

- **User:** `software@sindojan.de` / `root1234` (V1 Migration)
- **Rolle:** `ADMIN`
- **Tenant:** Default Tenant (`00000000-0000-0000-0000-000000000001`)

---

## 9. Settings & Konfiguration

### 9.1 Frontend Settings-Tabs

Die Settings-Seite (`/settings`) hat 8 Tabs, rollenabhängig sichtbar:

| Tab | Sichtbar für | Beschreibung |
|---|---|---|
| LLM-Konfiguration | Alle | Provider (nur Anthropic), API Key, Modell-Auswahl, Verbindungstest |
| Agenten | Alle | Agent-Instance-Liste mit inline Modell-Selektor, Detail-Sheet (System Prompt, Token-Budget, Tool-Whitelist mit Permission-Badges) |
| Rollen & Agents | Alle | Pro Rolle (ADMIN/MANAGER/TEAM_LEAD/WORKER) Standard-Agent-Instance zuweisen |
| Wissen | Alle | Kategorien-CRUD (Name + Farbauswahl), Tags-CRUD (als Chips) |
| Benutzer | ADMIN, SYSTEM_ADMIN | User-CRUD: Erstellen (Passwort-Generierung), Bearbeiten (Name/Rolle/Primary Agent), Passwort-Reset, Aktivieren/Deaktivieren, Löschen |
| Firma | ADMIN, SYSTEM_ADMIN | Logo-Upload (Drag-and-Drop, 2MB), Firmenname, Kontaktdaten, Adresse, USt-IdNr |
| Budget | ADMIN, MANAGER, SYSTEM_ADMIN | 3 KPI-Cards (Kosten/Tokens/Runs aktueller Monat), 30-Tage LineChart, Agent-Aufschlüsselung (Read-only) |
| Benachrichtigungen | Alle | Toggle-Switches: Agent-Run-Benachrichtigungen, Betrieb (Manager-only: Bestand, Maschinen, Jobs, Abwesenheiten), Posteingang, In-App (immer an), E-Mail |

### 9.2 Backend Settings-APIs

- `LlmConfigController` (`/api/settings/llm`): Config-Status, Speichern, Modelle auflisten
- `RoleAgentDefaultController` (`/api/settings/role-agent-defaults`): CRUD für Rollen-Agent-Zuordnungen
- `TenantConfigController` (`/api/tenant`): Tenant-Daten lesen/aktualisieren, Logo Upload/Delete
- `BudgetController` (`/api/budget/overview`): Budget-Übersicht (Kosten, Tokens, Runs pro Agent)
- `NotificationSettingsController` (über `/api/users/me/notifications`): Benachrichtigungseinstellungen pro User
- Agent Templates/Instances CRUD über die Standard-Agent-Endpoints

---

## 10. Infrastruktur & Deployment

### 10.1 Docker Compose

**Produktions-Stack** (`docker-compose.yml`): 5 Services
- `postgres` (postgres:16-alpine) – Volume `postgres_data`, Healthcheck via `pg_isready`
- `minio` (minio/minio:latest) – Console auf Port 9001, Volume `minio_data`
- `backend` (Multi-Stage Dockerfile: `eclipse-temurin:21-jdk-alpine` Build → `eclipse-temurin:21-jre-alpine` Runtime, Non-Root User `owlsburg`)
- `frontend` (Standalone Next.js Build)
- `nginx` (nginx:alpine) – Reverse Proxy, Port `${HTTP_PORT:-80}`

**Dev-Compose** (`docker-compose.dev.yml`): Nur Postgres + MinIO mit Dev-Credentials
- Postgres: `5432:5432`
- MinIO: `9000:9000` + `9002:9001` (Console remapped)
- Backend/Frontend/Nginx auf `profiles: ["prod-only"]` (laufen nativ)

**Monitoring-Stack** (`docker-compose.monitoring.yml`): 4 Services
- Loki (Port 3100) – Log-Aggregation
- Promtail – Docker Container Logs → Loki
- Prometheus (Port 9090) – Scrapes `/actuator/prometheus`
- Grafana (Port 3030) – Dashboards

### 10.2 Scripts

| Script | Funktion |
|---|---|
| `scripts/dev.sh` | Startet nur Postgres + MinIO, gibt Anweisungen für nativen Backend/Frontend Start |
| `scripts/prod.sh` | Prüft `.env`, baut und startet alle Container |
| `scripts/backup.sh` | Erstellt timestamped Backup: `pg_dump` + MinIO Volume Tar |
| `scripts/restore.sh` | Stellt aus Backup-Verzeichnis wieder her (mit Bestätigungs-Prompt) |

### 10.3 Monitoring-Anbindung

- Backend exponiert Actuator-Endpoints: `/actuator/health`, `/actuator/info`, `/actuator/prometheus`, `/actuator/metrics`
- Prometheus scrapes Backend alle 15s (konfiguriert in `docker/prometheus/prometheus.yml`)
- Grafana-Datenquellen: Prometheus + Loki (auto-provisioned)

---

## 11. Bekannte technische Schulden / TODOs

### Code-Marker

**Es gibt exakt null `TODO`, `FIXME`, `HACK` oder `XXX` Marker** im gesamten Quellcode (weder Backend noch Frontend). Ebenso keine Kommentare zu „Phase 2", „later", „future", „Memory" oder „Supervisor".

### Strukturelle Schulden & Lücken

| Kategorie | Beschreibung |
|---|---|
| **Agent** | Agent Console (`/agents`) ist eine Platzhalter-Seite ohne Funktionalität |
| **Agent** | AgentPanel (rechte Sidebar) zeigt nur Empty State – kein Chat-Input, keine Interaktion |
| **Agent** | Kein WebSocket/SSE für Real-Time Agent-Run-Updates – Client müsste pollen |
| **Agent** | Alle 13 Tools sind read-only – keine schreibenden Tools (z.B. Job-Status ändern, Bestellung auslösen) |
| **Agent** | Kosten-Pricing ist hardcoded im Code statt konfigurierbar |
| **Agent** | Kein Agent Memory / Konversationshistorie über Runs hinweg |
| **Agent** | Kein Supervisor-Pattern implementiert |
| **Frontend** | Inkonsistente Routen: `/employees` (Liste) vs. `/people/[id]` (Detail) |
| **Frontend** | Kein Server-Side Rendering für geschützte Routen – alles client-side Auth-Guards |
| **Frontend** | Kein Error Boundary für einzelne Page-Crashes |
| **Backend** | `users` Tabelle hat kein RLS – Filterung nur über Service-Logik |
| **Backend** | `@Value` mit `List<String>` funktioniert nicht für comma-separated YAML – Workaround mit String + `.split(",")` |
| **Backend** | Lombok nur für Entity-Annotations – alle Services haben explizite Konstruktoren und Logger |
| **Infra** | Kein README.md im Root (nur CLAUDE.md + DEPLOYMENT.md) |
| **Infra** | `folder_structure.txt` ist veraltet (2.6MB, enthält „sindoflow" Referenzen) |

---

## 12. Abweichungen zum Masterplan

**Es existiert kein separates Masterplan-Dokument im Repository.** Die CLAUDE.md dient als einzige Projektdokumentation. Daher kann kein formaler Abgleich durchgeführt werden.

Basierend auf den in CLAUDE.md dokumentierten „nächsten Blöcken":

| Geplant (laut CLAUDE.md) | IST-Zustand |
|---|---|
| Block 13: Settings vervollständigen | **Erledigt** – als Block 12b umgesetzt (V10 Migration, 5 neue Tabs) |
| Block 14: Docker/Deployment | **Erledigt** – als Block 12 Infra umgesetzt |
| Block 15: Agent Console | **Nicht implementiert** – nur Platzhalter-Route vorhanden |
| Agent Memory | **Nicht implementiert** – keine Entitäten oder Services dafür |
| Agent Supervisor | **Nicht implementiert** – Delegation existiert (max Tiefe 2), aber kein echtes Supervisor-Pattern |

**Beobachtung:** Die CLAUDE.md zeigt unter „Zuletzt bearbeitet" noch „Nächste Blöcke: Block 13, Block 14, Block 15" an, obwohl Block 13 und 14 bereits erledigt sind. Die CLAUDE.md ist in diesem Punkt nicht aktuell.

---

## 13. Offene Fragen / Unsicherheiten

> **Hinweis:** Die folgenden Punkte sind Interpretationen basierend auf Code-Analyse. Sie sind nicht als Fakten zu verstehen.

1. **Agent Console Architektur:** Die Route `/agents` existiert als Platzhalter und die Root-Route `/` leitet dorthin weiter. Die genaue Zielarchitektur (Chat-UI, Run-Viewer, etc.) ist im Code nicht erkennbar. Es ist unklar, ob die Agent Console die rechte Sidebar (AgentPanel) ersetzen oder ergänzen soll.

2. **Real-Time Strategie:** Ohne WebSocket/SSE muss der Agent-Run-Status über Polling abgefragt werden. Es ist unklar, ob dies bewusst so geplant ist oder ob eine Real-Time-Lösung vorgesehen war.

3. **`AGENT_SYSTEM` Rolle:** Diese Rolle existiert im Enum, aber es ist unklar, wie/wo sie aktiv genutzt wird. Vermutlich für automatisierte Agent-Aktionen gedacht, die als System-User ausgeführt werden.

4. **Delegation vs. Supervisor:** Der ReAct-Loop unterstützt `delegate_to_agent` mit max Tiefe 2. Es ist unklar, ob dies als Basis für ein Supervisor-Pattern dient oder ob ein separater Mechanismus geplant ist.

5. **Schreibende Tools:** Alle 13 Tools sind read-only. Es ist unklar, ob schreibende Tools (z.B. Job-Status ändern, Bestellung erstellen) bewusst ausgespart wurden oder noch implementiert werden sollen.

6. **Employee Route Inkonsistenz:** `/employees` für die Liste, aber `/people/[id]` für das Detail. Möglicherweise historisch gewachsen, möglicherweise bewusst.

7. **E-Mail-Benachrichtigungen:** Der Settings-Tab hat E-Mail-Toggles, aber es ist kein E-Mail-Service oder SMTP-Konfiguration im Backend erkennbar. Vermutung: UI ist vorbereitet, Backend-Implementation fehlt noch.

8. **Scheduled Trigger Cron:** Die Seed-Daten setzen Cron-Expressions (CEO 06:00, Prod Lead 22:00, Supply Lead 07:00), aber der `ScheduledRunExecutor` pollt alle 60s nach `next_run_at`. Es ist unklar, wie die Cron-Expression in `next_run_at` übersetzt wird (kein Cron-Parser im Code gefunden – möglicherweise manuell bei Trigger-Update berechnet).

9. **Budget-Limits:** Der Budget-Tab zeigt Kosten an, aber es ist unklar, ob das tägliche Token-Budget (`daily_token_budget`) bei Überschreitung tatsächlich Runs blockiert oder nur warnt. Die `checkBudget()` Methode gibt ein `BudgetCheckResult` zurück – die Entscheidung, ob ein Run gestartet wird, hängt vom Caller ab.

10. **MinIO Bucket-Erstellung:** Der `MinioConfig` prüft und erstellt den Bucket `owlsburg-documents` beim Start. Es ist unklar, ob verschiedene Tenants eigene Buckets bekommen oder alle denselben nutzen (Vermutung: ein Bucket, Trennung über Object-Key-Prefix mit tenant_id).
