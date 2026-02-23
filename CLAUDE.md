# Owlsburg OPS – Projektdokumentation

## Projekt

**Owlsburg OPS** – Agentenbasierte Operations-Plattform für eine Auto-Sitz-Manufaktur.
**Repo:** https://github.com/Sindojan/OPS-remastered
**GitHub Account:** Sindojan

## Tech Stack

| Layer | Technologie |
|-------|-------------|
| Frontend | Next.js 16, React 19, shadcn/ui, Tailwind CSS v4, TypeScript |
| Backend | Spring Boot 3.5.0, Java 21, Maven |
| Datenbank | PostgreSQL (Multi-Schema per Tenant) |
| Migrations | Flyway (programmatisch, pro Schema) |
| Auth | JWT (JJWT 0.12.6, stateless), Spring Security |
| Dateispeicher | MinIO (Dokumente, Binärdateien) |
| Typografie | DM Sans (body) + JetBrains Mono (mono/data) |
| Charts | recharts |
| Icons | lucide-react |

## Architekturprinzipien

1. **Deterministisch** – Alle Geschäftslogik ist deterministisch. Kein LLM in Domänenlogik.
2. **Modularer Monolith** – Package-by-Domain, kein Microservice-Split.
3. **Multi-Schema ab Tag 1** – Jeder Tenant hat ein eigenes PostgreSQL-Schema. Nicht verhandelbar.
4. **Kein LLM in Domänenlogik** – LLM-Agenten greifen nur über Tool Registry auf Services zu.
5. **Tool Registry für Agents** – Einzige Schnittstelle zwischen Agent-Runtime und Domain-Services.
6. **Cost Control** – Token-Verbrauch und Kosten werden von Anfang an in AgentRun erfasst.

## Globale Constraints

- Kein Agent schreibt außerhalb seines definierten Layers
- Kein direkter DB-Zugriff durch Agent-Layer
- Alle Statusübergänge sind deterministisch
- Multi-Schema-Isolation ist nicht verhandelbar
- Frontend: shadcn/ui + Tailwind only, keine eigenen CSS-Klassen
- Backend: Controller → Service → Repository (nie überspringen)

## Agent-Verzeichnis

| Agent | Rolle | Skill-File |
|-------|-------|-----------|
| backend-structure-agent | Projektstruktur, Module, Build | `.claude-agents/agents/backend-structure-agent.md` |
| db-agent | Schema-Design, Flyway, Entities | `.claude-agents/agents/db-agent.md` |
| auth-agent | JWT, Security, Tenant-Kontext | `.claude-agents/agents/auth-agent.md` |
| domain-agent | Business Logic, Services | `.claude-agents/agents/domain-agent.md` |
| api-agent | REST Controller, DTOs | `.claude-agents/agents/api-agent.md` |
| event-agent | Domain Events, Listener | `.claude-agents/agents/event-agent.md` |
| agentfactory-agent | AgentTemplate, Tool Registry | `.claude-agents/agents/agentfactory-agent.md` |
| frontend-agent | Next.js UI, Pages, Components | `.claude-agents/agents/frontend-agent.md` |

## Design Direction: "Industrial Precision"

- Kontrollraum-Ästhetik, industriell-utilitär
- Teal/Cyan Primary (oklch hue 195)
- Sidebar permanent dunkel (Slate-Blau) in beiden Themes
- Dot-Grid Hintergrund im Content-Bereich
- Monospace für Datenwerte (JetBrains Mono)
- StatusBadge mit rounded-md, Puls-Animation für "busy"

## Monorepo-Struktur

```
sindojan_ops_remastered/
├── CLAUDE.md                        # Diese Datei
├── .gitignore                       # Root Gitignore
├── .claude-agents/                  # Agent Skill-Files (nicht in Git)
│   └── agents/                      # 8 Agent-Definitionen
├── frontend/                        # Next.js Frontend
│   ├── app/                         # Routen (App Router)
│   ├── components/                  # UI + Layout + Shared
│   ├── lib/                         # Utilities
│   └── types/                       # TypeScript Typen
└── backend/                         # Spring Boot Backend
    ├── pom.xml
    └── src/main/java/com/owlsburg/ops/
        ├── common/                  # BaseEntity, TenantContext, Exceptions
        ├── config/                  # Security, JPA, Flyway, CORS, MinIO, PasswordEncoder
        ├── auth/                    # JWT, Login, User-CRUD, Rollen
        │   └── dto/                 # Auth DTOs (LoginRequest, UserResponse, etc.)
        ├── tenant/                  # Tenant-Management, Schema-Provisionierung
        │   └── dto/                 # Tenant DTOs
        ├── customers/               # Kunden, Kontakte, Adressen, Preisgruppen
        │   └── dto/
        ├── production/              # Jobs (Status-Maschine), Stationen, Schichten, QA
        │   └── dto/
        ├── machines/                # Maschinen, Wartung, Störungen
        │   └── dto/
        ├── people/                  # Mitarbeiter, Zeiterfassung, Abwesenheiten
        │   └── dto/
        ├── inventory/               # Lager, Artikel, Bestand, Lieferanten
        │   └── dto/
        ├── bom/                     # Stücklisten, Arbeitspläne, Kalkulation
        │   └── dto/
        ├── documents/               # Dokumente (Metadaten in DB, Binär in MinIO)
        │   └── dto/
        ├── inbox/                   # Conversations, Nachrichten, Tags
        │   └── dto/
        ├── events/                  # Domain Events, Scheduled Triggers
        │   └── dto/
        └── agentinfra/              # Agent Templates, Instances, Runs, Steps
            ├── dto/
            ├── llm/                 # LLM Provider, Config, Anthropic-Integration
            ├── tools/               # Tool Registry + 13 Domain Tools
            │   └── impl/
            ├── execution/           # ReAct-Loop, Orchestrator, SystemPromptBuilder
            └── events/              # Event Subscriptions, Scheduled Run Executor
```

