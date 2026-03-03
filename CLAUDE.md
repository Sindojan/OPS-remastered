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
| Datenbank | PostgreSQL (Single-Schema mit Row Level Security) |
| Migrations | Flyway (programmatisch, flache Struktur) |
| Rate Limiting | Bucket4j (Login: 10 req/min pro IP) |
| Auth | JWT (JJWT 0.12.6, stateless), Spring Security |
| Dateispeicher | MinIO (Dokumente, Binärdateien) |
| Typografie | DM Sans (body) + JetBrains Mono (mono/data) |
| Charts | recharts |
| Icons | lucide-react |

## Architekturprinzipien

1. **Deterministisch** – Alle Geschäftslogik ist deterministisch. Kein LLM in Domänenlogik.
2. **Modularer Monolith** – Package-by-Domain, kein Microservice-Split.
3. **RLS ab Tag 1** – Single-Schema mit PostgreSQL Row Level Security. Alle Tabellen haben `tenant_id`, RLS-Policies filtern automatisch. Nicht verhandelbar.
4. **Kein LLM in Domänenlogik** – LLM-Agenten greifen nur über Tool Registry auf Services zu.
5. **Tool Registry für Agents** – Einzige Schnittstelle zwischen Agent-Runtime und Domain-Services.
6. **Cost Control** – Token-Verbrauch und Kosten werden von Anfang an in AgentRun erfasst.

## Globale Constraints

- Kein Agent schreibt außerhalb seines definierten Layers
- Kein direkter DB-Zugriff durch Agent-Layer
- Alle Statusübergänge sind deterministisch
- RLS-Isolation ist nicht verhandelbar (jede Tabelle hat tenant_id + RLS Policy)
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
│   │   ├── login/                   # Login-Seite (außerhalb AppShell)
│   │   └── (app)/                   # Auth-geschützte Route Group (mit AppShell)
│   ├── components/                  # UI + Layout + Shared + Chat
│   ├── contexts/                    # AuthContext Provider
│   ├── hooks/                       # Custom Hooks (usePrimaryAgent, API Hooks)
│   ├── lib/                         # Utilities
│   └── types/                       # TypeScript Typen
└── backend/                         # Spring Boot Backend
    ├── pom.xml
    └── src/main/java/com/owlsburg/ops/
        ├── common/                  # BaseEntity, TenantAwareBaseEntity, TenantContext, Exceptions
        ├── config/                  # Security, JPA, Flyway, CORS, MinIO, RlsTenantInterceptor, LoginRateLimiter
        ├── auth/                    # JWT, Login, AuthService, User-CRUD, Rollen
        │   └── dto/                 # Auth DTOs (LoginRequest, UserResponse, etc.)
        ├── tenant/                  # Tenant-Management
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
        ├── knowledge/               # Wissensdatenbank (Artikel, Kategorien, Tags, Suche)
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
| TASK-BE-002 | PostgreSQL & Flyway | ✅ |

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

### Block 8: RLS Migration & Rebranding ✅
| Task | Beschreibung | Commit |
|------|-------------|--------|
| FIX-BLOCK-A | Multi-Schema → Single-Schema RLS, Rebranding Sindoflow → Owlsburg | `c783058` |
| REVIEW-001 | System-Review (DB, Auth, Domain, Code Quality, Frontend) | – |
| FIX-BLOCK-B | CORS Fix, AuthService Extract, N+1 Fixes, Overdue Flag | `25a5fa1` |
| CORS-FIX | CORS @Value comma-separated string Fix | `edca221` |

### Block 9: Login, Agent-Button, My-Day Dashboard ✅
| Task | Beschreibung | Commit |
|------|-------------|--------|
| TASK-FE-015 | Login-Screen & Auth-Flow (AuthContext, Route Guard, User Dropdown) | `8d99a8a` |
| TASK-BE-012 | Primary Agent Backend (V5/V6 Migrationen, PrimaryAgentService, /me Endpoint) | `8d99a8a` |
| TASK-FE-016 | Dynamischer Agent-Button (usePrimaryAgent Hook) | `8d99a8a` |
| TASK-FE-017 | My-Day Dashboard (rollenbasiert, Clock-In/Out, Jobs, KPIs, Info-Cards) | `8d99a8a` |
| TASK-FE-018 | Settings: Rollen-Agent-Zuweisung Tab | `8d99a8a` |

