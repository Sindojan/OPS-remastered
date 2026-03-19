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
        ├── common/                  # BaseEntity, TenantAwareBaseEntity, TenantContext, Exceptions, Module-System
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
            ├── runtime/             # OOP Agent-System (Agent sealed interface, Factory, Records)
            ├── memory/              # Agent Memory (Entity, Repository, Service, Pruning)
            ├── messaging/           # Agent Message Bus (Entity, Repository, Bus, Level)
            ├── tools/               # Tool Registry + 40 Tools (13 Domain + 20 CEO/Lead + 7 Agent-Infra)
            │   └── impl/
            ├── execution/           # Orchestrator, SystemPromptBuilder
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
| TASK-BE-024 | LeadAgentRunner (sync ReAct-Loop für Leads), DelegateToLeadTool, 9 neue Lead-Tools (Produktion, Maschinen, Lager, Personal, Support), ReorderRequest Entity/Service/Repo | `81363de` |
| TASK-BE-025 | V15 Migration (reorder_requests, CEO auf 2 Tools, Lead-Tool-Zuweisungen, Lead-Instance System-Prompts) | `81363de` |
| TASK-FE-030 | Delegation SSE-Events im Agent-Panel (delegation/delegationResult statt toolCall/toolResult) | `81363de` |

### FIX-BLOCK-H: LLM Model Normalisierung ✅
| Task | Beschreibung | Commit |
|------|-------------|--------|
| TASK-BE-026 | V16 Migration (Model-Normalisierung: CEO→opus, Leads→sonnet), Per-Instance Model Resolution in SimpleChatService + LeadAgentRunner, Backend-Validierung (VALID_MODELS Set) | `81363de` |
| TASK-FE-031 | Fixed 3-Option Model Dropdown (Opus 4.6, Sonnet 4.6, Haiku 4.5), keine API-Abfrage mehr | `81363de` |

### Block 14: Agent-System Redesign (OOP) ✅
| Task | Beschreibung | Commit |
|------|-------------|--------|
| TASK-BE-027 | Sealed Agent Interface (CeoAgent, LeadAgent, SubAgent), AgentFactory, Records (AgentIdentity, AgentCapabilities, AgentContext, AgentResult) | `d9e39c0` |
| TASK-BE-028 | Memory-System: V17 Migration (agent_memories + RLS), AgentMemoryService (Upsert, LRU-Eviction, Pruning, Promotion), 3 Memory-Tools (save_memory, recall_memory, read_agent_memory) | `d9e39c0` |
| TASK-BE-029 | Message Bus: V17 Migration (agent_messages + RLS), AgentMessageBus (Hierarchie-Enforcement), 3 Message-Tools (send_message, report_to_ceo, check_messages) | `d9e39c0` |
| TASK-BE-030 | Sub-Agent Lifecycle: SpawnSubAgentTool, SubAgentCleanupScheduler (TTL 1h), MemoryPruningScheduler (täglich 3 Uhr), Memory-Promotion (importance ≥ 7) | `d9e39c0` |
| REFACTOR-1 | SimpleChatService refactored auf AgentFactory/CeoAgent, DelegateToLeadTool auf AgentFactory, LeadAgentRunner gelöscht | `d9e39c0` |
| CLEANUP-1 | CeoAgent: HttpClient static shared, InputStream Leak Fix, silenced Exceptions → debug logs. AgentMemoryService: read-only Transaction Fix. Model-Name Normalisierung auf `claude-sonnet-4-6` | `d9e39c0` |

### Block 15: Lead-Transparenz & Parallele Delegation ✅
| Task | Beschreibung | Commit |
|------|-------------|--------|
| TASK-BE-031 | Lead-Transparenz und parallele Delegation | `f019897` |