## Abgeschlossene Tasks

### Block 1: Frontend Foundation ✅
| Task | Beschreibung | Commit |
|------|-------------|--------|
| TASK-FE-001 | Next.js Projekt initialisieren | `ea88b3a` |
| TASK-FE-002 | Drei-Bereich-Layout Shell | `bc8e4e7` |
| TASK-FE-003 | Navigationsrouten & leere Pages | `33f17c6` |
| TASK-FE-004 | Design Tokens & globale Styles | `1b8722b` |

### Block 2: Shared Component Library ✅
| Task | Beschreibung | Commit |
|------|-------------|--------|
| TASK-FE-005 | KPI Card (Sparkline, Trend) | `6d268d0` |
| TASK-FE-006 | DataTable\<T\> | `2fc8f8e` |
| TASK-FE-007 | PageHeader, ConfirmationDialog, Skeletons | `77c5247` |

### Block 3: Agent Setup & Backend Grundarchitektur ✅
| Task | Beschreibung | Status |
|------|-------------|--------|
| TASK-SETUP-001 | Claude Agent Struktur & Skill-Files | ✅ |
| TASK-BE-001 | Spring Boot Projektstruktur | ✅ |
| TASK-BE-002 | PostgreSQL Multi-Schema & Flyway | ✅ |

### Block 4+5: Auth, Tenant & vollständige Domänenarchitektur ✅
| Task | Beschreibung | Status |
|------|-------------|--------|
| TASK-BE-003 | Auth (JWT, Login, Refresh, Logout), User-CRUD, Tenant-Provisionierung | ✅ |
| TASK-BE-004 | Vollständiges DB-Schema (V3 Migration: alle Tabellen, Enums, Indizes) | ✅ |
| TASK-BE-005 | MinIO Setup & Document Service | ✅ |
| TASK-BE-006a | Kunden (CRUD, Kontakte, Adressen, Preisgruppen) | ✅ |
| TASK-BE-006b | Produktion (Jobs mit Status-Maschine, Stationen, Schichten, QA) | ✅ |
| TASK-BE-006c | Maschinen (CRUD, Wartung, Störungen) | ✅ |
| TASK-BE-006d | Mitarbeiter & Zeiterfassung (Clock-In/Out, MyDay, Abwesenheiten) | ✅ |
| TASK-BE-006e | Lager & Material (Artikel, Bestand, Bewegungen, Lieferanten) | ✅ |
| TASK-BE-006f | Stücklisten & Kalkulation (BOM, Arbeitspläne, Soll/Ist-Vergleich) | ✅ |
| TASK-BE-006g | Inbox & Support (Conversations, Messages, Tags, Links) | ✅ |
| TASK-BE-006h | Agent Infrastructure (Templates, Instances, Runs, Steps, Incidents) | ✅ |
| Events | Domain Events & Scheduled Triggers | ✅ |

### Block 6: Agent Infrastructure & LLM-Integration ✅
| Task | Beschreibung | Commit |
|------|-------------|--------|
| TASK-BE-007 | LLM Provider Abstraktionsschicht (Anthropic, AES-256-GCM) | `7bdf7ea` |
| TASK-BE-008 | Tool Registry (13 Domain Tools) | `7bdf7ea` |
| TASK-BE-009 | Agent Execution Engine (ReAct Loop, Delegation) | `7bdf7ea` |
| TASK-BE-010 | Seed Agent Templates & Instances (CEO + 7 Leads) | `7bdf7ea` |
| TASK-BE-011 | Event Routing & Scheduler | `7bdf7ea` |
| TASK-FE-008 | LLM Settings UI + Agent Instances Tab | `67821f2` |
| TASK-FIX-001 | CORS Fix, System-Test, Bugfixes | `67821f2` |