### Block 10: System Admin & Employee Roles ✅
| Task | Beschreibung | Commit |
|------|-------------|--------|
| TASK-BE-013 | System Admin API (`/api/system/companies`), SYSTEM_ADMIN Rolle, V7 Migration, Tenant-Extension (slug/plan/status) | `42b87e3` |
| TASK-FE-019 | System Admin UI (`(system)` Route Group, SystemShell, Company CRUD, Detail mit Tabs) | `42b87e3` |
| Employee Roles | Funktionale Rolle → System-Rolle Mapping, Auto-User-Erstellung bei Mitarbeiter-Anlage | `42b87e3` |

### Block 11: Kunden-Verwaltung & Parts/Processes ✅
| Task | Beschreibung | Commit |
|------|-------------|--------|
| TASK-BE-014 | V8 Migration (customer_number, short_name), CustomerEntity/DTO-Erweiterung, Calculation History Endpoint | `41cd131` |
| TASK-FE-020 | Kunden-Views (Liste mit KPIs, Detail mit 5 Tabs: Übersicht, Ansprechpartner, Adressen, Preisgruppen, Historie) | `41cd131` |
| TASK-FE-021 | Parts & Processes Views (Teile, BOM Baumstruktur, Arbeitsplan, Kalkulations-Panel) | `41cd131` |

### FIX-BLOCK-D: UI Konsistenz & Deutsch-Lokalisierung ✅
| Task | Beschreibung | Commit |
|------|-------------|--------|
| FIX-1 | "Neuer Kunde" Button in Tabellen-Toolbar, Breadcrumb-Fix (fehlende Routen) | `a847882` |
| FIX-2 | Komplette deutsche Lokalisierung (31 Dateien, zentrale i18n.ts, 150+ Begriffe) | `a847882` |
| FIX-3 | Mitarbeiter-Rollenauswahl auf System-Rollen vereinfacht (WORKER, TEAM_LEAD, MANAGER, ADMIN) | `a847882` |

### Block 12: Knowledge & Dokumente ✅
| Task | Beschreibung | Commit |
|------|-------------|--------|
| TASK-BE-015 | V9 Migration (knowledge_categories, knowledge_articles, knowledge_tags), Knowledge CRUD, Search, Document-Erweiterungen (preview, metadata update) | `65b1a2c` |
| TASK-FE-022 | Knowledge Overview, Markdown-Editor (MDEditor), Artikel-Detail, Dokument-Management mit Vorschau, Settings-Tab für Kategorien/Tags | `65b1a2c` |
| FIX-AUTH | AuthContext: /me-Validierung bei Start (stale Token nach DB-Reset), Select.Item empty-value Fixes, LazyInitializationException Fix | `65b1a2c` |

### FIX-BLOCK-E: Unicode-Encoding ✅
| Task | Beschreibung | Commit |
|------|-------------|--------|
| FIX-UNICODE | Unicode-Escape-Sequences (\u00F6 etc.) durch UTF-8 Zeichen ersetzt (9 Dateien) | `ccf7f5c` |

### Block 12b: Settings vervollständigen ✅
| Task | Beschreibung | Commit |
|------|-------------|--------|
| TASK-BE-016 | V10 Migration (Tenant-Config-Felder, user_notification_settings), TenantConfig API, Budget API, Notification Settings, Available Tools Endpoint | `f6f788f` |
| TASK-FE-023 | 5 neue Settings-Tabs (Benutzer, Firma, Agents erweitert, Budget, Benachrichtigungen), Switch-Komponente, use-settings Hooks | `f6f788f` |
| FIX-LLM | TenantLlmConfigEntity: BaseEntity-Vererbung für tenant_id/RLS | `ea80f43` |