### Block 16: Per-Tenant Feature-Module ✅
| Task | Beschreibung | Commit |
|------|-------------|--------|
| TASK-BE-032 | V18 Migration (modules + tenant_modules), ModuleEntity, TenantModuleEntity, ModuleRepository, TenantModuleRepository | `8b53b92` |
| TASK-BE-033 | ModuleService (Caching, Toggle, Eviction), ModuleController (GET/PUT), ModuleResponse DTO | `8b53b92` |
| TASK-BE-034 | AgentTool.getModuleId() Default + Override in 33 Domain-Tools, AgentToolRegistry Modul-Filter | `8b53b92` |
| TASK-BE-035 | ModuleAccessInterceptor (API-Pfad→Modul Mapping, 403 bei deaktiviert), WebMvcConfig | `8b53b92` |
| TASK-BE-036 | GetKpiSummaryTool + DelegateToLeadTool modul-aware, MeResponse + UserController erweitert | `8b53b92` |
| TASK-FE-032 | Frontend Modul-Awareness: enabledModules in AuthContext, Sidebar-Filter, Route-Guard, Settings Modules-Tab | `8b53b92` |

### Block 17: Systemverwaltung & Token-Tracking ✅
| Task | Beschreibung | Commit |
|------|-------------|--------|
| TASK-BE-037 | CeoAgent: Token-Parsing im Streaming (message_start/message_delta), StreamResult + ChatResult Records | - |
| TASK-BE-038 | SimpleChatService: AgentRun-Erstellung pro Chat, Kosten-Berechnung (Opus/Sonnet/Haiku), Usage SSE-Event | - |
| TASK-BE-039 | AgentRunRepository: 4 native Queries für Tenant-Aggregation (countByTenantSince, sumTokens, sumCost, lastActive) | - |
| TASK-BE-040 | SystemCompanyService: Echte Stats (totalTokens30d, totalCostUsd30d), CompanyStatsResponse erweitert | - |
| TASK-BE-041 | SystemCompanyController: +Budget, +Agents (GET/PATCH), +LLM-Config (GET/PUT) Endpoints | - |
| TASK-FE-033 | Agent-Panel: Usage SSE-Event parsen, Token-Anzeige pro Nachricht + Session-Total im Header | - |
| TASK-FE-034 | Firmendetail: 7 Tabs (Übersicht, Statistiken, Admins, Module, Token-Verbrauch, Agenten, LLM) | - |

### Block 18: Agent-Optimierung (Activity Status, Last Run, Reliability) ✅
| Task | Beschreibung | Commit |
|------|-------------|--------|
| TASK-BE-042 | V19 Migration (activity_status, last_run_id, activity_status_changed_at auf agent_instances) | - |
| TASK-BE-043 | AgentActivityStatus Enum (IDLE/BUSY/ERROR), Entity-Felder, Service-Methoden (updateActivityStatus, linkLastRun) | - |
| TASK-BE-044 | AgentInstanceResponse + AgentInstanceActivity + AgentInstanceDetailResponse: +activityStatus Felder | - |
| TASK-BE-045 | Last-Run Endpoint: GET /api/agent-instances/{id}/last-run → AgentRunResponse mit Steps | - |
| TASK-BE-046 | SimpleChatService: BUSY/IDLE/ERROR Lifecycle, Incident-Reporting, Finally-Block für verwaiste Runs | - |
| TASK-BE-047 | AgentExecutionService: BUSY/IDLE/ERROR Lifecycle, Incident-Reporting | - |
| TASK-BE-048 | CeoAgent Reliability: Delegation-Timeout-Interrupt, Tool-Input-Parse-Warning, SSE-Alive-Check (trySend) | - |
| TASK-BE-049 | BudgetExceededException + startRunWithBudgetCheck() atomisch in @Transactional | - |
| TASK-FE-035 | Types: AgentActivityStatus, AgentRunDetail, AgentRunStep, erweiterte Interfaces | - |
| TASK-FE-036 | Agent Settings: Activity-Badge (IDLE=grün, BUSY=pulsierend, ERROR=rot) neben Admin-Status | - |
| TASK-FE-037 | Agent Panel: "Letzter Run" Button mit Steps-Anzeige (Collapsible Section) | - |
| TASK-FE-038 | System Admin Firmendetail: Activity-Status Badge pro Agent im Agenten-Tab | - |

### Block 19: Docker-Hardening, My-Day Dashboard-Fixes, Backend-Tests ✅
| Task | Beschreibung | Commit |
|------|-------------|--------|
| TASK-DEV-001 | Docker-Hardening, My-Day Dashboard-Fixes und Backend-Tests | `09d1e9a` |