### Block 7: Frontend OPS-Views ✅
| Task | Beschreibung | Commit |
|------|-------------|--------|
| Block-7-infra | Domain Types, API Hooks, Format Utils, DomainStatusBadge | `bc0b0f6` |
| TASK-FE-009 | Production Views (Jobs, Planner, Stations) | `7db1d18` |
| TASK-FE-010 | Machines Views (Overview, Detail mit Wartung/Störungen) | `7db1d18` |
| TASK-FE-011 | Inventory Views (Artikel, Lieferanten, Bewegungen) | `c5df042` |
| TASK-FE-012 | People Views (Mitarbeiter, Zeiterfassung, My Day) | `c5df042` |
| TASK-FE-013 | Inbox (Split-Layout, Chat, Konversationen) | `5672775` |
| TASK-FE-014 | Reports (KPI Dashboard, Charts, CSV Export) | `5672775` |

## Arbeitsweise mit Agents

**Vor jeder Entwicklungsarbeit** das jeweilige Agent Skill-File aus `.claude-agents/agents/` lesen und dessen Regeln befolgen:
- Nur in den erlaubten Pfaden arbeiten
- Verbotene Aktionen einhalten
- Constraints beachten

Zuordnung:
- Backend-Struktur/Config → `backend-structure-agent.md`
- Datenbank/Entities/Migrations → `db-agent.md`
- Auth/Security/JWT → `auth-agent.md`
- Business Logic/Services → `domain-agent.md`
- REST Controller/DTOs → `api-agent.md`
- Domain Events → `event-agent.md`
- Agent-Infrastruktur → `agentfactory-agent.md`
- Frontend/UI → `frontend-agent.md`

## Wichtige Konventionen

- **Commits:** Conventional Commits mit Task-Nummern, Co-Authored-By Claude
- **Frontend:** `cd frontend && npx next dev --port 4201`
- **Backend:** `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) ./mvnw spring-boot:run` (Port 8080)
- **Java:** Version 21 (explizit via JAVA_HOME setzen, da 23 default)
- **Lombok:** Funktioniert nicht zuverlässig mit dem Maven Compiler – explizite Logger + Konstruktoren verwenden
- **Pfad-Aliases (FE):** `@/components`, `@/lib`, `@/types`

## API-Endpunkte (Übersicht)

| Bereich | Basis-Pfad | Auth |
|---------|-----------|------|
| Auth | `/api/auth/**` | public |
| Users | `/api/users` | ADMIN/MANAGER |
| Tenants | `/api/admin/tenants` | ADMIN |
| Customers | `/api/customers` | authenticated |
| Jobs | `/api/jobs` | authenticated |
| Stations | `/api/stations` | authenticated |
| Shifts | `/api/shifts` | authenticated |
| Quality | `/api/quality-checks` | authenticated |
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
| Conversations | `/api/conversations` | authenticated |
| Agent Templates | `/api/agent-templates` | authenticated |
| Agent Instances | `/api/agent-instances` | authenticated |
| Agent Runs | `/api/agent-runs` | authenticated |
| LLM Config | `/api/settings/llm` | authenticated |
| Events | `/api/events` | authenticated |
| Triggers | `/api/scheduled-triggers` | authenticated |
| Health | `/actuator/health` | public |

## Flyway Migrationen

| Schema | Migration | Inhalt |
|--------|----------|--------|
| public | V1__init_public.sql | Tenants-Tabelle |
| tenant | V1__init_tenant_schema.sql | Platzhalter |
| tenant | V2__users_and_auth.sql | Users, Refresh Token Blacklist, Default Admin |
| tenant | V3__full_schema.sql | Alle Domänen-Tabellen, 25 Enums, ~80 Indizes |
| tenant | V4__llm_config.sql | tenant_llm_config (Provider, API Key encrypted, Model) |
| tenant | V5__seed_agent_templates.sql | 8 Agent Templates (CEO + 7 Leads) |
| tenant | V6__seed_agent_instances.sql | 8 Agent Instances (PERSISTENT, ACTIVE) |
| tenant | V7__event_subscriptions_and_triggers.sql | Event Subscriptions + Scheduled Triggers |

## Default Admin

- **Email:** software@sindojan.de
- **Passwort:** root1234
- **Rolle:** ADMIN
- Wird automatisch bei jeder neuen Tenant-Schema-Erstellung angelegt (via V2 Migration)

## Rollen

`ADMIN`, `MANAGER`, `TEAM_LEAD`, `WORKER`, `AGENT_SYSTEM`

## Zuletzt bearbeitet

**Datum:** 2026-02-23
**Session:** Block 7 komplett (Alle Frontend OPS-Views implementiert)
**Status:** Backend ~400 Java-Dateien, alle APIs getestet. Frontend hat alle Domain-Views: Production (Jobs, Planner, Stations), Machines (Detail, Wartung, Störungen), Inventory (Artikel, Lieferanten, Bewegungen), People (Mitarbeiter, Zeiterfassung, Abwesenheiten, My Day), Inbox (Split-Layout Chat), Reports (KPI Dashboard, Charts, CSV Export). Alles TypeScript-fehlerfrei.
**Nächster Block:** Agent Console (Chat-Interface, Agent Hierarchy, Live-Runs) oder End-to-End-Testing mit echtem Backend