### Block 13: Agent Console (Chat) ✅
| Task | Beschreibung | Commit |
|------|-------------|--------|
| TASK-BE-019 | Simple Chat POC – SSE Streaming Endpoint (`POST /api/chat/message`), Anthropic API Integration | `f7632ab` |
| TASK-FE-026 | Agent Panel mit SSE Streaming, Blinking Cursor, Auto-Scroll | `f7632ab` |
| TASK-FE-027 | Markdown-Rendering für Agent-Antworten (react-markdown, remark-gfm, rehype-highlight) | `ad5b386` |
| TASK-BE-020 | Chat Persistence – V11 Migration (chat_sessions, chat_messages), Session CRUD, History Loading | `228f3ba` |
| TASK-BE-021 | CEO System Prompt – V12 Migration, detaillierter Prompt mit `{{TENANT_NAME}}` Placeholder | `228f3ba` |
| TASK-FE-028 | Persistent Chat Frontend – Session-Liste, Auto-Load, Wechsel, Löschung | `228f3ba` |
| FIX-CHAT | TenantContext-Propagation in Virtual Thread, Greeting persistieren, LLM-Model im Header | `b4fe23e` |
| TASK-BE-022 | Per-Instance System Prompt – V13 Migration (`custom_system_prompt`), PATCH Endpoint, Editor in Settings | `6491189` |

### FIX-BLOCK-G: Agent Tenant-Isolation Security ✅
| Task | Beschreibung | Commit |
|------|-------------|--------|
| TASK-SEC-001 | `findByIdAndTenantId` auf allen Agent/Chat Repositories, `findByIdSecure()` im Service, AccessDeniedException → 403, PATCH Template Endpoint | `9e05204` |

### Block 13.5: CEO Tool-Calling ✅
| Task | Beschreibung | Commit |
|------|-------------|--------|
| TASK-BE-023 | ReAct-Loop in SimpleChatService (Streaming + Tool-Calling), 10 Chat-Tools (get_jobs, get_job_detail, update_job_status, get_machines, get_machine_detail, get_critical_stock, get_stock_level, get_attendance_today, get_absences, get_customer_orders), V14 Migration (CEO Prompt + allowed_tools) | - |
| TASK-FE-029 | Tool-Call/Result SSE Events im Agent-Panel anzeigen (inline als Info-Cards) | - |

### Block 13.6: CEO → Lead-Agent Delegation ✅
| Task | Beschreibung | Commit |
|------|-------------|--------|
| TASK-BE-024 | LeadAgentRunner (sync ReAct-Loop für Leads), DelegateToLeadTool, 9 neue Lead-Tools (Produktion, Maschinen, Lager, Personal, Support), ReorderRequest Entity/Service/Repo | - |
| TASK-BE-025 | V15 Migration (reorder_requests, CEO auf 2 Tools, Lead-Tool-Zuweisungen, Lead-Instance System-Prompts) | - |
| TASK-FE-030 | Delegation SSE-Events im Agent-Panel (delegation/delegationResult statt toolCall/toolResult) | - |

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
| System Companies | `/api/system/companies` | SYSTEM_ADMIN |
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
| Knowledge Articles | `/api/knowledge/articles` | authenticated |
| Knowledge Categories | `/api/knowledge/categories` | authenticated |
| Knowledge Tags | `/api/knowledge/tags` | authenticated |
| Knowledge Search | `/api/knowledge/search` | authenticated |
| Conversations | `/api/conversations` | authenticated |
| Agent Templates | `/api/agent-templates` | authenticated |
| Agent Instances | `/api/agent-instances` | authenticated |
| Agent Runs | `/api/agent-runs` | authenticated |
| LLM Config | `/api/settings/llm` | authenticated |
| Role-Agent Defaults | `/api/settings/role-agent-defaults` | ADMIN/MANAGER |
| User Me | `/api/users/me` | authenticated |
| Events | `/api/events` | authenticated |
| Chat | `/api/chat` | authenticated |
| Triggers | `/api/scheduled-triggers` | authenticated |
| Health | `/actuator/health` | public |

## Flyway Migrationen

Flache Struktur in `resources/db/migration/` (kein public/tenant Split mehr):