### Block 20: Memory-System (4-Typ) ✅
| Task | Beschreibung | Commit |
|------|-------------|--------|
| TASK-BE-050 | V20 Migration (memory_type, run_id, confidence auf agent_memories), RunMemory, Episodic Extraction, Procedural Tracking | `0feb580` |

### Zwischenblock: Per-Tenant Agent & LLM-Konfiguration ✅
| Task | Beschreibung | Commit |
|------|-------------|--------|
| TASK-DB-021 | V21 Migration (settings JSONB auf tenant_llm_config, allowed_tools_override auf agent_instances) | `8d8fb69` |
| TASK-BE-051 | TenantLlmConfigEntity + AgentInstanceEntity erweitert, LlmRequest + AgentCapabilities + Temperature Support | `8d8fb69` |
| TASK-BE-052 | AgentFactory Instance-Level Overrides (Tools, Budget, Temperature), AgentToolRegistry erweitert | `8d8fb69` |
| TASK-BE-053 | SystemCompanyController: Erweiterte Agent/LLM-Endpoints, DTOs (AgentSystemSummaryResponse, ToolInfoResponse) | `8d8fb69` |
| TASK-FE-039 | LLM-Tab (Temperature Slider, MaxTokens, Modell-Auswahl), Agents-Tab (Toggle, Tool-Override, Budget, Prompt-Dialog) | `8d8fb69` |

### FIX-BLOCK-I: Chat-Bugfixes & Agent-PATCH ✅
| Task | Beschreibung | Commit |
|------|-------------|--------|
| FIX-TENANT | AgentRunEntity: Fehlendes tenant_id Feld + @PrePersist (NOT NULL Constraint Violation) | - |
| FIX-JSONB | AgentRunService: ensureJson() für Plain-Text → JSONB Escaping (newlines, tabs, quotes) | - |
| FIX-SSE | SimpleChatService: Try-catch um SSE done/usage Events (Client-Disconnect Robustheit) | - |
| FIX-PATCH | SystemCompanyController + AgentInstanceService: Explizites save() nach PATCH-Änderungen | - |

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
| Modules | `/api/modules` | authenticated (Toggle: ADMIN/MANAGER) |
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
| V16__model_normalization.sql | Model-Normalisierung in agent_instances config JSONB: CEO→opus, Leads→sonnet, Catch-All→sonnet |
| V17__agent_communication.sql | agent_memories + agent_messages Tabellen mit RLS, spawned_by_run_id, CEO + Lead Tool-Zuweisungen aktualisiert (40 Tools) |
| V18__tenant_modules.sql | modules + tenant_modules Tabellen, RLS Policy, Seed 9 Module, Backward-Compat für bestehende Tenants |
| V19__agent_activity_status.sql | activity_status (IDLE/BUSY/ERROR), last_run_id, activity_status_changed_at auf agent_instances |
| V20__agent_memory_types.sql | memory_type, run_id, confidence Felder auf agent_memories, 4-Typ Memory-System |
| V21__agent_llm_enhancements.sql | settings JSONB auf tenant_llm_config, allowed_tools_override JSONB auf agent_instances |
| V22__scale_indexes.sql | Performance-Indizes für Agent-Tabellen (Runs, Messages, Memory) |
| V23__system_agents.sql | System-Level Agent-Infrastruktur (Templates, Instances, Runs, Steps, Chat – ohne tenant_id/RLS) |
| V24__fix_system_admin_email.sql | System-Admin E-Mail Fix (`.com` Endung fehlte in V7) |
| V25__drop_custom_enum_types.sql | PostgreSQL Custom-ENUM-Typen (severity_level, incident_type etc.) durch VARCHAR ersetzen |
| V26__fix_severity_columns_and_cleanup.sql | severity-Spalten re-add (von V25 CASCADE gedroppt), TIMESTAMPTZ Konvertierung |

## Default Admin

- **Email:** software@sindojan.de
- **Passwort:** root1234
- **Rolle:** ADMIN
- Wird in V1 Migration angelegt (Default Tenant + Admin User)

## System Admin

- **Email:** philipp.ebert@strate-software.com
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

## Per-Tenant Feature-Module

9 Module (core immer aktiv, 8 togglebar pro Tenant):

| Modul-ID | Label | Domain-Package | Frontend-Routes | Lead-Agent |
|----------|-------|---------------|-----------------|------------|
| `core` | Kern | auth, tenant, common, agentinfra, events, documents | /my-day, /agents, /settings, /reports | ceo |
| `production` | Produktion | production | /production | production_lead |
| `machines` | Maschinen | machines | /machines | machine_lead |
| `inventory` | Lager & Material | inventory | /inventory | supply_lead |
| `people` | Personal | people | /employees | people_lead |
| `customers` | Kunden | customers | /customers | — |
| `inbox` | Posteingang | inbox | /inbox | support_lead |
| `bom` | Teile & Prozesse | bom | /parts-and-processes | — |
| `knowledge` | Wissensdatenbank | knowledge | /knowledge | — |

Deaktivierung: Kein Sidebar-Eintrag, kein API-Zugriff (403), keine Agent-Tools. Daten bleiben intakt.

## Zuletzt bearbeitet

**Datum:** 2026-03-19
**Session:** System-Review & Stabilisierung
**Status:** Backend ~580 Java-Dateien, V26 Migration. Vollständiger System-Review mit 6 spezialisierten Agents (89 Findings). ~25 CRITICAL/HIGH Bugs gefixt. Design Guide erstellt.
**Uncommitted:** 35 Dateien (System-Review Fixes + V26 Migration + DESIGN_GUIDE.md) – noch nicht committed.
**Nächste Blöcke:** Commit der Review-Fixes, dann neues Feature (TBD)

### Deployment-Vorbereitung ✅
| Task | Beschreibung | Commit |
|------|-------------|--------|
| BUG-1 | V24 Migration: System-Admin E-Mail Fix (.com Endung) | `f56ad38` |
| BUG-2 | Docker API_URL Default Fix (ohne /api) | `f56ad38` |
| BUG-3 | Nginx SSE-Config für /api/system/chat/ | `f56ad38` |
| BUG-4 | TLS/HTTPS: docker-compose.prod.yml + owlsburg-ssl.conf | `f56ad38` |
| DOCS | DEPLOYMENT.md erweitert (Produktion, TLS, Troubleshooting) | `f56ad38` |

### Deployment-Bugfixes ✅
| Task | Beschreibung | Commit |
|------|-------------|--------|
| FIX-ENV | System-Admin Credentials + LLM API-Key via .env (SystemAdminInitializer) | `4a0e0cc` |
| FIX-JSONB | @JdbcTypeCode(SqlTypes.JSON) auf alle 27 JSONB-Felder (13 Entities) | `0ac2fa3` |
| FIX-CHAT | Chat API-URL Fix, SYSTEM_ADMIN aus Userliste, Modules-Tab entfernt | `c38beda` |
| FIX-ENUM | V25: PostgreSQL Custom-ENUM-Typen durch VARCHAR ersetzt | `ed088b5` |

### System-Review & Stabilisierung (uncommitted)
| Task | Beschreibung |
|------|-------------|
| SEC-FIX | @PreAuthorize auf AgentInstance/Template/Run/Absence/TenantConfig Controller |
| SEC-FIX | JWT/Encryption Secrets: Defaults entfernt, nur in application-dev.yml |
| SEC-FIX | DocumentController: uploadedBy aus SecurityContext statt Request-Param |
| SEC-FIX | UserController: Tenant-Check auf getById |
| BUG-FIX | Employee Deactivate: status: "INACTIVE" fehlte im Request |
| BUG-FIX | SSE Error: Keine rohen Exceptions mehr an Client |
| BUG-FIX | CeoAgent: SseEmitter Leak Fix (finally-Block) |
| BUG-FIX | AgentExecutionService: BUSY-Recovery mit try-finally |
| BUG-FIX | Model-IDs normalisiert (claude-sonnet-4-6 statt alte IDs) |
| DATA-FIX | V26 Migration: severity-Spalten re-add, TIMESTAMPTZ Konvertierung |
| FE-FIX | Umlaut-Fixes in 12+ Frontend-Dateien |
| DOCS | DESIGN_GUIDE.md erstellt (Farben, Typografie, Komponenten) |