| Migration | Inhalt |
|----------|--------|
| V1__init_public.sql | Tenants, Users (mit tenant_id FK), Refresh Token Blacklist, Default Tenant + Admin |
| V2__full_schema.sql | Alle Domänen-Tabellen – jede mit `tenant_id UUID NOT NULL REFERENCES tenants(id)` + Index |
| V3__rls_policies.sql | `current_tenant_id()` Funktion + RLS Policies für alle tenant_id-Tabellen |
| V4__seed_agents.sql | Agent Templates, Instances, Event Subscriptions, Scheduled Triggers (mit tenant_id) |
| V5__primary_agent.sql | `role_agent_defaults` Tabelle + RLS + `users.primary_agent_instance_id` |
| V6__seed_role_defaults.sql | Rollen-Defaults: ADMIN/MANAGER→CEO, TEAM_LEAD→Production Lead, WORKER→People Lead |
| V7__system_admin.sql | Tenant-Extension (slug, plan, status, suspended_at, suspend_reason), SYSTEM_ADMIN User Seed |
| V8__customer_fields.sql | `customer_number VARCHAR(50)`, `short_name VARCHAR(100)` auf customers, Unique Index |
| V9__knowledge.sql | Knowledge-Tabellen (categories, articles, tags, article_tags), Document-Erweiterung (category_id, excerpt), RLS Policies |
| V10__tenant_config.sql | Tenant-Config-Felder, user_notification_settings, Budget-Felder |
| V11__chat_sessions.sql | chat_sessions, chat_messages Tabellen mit RLS Policies |
| V12__ceo_system_prompt.sql | CEO Agent Template: Detaillierter System-Prompt mit `{{TENANT_NAME}}` |
| V13__instance_system_prompt.sql | `custom_system_prompt TEXT` auf agent_instances |
| V14__ceo_tools_prompt.sql | CEO Tool-Section im System-Prompt, allowed_tools für 10 Tools |
| V15__lead_delegation.sql | reorder_requests Tabelle, CEO auf 2 Tools (delegate_to_lead, get_kpi_summary), Lead-Tool-Zuweisungen, Lead-Instance System-Prompts |

## Default Admin

- **Email:** software@sindojan.de
- **Passwort:** root1234
- **Rolle:** ADMIN
- Wird in V1 Migration angelegt (Default Tenant + Admin User)

## System Admin

- **Email:** philipp.ebert@strate-software
- **Passwort:** N0n3Xx.Blender
- **Rolle:** SYSTEM_ADMIN
- Wird in V7 Migration angelegt (kein Tenant, bypassed RLS)

## Rollen

`SYSTEM_ADMIN`, `ADMIN`, `MANAGER`, `TEAM_LEAD`, `WORKER`, `AGENT_SYSTEM`

Mitarbeiter-Erstellung bietet direkt System-Rollen zur Auswahl (WORKER, TEAM_LEAD, MANAGER, ADMIN). Kein funktionales Rollen-Mapping mehr.

## Multi-Tenancy (RLS)

- **Architektur:** Single-Schema, alle Tabellen in `public` Schema mit `tenant_id` Spalte
- **RLS Enforcement:** `TenantAwareDataSource` (DelegatingDataSource) setzt `set_config('app.current_tenant', ?, false)` auf jeder JDBC Connection
- **BaseEntity:** Hat `tenant_id` Feld, `@PrePersist` setzt aus `TenantContext`
- **TenantAwareBaseEntity:** MappedSuperclass für Entities die nicht BaseEntity erweitern (z.B. Join-Tables, History)
- **Ausnahmen (kein RLS):** TenantEntity, UserEntity (eigene tenant_id Logik), RefreshTokenBlacklistEntity (global)
- **Login:** Nur email + password, tenantId wird aus User-Record gelesen

## Zuletzt bearbeitet

**Datum:** 2026-03-03
**Session:** Block 13.6 (CEO → Lead-Agent Delegation) komplett
**Status:** Backend ~530 Java-Dateien, V15 Migration. CEO delegiert an 5 Lead-Agents (Produktion, Maschinen, Lager, Personal, Support) via `delegate_to_lead` Tool. Leads haben eigene Tools und laufen als sync ReAct-Loop (LeadAgentRunner, max 5 Iterationen). 33 Tools total registriert. Delegation-Events im Frontend als eigene UI-Elemente.
**Nächste Blöcke:** Block 14 (Docker/Deployment)
