# Owlsburg OPS - Projektfortschritt

> Agentenbasierte Operations-Plattform für eine Auto-Sitz-Manufaktur
> **Repo:** https://github.com/Sindojan/OPS-remastered
> **Stand:** 2026-03-03 | **51 Commits** | **~530 Java-Dateien** | **16 Flyway-Migrationen** | **33 Agent-Tools**

---

## Tech Stack

| Layer | Technologie |
|-------|-------------|
| Frontend | Next.js 16, React 19, shadcn/ui, Tailwind CSS v4, TypeScript |
| Backend | Spring Boot 3.5.0, Java 21, Maven |
| Datenbank | PostgreSQL 16 (Single-Schema mit Row Level Security) |
| Migrations | Flyway (programmatisch, flache Struktur V1-V16) |
| Auth | JWT (JJWT 0.12.6, stateless), Spring Security, Bucket4j Rate Limiting |
| Dateispeicher | MinIO (Dokumente, Binärdateien) |
| LLM | Anthropic Claude API (Opus 4.6, Sonnet 4.6, Haiku 4.5) |
| Infra | Docker Compose (PostgreSQL, MinIO, Monitoring) |

---

## Block 1: Frontend Foundation

**Datum:** 2026-02-20
**Commits:** `ea88b3a` .. `1b8722b`

| Task | Beschreibung | Details |
|------|-------------|---------|
| TASK-FE-001 | Next.js Projekt initialisieren | Next.js 16 + React 19 + shadcn/ui + Tailwind CSS v4 Setup |
| TASK-FE-002 | Drei-Bereich-Layout Shell | Sidebar (permanent dunkel) + Header + Content-Area mit Dot-Grid |
| TASK-FE-003 | Navigationsrouten & leere Pages | App Router Route Group `(app)/` mit Placeholder-Seiten |
| TASK-FE-004 | Design Tokens & globale Styles | oklch Farbraum (Teal/Cyan hue 195), DM Sans + JetBrains Mono, StatusBadge |

**Architektur-Entscheidungen:**
- "Industrial Precision" Design Direction (Kontrollraum-Aesthetik)
- App Router mit Route Groups: `(app)/` = auth-geschuetzt, `login/` = public
- Sidebar permanent dunkel in Light + Dark Mode

---

## Block 2: Shared Component Library

**Datum:** 2026-02-20
**Commits:** `6d268d0` .. `77c5247`

| Task | Beschreibung | Details |
|------|-------------|---------|
| TASK-FE-005 | KPI Card | Sparkline-Diagramm, Trend-Indikator (up/down/neutral), recharts Integration |
| TASK-FE-006 | DataTable\<T\> | Generische Tabelle mit Sortierung, Suche, Pagination, Row-Click |
| TASK-FE-007 | PageHeader, ConfirmationDialog, Skeletons | Wiederverwendbare Layout-Komponenten, Bestaetigungsdialog, Skeleton-Varianten |

---

## Block 3: Agent Setup & Backend Grundarchitektur

**Datum:** 2026-02-20
**Commits:** `54ab443` .. `6783d63`

| Task | Beschreibung | Details |
|------|-------------|---------|
| TASK-SETUP-001 | Claude Agent Struktur | 8 Agent Skill-Files in `.claude-agents/agents/` (backend-structure, db, auth, domain, api, event, agentfactory, frontend) |
| TASK-BE-001 | Spring Boot Projektstruktur | Maven-Projekt, Package-by-Domain Struktur, Java 21 |
| TASK-BE-002 | PostgreSQL & Flyway | Docker Compose fuer PostgreSQL + MinIO, Flyway programmatisch konfiguriert |

**Architektur-Entscheidungen:**
- Monorepo: `frontend/` und `backend/` als Unterordner
- Modularer Monolith (Package-by-Domain, kein Microservice-Split)
- Deterministisch: Kein LLM in Domaenenlogik

---

## Block 4+5: Auth, Tenant & vollstaendige Domaenenarchitektur

**Datum:** 2026-02-20
**Commits:** `a4f1499` .. `20afeb4`

| Task | Beschreibung | Details |
|------|-------------|---------|
| TASK-BE-003 | Auth System | JWT (Access 24h + Refresh 7d), Login/Refresh/Logout, User-CRUD, Tenant-Provisionierung, BCrypt Passwort-Hashing |
| TASK-BE-004 | DB-Schema | V2 Migration: Alle Domaenen-Tabellen mit `tenant_id UUID NOT NULL REFERENCES tenants(id)` + Indizes |
| TASK-BE-005 | MinIO Setup | Document Service mit Binaer-Storage in MinIO, Metadaten in PostgreSQL |
| TASK-BE-006a | Kunden | CRUD, Kontakte, Adressen, Preisgruppen |
| TASK-BE-006b | Produktion | Jobs mit Status-Maschine (CREATED->PLANNED->IN_PROGRESS->COMPLETED), Stationen, Schichten, QA |
| TASK-BE-006c | Maschinen | CRUD, Wartungsplaene, Stoerungen (MachineIncident) |
| TASK-BE-006d | Mitarbeiter | Clock-In/Out Zeiterfassung, MyDay-Endpoint, Abwesenheiten (PENDING->APPROVED/REJECTED) |
| TASK-BE-006e | Lager & Material | Artikel, Bestand, Bewegungen (IN/OUT/ADJUSTMENT), Lieferanten |
| TASK-BE-006f | Stuecklisten | BOM (Baumstruktur), Arbeitsplaene, Kalkulation (Soll/Ist-Vergleich) |
| TASK-BE-006g | Inbox & Support | Conversations (Threaded), Messages, Tags, Status-Management |
| TASK-BE-006h | Agent Infrastructure | Templates, Instances, Runs, Steps, Machine Incidents |
| Events | Domain Events | EventEntity, Scheduled Triggers, EventSubscriptions |

**Migrationen:** V1 (Tenants+Users), V2 (Full Schema), V3 (RLS Policies), V4 (Seed Agents)

**Architektur-Entscheidungen:**
- Single-Schema RLS (nicht Multi-Schema): Alle Tabellen in `public` mit `tenant_id`
- `TenantAwareDataSource` (DelegatingDataSource) setzt `set_config('app.current_tenant', ?)` pro Connection
- BaseEntity mit `@PrePersist` fuer automatische `tenant_id` Setzung
- Controller -> Service -> Repository (strikt, nie ueberspringen)

**Bekannte Learnings:**
- Lombok `@Slf4j`/`@RequiredArgsConstructor` funktioniert NICHT mit Maven Compiler -> explizite Logger + Konstruktoren
- `@Value` mit `List<String>` fuer comma-separated YAML funktioniert NICHT -> `String` + `.split(",")`

---

## Block 6: Agent Infrastructure & LLM-Integration

**Datum:** 2026-02-23
**Commits:** `7bdf7ea` .. `67821f2`

| Task | Beschreibung | Details |
|------|-------------|---------|
| TASK-BE-007 | LLM Provider Abstraktion | `LlmProvider` Interface, `AnthropicProvider` Implementation, AES-256-GCM Verschluesselung fuer API Keys |
| TASK-BE-008 | Tool Registry | `AgentToolRegistry` mit 13 initialen Domain-Tools, `AgentTool` Interface mit `getName()`, `getDescription()`, `getInputSchema()`, `execute()` |
| TASK-BE-009 | Agent Execution Engine | ReAct Loop (Reason-Act-Observe), Streaming via SSE, Budget-Tracking (Token-Verbrauch) |
| TASK-BE-010 | Seed Templates & Instances | CEO Agent + 7 Lead-Agents (Production, Machine, Supply, People, Support, Finance, Knowledge) |
| TASK-BE-011 | Event Routing & Scheduler | Domain Events -> Agent Subscriptions, Scheduled Triggers |
| TASK-FE-008 | LLM Settings UI | API Key Konfiguration (masked), Model-Auswahl, Agent Instances Tab |
| TASK-FIX-001 | CORS Fix & Bugfixes | CORS-Konfiguration, System-Test-Durchlauf |

**Architektur-Entscheidungen:**
- Tool Registry = einzige Schnittstelle zwischen Agent-Runtime und Domain-Services
- Kein Agent schreibt ausserhalb seines definierten Layers
- Kein direkter DB-Zugriff durch Agent-Layer
- AES-256-GCM fuer API Key Verschluesselung (Symmetric, Key aus Umgebungsvariable)

---

## Block 7: Frontend OPS-Views

**Datum:** 2026-02-23
**Commits:** `bc0b0f6` .. `5672775`

| Task | Beschreibung | Details |
|------|-------------|---------|
| Block-7-infra | Infrastruktur | Domain Types (`types/api.ts`), API Hooks (`useApi`, `usePagedApi`, `useMutation`), Format Utils, DomainStatusBadge |
| TASK-FE-009 | Production Views | Jobs-Liste, Job-Detail, Planner (Gantt-aehnlich), Stationen-Uebersicht |
| TASK-FE-010 | Machines Views | Maschinen-Uebersicht, Detail-Seite mit Wartung/Stoerungen Tabs |
| TASK-FE-011 | Inventory Views | Artikel-Liste, Artikel-Detail, Lieferanten, Bewegungs-Historie |
| TASK-FE-012 | People Views | Mitarbeiter-Liste, Detail, Zeiterfassung, My-Day Dashboard |
| TASK-FE-013 | Inbox | Split-Layout (Liste links, Chat rechts), Konversations-Management |
| TASK-FE-014 | Reports | KPI Dashboard mit recharts, CSV Export |

**Bekannte Learnings:**
- Backend gibt `Page<T>` zurueck -> Frontend MUSS `usePagedApi<T>` verwenden (nicht `useApi<T[]>`)
- Radix UI Select: `value=""` WIRFT Error -> immer `undefined` statt `""` verwenden

---

## Block 8: RLS Migration & Rebranding

**Datum:** 2026-02-23
**Commits:** `c783058` .. `edca221`

| Task | Beschreibung | Details |
|------|-------------|---------|
| FIX-BLOCK-A | Multi-Schema -> Single-Schema RLS | Komplette Migration von separaten Tenant-Schemas zu einem einzelnen Schema mit RLS Policies. Rebranding von "Sindoflow" zu "Owlsburg" |
| REVIEW-001 | System-Review | Vollstaendiger Review von DB, Auth, Domain, Code Quality, Frontend |
| FIX-BLOCK-B | Review-Fixes | CORS Fix, AuthService Extraktion, N+1 Query Fixes (JOIN FETCH), Overdue Flag auf Jobs |
| CORS-FIX | CORS Konfiguration | `@Value` comma-separated string Fix (String + split statt List) |

**Architektur-Aenderung:**
- **Vorher:** Multi-Schema (ein Schema pro Tenant) mit Schema-Switching
- **Nachher:** Single-Schema mit `tenant_id` auf jeder Tabelle + PostgreSQL RLS Policies
- `current_tenant_id()` SQL-Funktion liest `app.current_tenant` Setting

---

## Block 9: Login, Agent-Button, My-Day Dashboard

**Datum:** 2026-02-23
**Commit:** `8d99a8a`

| Task | Beschreibung | Details |
|------|-------------|---------|
| TASK-FE-015 | Login-Screen & Auth-Flow | Login-Formular, AuthContext Provider, Route Guard, User Dropdown mit Logout, localStorage fuer Token |
| TASK-BE-012 | Primary Agent Backend | V5+V6 Migrationen, `role_agent_defaults` Tabelle, PrimaryAgentService, `/me` Endpoint mit Agent-Info |
| TASK-FE-016 | Dynamischer Agent-Button | `usePrimaryAgent` Hook, rollenbasierter Agent-Button im Header |
| TASK-FE-017 | My-Day Dashboard | Rollenbasiertes Dashboard, Clock-In/Out Widget, Jobs/KPIs/Info-Cards je nach Rolle |
| TASK-FE-018 | Settings: Rollen-Agent-Zuweisung | Tab in Settings fuer ADMIN: Welche Rolle bekommt welchen Default-Agent |

**Migrationen:** V5 (role_agent_defaults + RLS), V6 (Seed: ADMIN/MANAGER->CEO, TEAM_LEAD->Production Lead, WORKER->People Lead)

---

## Block 10: System Admin & Employee Roles

**Datum:** 2026-02-23
**Commit:** `42b87e3`

| Task | Beschreibung | Details |
|------|-------------|---------|
| TASK-BE-013 | System Admin API | `/api/system/companies` CRUD, SYSTEM_ADMIN Rolle, V7 Migration mit Tenant-Extension (slug, plan, status, suspended_at) |
| TASK-FE-019 | System Admin UI | `(system)` Route Group mit eigenem SystemShell, Company-Liste, Company-Detail mit Tabs |
| Employee Roles | Rollen-Mapping | Funktionale Rolle (z.B. Monteur) -> System-Rolle (WORKER/TEAM_LEAD/MANAGER/ADMIN), Auto-User-Erstellung bei Mitarbeiter-Anlage |

**Migration:** V7 (Tenant-Extension: slug/plan/status, SYSTEM_ADMIN User Seed)

**SYSTEM_ADMIN Login:** philipp.ebert@strate-software / N0n3Xx.Blender (kein Tenant, bypassed RLS)

---

## Block 11: Kunden-Verwaltung & Parts/Processes

**Datum:** 2026-02-23
**Commit:** `41cd131`

| Task | Beschreibung | Details |
|------|-------------|---------|
| TASK-BE-014 | Kunden-Backend | V8 Migration (customer_number, short_name), CustomerEntity/DTO-Erweiterung, Calculation History Endpoint |
| TASK-FE-020 | Kunden-Views | Liste mit KPIs, Detail mit 5 Tabs (Uebersicht, Ansprechpartner, Adressen, Preisgruppen, Historie) |
| TASK-FE-021 | Parts & Processes | Teile-Liste, BOM Baumstruktur (rekursiv), Arbeitsplan-Editor, Kalkulations-Panel |

**Migration:** V8 (customer_number + short_name + Unique Index)

---

## FIX-BLOCK-D: UI Konsistenz & Deutsch-Lokalisierung

**Datum:** 2026-02-23
**Commit:** `a847882`

| Fix | Beschreibung | Details |
|-----|-------------|---------|
| FIX-1 | Tabellen-Toolbar | "Neuer Kunde" Button in DataTable-Toolbar, fehlende Breadcrumb-Routen ergaenzt |
| FIX-2 | Deutsche Lokalisierung | Komplette Lokalisierung: 31 Dateien, zentrale `lib/i18n.ts` mit 150+ Begriffen, `t()` Helper-Funktion |
| FIX-3 | Rollenauswahl | Mitarbeiter-Erstellung vereinfacht: direkte System-Rollen (WORKER, TEAM_LEAD, MANAGER, ADMIN) statt funktionale Rollen |

---

## Block 12: Knowledge & Dokumente

**Datum:** 2026-02-23
**Commit:** `65b1a2c`

| Task | Beschreibung | Details |
|------|-------------|---------|
| TASK-BE-015 | Knowledge Backend | V9 Migration (knowledge_categories, knowledge_articles, knowledge_tags), Full-Text Search, Document-Erweiterungen (preview, metadata update) |
| TASK-FE-022 | Knowledge Frontend | Knowledge Overview mit Kategorie-Filter, Markdown-Editor (MDEditor Komponente), Artikel-Detail, Dokument-Management mit Vorschau |
| FIX-AUTH | Auth-Fixes | AuthContext: `/me`-Validierung bei Start (stale Token nach DB-Reset erkennen), Radix Select empty-value Fix, LazyInitializationException Fix (`@Transactional` auf Entity-to-DTO Mapping) |

**Migration:** V9 (Knowledge-Tabellen + Document-Erweiterung + RLS Policies)

---

## FIX-BLOCK-E: Unicode-Encoding

**Datum:** 2026-02-23
**Commit:** `ccf7f5c`

Unicode-Escape-Sequences (`\u00F6` etc.) durch UTF-8 Zeichen ersetzt in 9 Dateien. Java-Source-Files enthalten jetzt direkt deutsche Umlaute.

---

## Block 12b: Settings vervollstaendigen

**Datum:** 2026-02-23
**Commits:** `f6f788f` .. `ea80f43`

| Task | Beschreibung | Details |
|------|-------------|---------|
| TASK-BE-016 | Settings Backend | V10 Migration (Tenant-Config-Felder, user_notification_settings), TenantConfig API, Budget API, Notification Settings, Available Tools Endpoint |
| TASK-FE-023 | Settings Frontend | 5 neue Tabs: Benutzer, Firma, Agents erweitert, Budget, Benachrichtigungen. Switch-Komponente, `use-settings` Hooks |
| FIX-LLM | LLM Config Fix | `TenantLlmConfigEntity` muss `BaseEntity` erweitern fuer `tenant_id`/RLS Kompatibilitaet |

**Migration:** V10 (Tenant-Config, Notification Settings, Budget-Felder)

---

## Block 12c: Docker & Deployment

**Datum:** 2026-02-24
**Commit:** `93f8fb6`

Docker Deployment Setup mit Monitoring Stack:
- `docker-compose.yml` fuer Produktion (PostgreSQL, MinIO, Backend, Frontend)
- Monitoring: Prometheus, Grafana
- Deployment-Scripts

---

## Block 13: Agent Console (Chat)

**Datum:** 2026-02-26
**Commits:** `f7632ab` .. `6491189`

### Block 13.1: SSE Streaming POC
| Task | Details |
|------|---------|
| TASK-BE-019 | `SimpleChatService` mit `SseEmitter` + `Thread.startVirtualThread()`. `POST /api/chat/message` Endpoint. Anthropic API Streaming Integration (content_block_delta Events) |
| TASK-FE-026 | Agent Panel Komponente: SSE EventSource, Blinking Cursor waehrend Streaming, Auto-Scroll |

### Block 13.2: Markdown-Rendering
| Task | Details |
|------|---------|
| TASK-FE-027 | react-markdown + remark-gfm + rehype-highlight. Code-Bloecke mit Syntax-Highlighting, Tabellen, Listen |

### Block 13.3+13.4: Chat Persistence & CEO Prompt
| Task | Details |
|------|---------|
| TASK-BE-020 | V11 Migration (chat_sessions, chat_messages mit RLS). Session CRUD, History Loading, Greeting-Message |
| TASK-BE-021 | V12 Migration: CEO System-Prompt mit `{{TENANT_NAME}}` Placeholder, detaillierte Rollenbeschreibung |
| TASK-FE-028 | Session-Liste (Sidebar), Auto-Load letzte Session, Session-Wechsel, Loeschung |
| FIX-CHAT | **Kritischer Fix:** TenantContext (ThreadLocal) wird NICHT in Virtual Threads propagiert -> manuelles Capture/Set/Clear. Greeting persistieren. LLM-Model im SSE Header |

### Block 13.3.1: Per-Instance System Prompt
| Task | Details |
|------|---------|
| TASK-BE-022 | V13 Migration (`custom_system_prompt TEXT` auf agent_instances), PATCH Endpoint, Prompt-Editor in Settings. NULL = Template-Fallback |

**Migrationen:** V11 (chat_sessions/messages), V12 (CEO Prompt), V13 (custom_system_prompt)

**Kritische Learnings:**
- ThreadLocal (TenantContext, SecurityContext) wird NICHT automatisch in Virtual Threads propagiert
- Muss manuell captured und im Virtual Thread gesetzt/gecleared werden
- SSE Streaming: `SseEmitter` mit Event-Types (content, toolCall, toolResult, delegation, etc.)

---

## FIX-BLOCK-G: Agent Tenant-Isolation Security

**Datum:** 2026-02-26
**Commit:** `9e05204`

| Task | Beschreibung | Details |
|------|-------------|---------|
| TASK-SEC-001 | Tenant-Isolation | `findByIdAndTenantId` auf ALLEN Agent/Chat Repositories. `findByIdSecure()` im Service fuer Controller (Defense-in-Depth ueber RLS hinaus). `AccessDeniedException` -> HTTP 403 "Zugriff verweigert" (nie 404, kein Information Leakage). PATCH Template Endpoint mit Tenant-Validierung |

**Sicherheitsprinzip:** Doppelte Isolation:
1. PostgreSQL RLS Policies (Datenbank-Level)
2. `findByIdAndTenantId` im Service (Applikations-Level)

---

## Block 13.5: CEO Tool-Calling

**Datum:** 2026-03-02
**Commit:** `95ebe1d`

| Task | Beschreibung | Details |
|------|-------------|---------|
| TASK-BE-023 | ReAct-Loop mit Streaming | ReAct-Loop direkt in `SimpleChatService` (Streaming + Tool-Calling). Tool-Definitionen aus Template's `allowedTools` geladen. Max 10 Iterationen |
| 10 Chat-Tools | CEO-Werkzeuge | `get_jobs`, `get_job_detail`, `update_job_status`, `get_machines`, `get_machine_detail`, `get_critical_stock`, `get_stock_level`, `get_attendance_today`, `get_absences`, `get_customer_orders` |
| TASK-FE-029 | Tool-Call UI | SSE Events `toolCall`/`toolResult` im Agent-Panel als Info-Cards angezeigt |

**Migration:** V14 (CEO Prompt Tool-Section, allowed_tools fuer 10 Tools)

**Architektur:**
- CEO hat ReAct-Loop: LLM entscheidet ob Tool-Call oder Text-Antwort
- Tool wird ausgefuehrt, Ergebnis als `tool_result` zurueck an LLM
- Max 10 Iterationen, dann Abbruch
- SSE Events: `content` (Text-Chunks), `toolCall` (Tool-Aufruf), `toolResult` (Tool-Ergebnis), `done` (Ende)

---

## Block 13.6: CEO -> Lead-Agent Delegation

**Datum:** 2026-03-03
**Commit:** `81363de`

| Task | Beschreibung | Details |
|------|-------------|---------|
| TASK-BE-024 | LeadAgentRunner | Synchroner (nicht-streamender) ReAct-Loop fuer Lead-Agents. Max 5 Iterationen. Kein AgentRun-Management, kein Budget-Tracking. `@Lazy AgentToolRegistry` gegen zirkulaere DI-Abhaengigkeit |
| DelegateToLeadTool | CEO-Tool | Input: `{lead, task, priority?}`. Mapping: produktions_lead->production_lead, maschinen_lead->machine_lead, etc. Laedt Lead-Template + Instance, ruft `LeadAgentRunner.runLead()` (blocking) |
| 9 neue Lead-Tools | Spezialisierte Tools | `get_production_kpis`, `report_incident`, `schedule_maintenance`, `get_stock_movements`, `create_reorder`, `get_employee_detail`, `approve_absence`, `get_inbox_messages`, `create_inbox_reply` |
| ReorderRequest | Neues Domain-Feature | `ReorderRequestEntity`, `ReorderRequestRepository`, `ReorderRequestService` im inventory-Package. Status: OPEN->ORDERED->RECEIVED/CANCELLED |
| TASK-BE-025 | V15 Migration | `reorder_requests` Tabelle + RLS, CEO auf 2 Tools reduziert (`delegate_to_lead`, `get_kpi_summary`), Lead-Tool-Zuweisungen, Lead-Instance System-Prompts |
| TASK-FE-030 | Delegation UI | SSE Events `delegation`/`delegationResult` im Agent-Panel (zeigt "Delegiere an **produktions_lead**..." und Ergebnis) |

**Migration:** V15 (reorder_requests, CEO Tool-Reduktion, Lead-Tool-Zuweisungen, Lead System-Prompts)

**Architektur:**
- CEO hat nur 2 Tools: `delegate_to_lead` + `get_kpi_summary`
- CEO delegiert an 5 Leads: Production, Machine, Supply, People, Support
- Jeder Lead hat eigenen System-Prompt, eigene Tools, eigenen sync ReAct-Loop
- Keine Rekursion moeglich: Lead-Templates haben kein `delegate_to_lead`
- Lead-Agent-Antwort wird als Tool-Result an CEO zurueck gegeben

**Tool-Verteilung nach Delegation:**

| Agent | Tools |
|-------|-------|
| CEO | `delegate_to_lead`, `get_kpi_summary` |
| Production Lead | `get_jobs`, `get_job_detail`, `update_job_status`, `get_stations`, `get_production_kpis` |
| Machine Lead | `get_machines`, `get_machine_detail`, `get_maintenance_due`, `report_incident`, `schedule_maintenance` |
| Supply Lead | `get_critical_stock`, `get_stock_level`, `get_stock_movements`, `create_reorder` |
| People Lead | `get_attendance_today`, `get_absences`, `get_employee_detail`, `approve_absence` |
| Support Lead | `get_customer_orders`, `get_open_conversations`, `get_conversation_detail`, `get_inbox_messages`, `create_inbox_reply` |

---

## FIX-BLOCK-H: LLM Model Normalisierung

**Datum:** 2026-03-03
**Commit:** `81363de`

| Task | Beschreibung | Details |
|------|-------------|---------|
| TASK-BE-026 | Model Normalisierung | V16 Migration: CEO->claude-opus-4-6, Leads->claude-sonnet-4-6, Catch-All->claude-sonnet-4-6. Per-Instance `resolveModel()` in SimpleChatService + LeadAgentRunner (config JSONB -> tenant default -> fallback) |
| Backend-Validierung | VALID_MODELS Set | `Set.of("claude-opus-4-6", "claude-sonnet-4-6", "claude-haiku-4-5-20251001")` im Controller. PATCH `/api/agent-instances/{id}/model` validiert gegen diese Liste |
| TASK-FE-031 | Fixed Model Dropdown | 3 fixe Optionen (Opus 4.6, Sonnet 4.6, Haiku 4.5) statt dynamischer API-Abfrage. Friendly Names statt Raw-IDs |

**Migration:** V16 (Model-Normalisierung in config JSONB)

**Model Resolution Chain:**
1. Instance `config` JSONB -> `model` Feld
2. Tenant-Level `TenantLlmConfig` -> `defaultModel`
3. Hardcoded Fallback: `claude-sonnet-4-6`

**Ergebnis:**
```
CEO Agent       | claude-opus-4-6
Production Lead | claude-sonnet-4-6
Machine Lead    | claude-sonnet-4-6
Supply Lead     | claude-sonnet-4-6
People Lead     | claude-sonnet-4-6
Support Lead    | claude-sonnet-4-6
Finance Lead    | claude-sonnet-4-6
Knowledge Lead  | claude-sonnet-4-6
```

---

## Flyway-Migrationen (Uebersicht)

| Version | Name | Inhalt |
|---------|------|--------|
| V1 | init_public | Tenants, Users (mit tenant_id FK), Refresh Token Blacklist, Default Tenant + Admin |
| V2 | full_schema | Alle Domaenen-Tabellen mit tenant_id + Indizes |
| V3 | rls_policies | `current_tenant_id()` Funktion + RLS Policies fuer alle Tabellen |
| V4 | seed_agents | Agent Templates (CEO + 7 Leads), Instances, Event Subscriptions, Scheduled Triggers |
| V5 | primary_agent | `role_agent_defaults` Tabelle + RLS + `users.primary_agent_instance_id` |
| V6 | seed_role_defaults | ADMIN/MANAGER->CEO, TEAM_LEAD->Production Lead, WORKER->People Lead |
| V7 | system_admin | Tenant-Extension (slug/plan/status), SYSTEM_ADMIN User Seed |
| V8 | customer_fields | customer_number + short_name auf customers Tabelle |
| V9 | knowledge | Knowledge-Tabellen (categories, articles, tags), Document-Erweiterung |
| V10 | tenant_config | Tenant-Config-Felder, user_notification_settings, Budget-Felder |
| V11 | chat_sessions | chat_sessions + chat_messages Tabellen mit RLS |
| V12 | ceo_system_prompt | CEO System-Prompt mit `{{TENANT_NAME}}` |
| V13 | instance_system_prompt | `custom_system_prompt TEXT` auf agent_instances |
| V14 | ceo_tools_prompt | CEO Tool-Section, allowed_tools fuer 10 Tools |
| V15 | lead_delegation | reorder_requests, CEO auf 2 Tools, Lead-Tool-Zuweisungen, Lead-Prompts |
| V16 | model_normalization | Model-Strings in config JSONB normalisiert |

---

## API-Endpunkte

| Bereich | Basis-Pfad | Auth |
|---------|-----------|------|
| Auth | `/api/auth/**` | public |
| Users | `/api/users` | ADMIN/MANAGER |
| User Me | `/api/users/me` | authenticated |
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
| Knowledge | `/api/knowledge/**` | authenticated |
| Conversations | `/api/conversations` | authenticated |
| Agent Templates | `/api/agent-templates` | authenticated |
| Agent Instances | `/api/agent-instances` | authenticated |
| Agent Runs | `/api/agent-runs` | authenticated |
| LLM Config | `/api/settings/llm` | authenticated |
| Role-Agent Defaults | `/api/settings/role-agent-defaults` | ADMIN/MANAGER |
| Events | `/api/events` | authenticated |
| Chat | `/api/chat` | authenticated |
| Triggers | `/api/scheduled-triggers` | authenticated |
| Health | `/actuator/health` | public |

---

## Rollen & Zugaenge

| Rolle | Email | Passwort | Beschreibung |
|-------|-------|----------|-------------|
| ADMIN | software@sindojan.de | root1234 | Default Admin (V1 Migration) |
| SYSTEM_ADMIN | philipp.ebert@strate-software | N0n3Xx.Blender | System Admin (V7 Migration, kein Tenant) |

**System-Rollen:** `SYSTEM_ADMIN`, `ADMIN`, `MANAGER`, `TEAM_LEAD`, `WORKER`, `AGENT_SYSTEM`

---

## Commit-Historie (chronologisch)

| # | Hash | Datum | Beschreibung |
|---|------|-------|-------------|
| 1 | `483b99b` | 2026-02-20 | Initial commit from Create Next App |
| 2 | `ea88b3a` | 2026-02-20 | feat(TASK-FE-001): initialize Next.js project with shadcn/ui |
| 3 | `bc8e4e7` | 2026-02-20 | feat(TASK-FE-002): implement three-panel layout shell |
| 4 | `33f17c6` | 2026-02-20 | feat(TASK-FE-003): add navigation routes and placeholder pages |
| 5 | `1b8722b` | 2026-02-20 | feat(TASK-FE-004): add design tokens, theme system, and StatusBadge |
| 6 | `6d268d0` | 2026-02-20 | feat(TASK-FE-005): add KPI Card component with sparkline |
| 7 | `2fc8f8e` | 2026-02-20 | feat(TASK-FE-006): add generic DataTable\<T\> component |
| 8 | `77c5247` | 2026-02-20 | feat(TASK-FE-007): add PageHeader, ConfirmationDialog, skeletons |
| 9 | `d11e7bc` | 2026-02-20 | refactor: move git repo to monorepo root |
| 10 | `54ab443` | 2026-02-20 | feat(TASK-SETUP-001): add Claude agent structure and skill files |
| 11 | `6783d63` | 2026-02-20 | feat(TASK-BE-001): initialize Spring Boot project structure |
| 12 | `2e8e828` | 2026-02-20 | docs: update CLAUDE.md with agent workflow instructions |
| 13 | `a4f1499` | 2026-02-20 | feat(TASK-BE-003): implement auth, JWT, user-CRUD, tenant provisioning |
| 14 | `73b3152` | 2026-02-20 | feat(TASK-BE-004): add complete database schema migration |
| 15 | `1f00841` | 2026-02-20 | feat(TASK-BE-005,BE-006): implement MinIO, documents, and all domain services |
| 16 | `da54ba8` | 2026-02-20 | docs: update CLAUDE.md with Block 4+5 completion |
| 17 | `20afeb4` | 2026-02-20 | chore: add docker-compose for local dev |
| 18 | `7bdf7ea` | 2026-02-23 | feat(Block-6): implement agent infrastructure with LLM integration |
| 19 | `67821f2` | 2026-02-23 | fix(TASK-FIX-001): CORS fix, LLM settings UI, bugfixes |
| 20 | `e84413a` | 2026-02-23 | docs: update CLAUDE.md with Block 6 completion |
| 21 | `bc0b0f6` | 2026-02-23 | feat(Block-7-infra): add domain types, API hooks, format utils |
| 22 | `7db1d18` | 2026-02-23 | feat(FE-009,FE-010): implement Production and Machines views |
| 23 | `c5df042` | 2026-02-23 | feat(FE-011,FE-012): implement Inventory and People views |
| 24 | `5672775` | 2026-02-23 | feat(FE-013,FE-014): implement Inbox and Reports views |
| 25 | `88b4e43` | 2026-02-23 | docs: update CLAUDE.md with Block 7 completion |
| 26 | `8dfb640` | 2026-02-23 | fix: handle paginated API responses in frontend hooks |
| 27 | `da76bdc` | 2026-02-23 | fix: add toast feedback, fix CRUD operations |
| 28 | `97e312e` | 2026-02-23 | fix(frontend): machine creation, people forms, chart colors |
| 29 | `c783058` | 2026-02-23 | refactor: migrate to single-schema RLS, rebrand to Owlsburg |
| 30 | `25a5fa1` | 2026-02-23 | fix: address review findings (CORS, N+1, auth, overdue) |
| 31 | `edca221` | 2026-02-23 | fix: comma-separated string for CORS config |
| 32 | `8d99a8a` | 2026-02-23 | feat: login, auth flow, agent button, my-day, role-agent settings |
| 33 | `42b87e3` | 2026-02-23 | feat: system admin company management, employee roles |
| 34 | `653d7b2` | 2026-02-23 | fix: restrict navigation by role, add route guards |
| 35 | `41cd131` | 2026-02-23 | feat: customer management and parts & processes views |
| 36 | `a847882` | 2026-02-23 | fix(frontend): complete German localization, simplify roles |
| 37 | `65b1a2c` | 2026-02-23 | feat: knowledge base with articles, documents, categories |
| 38 | `eb51367` | 2026-02-23 | docs: update CLAUDE.md with Knowledge completion |
| 39 | `ccf7f5c` | 2026-02-23 | fix(frontend): replace Unicode escapes with UTF-8 |
| 40 | `f6f788f` | 2026-02-23 | feat: settings (users, company, agents, budget, notifications) |
| 41 | `ea80f43` | 2026-02-23 | fix: TenantLlmConfigEntity extend BaseEntity for RLS |
| 42 | `93f8fb6` | 2026-02-24 | feat(infra): Docker deployment, monitoring stack |
| 43 | `f7632ab` | 2026-02-26 | feat(chat): simple chat POC with SSE streaming |
| 44 | `ad5b386` | 2026-02-26 | feat(chat): Markdown rendering for agent messages |
| 45 | `228f3ba` | 2026-02-26 | feat(chat): persistent sessions and CEO system prompt |
| 46 | `b4fe23e` | 2026-02-26 | fix(chat): TenantContext propagation, greeting, LLM model |
| 47 | `6491189` | 2026-02-26 | feat(agents): per-instance system prompt editor |
| 48 | `9e05204` | 2026-02-26 | sec(agents): tenant isolation for agent/chat endpoints |
| 49 | `e818662` | 2026-02-27 | docs: update CLAUDE.md with Block 13, Security Fix |
| 50 | `95ebe1d` | 2026-03-02 | feat(agents): CEO tool-calling with ReAct loop |
| 51 | `81363de` | 2026-03-03 | feat(agents): CEO->Lead delegation + model normalization |
| 52 | `903abd9` | 2026-03-03 | docs: update CLAUDE.md with Block 13.6, FIX-BLOCK-H |

---

## Naechste geplante Schritte

- **Block 14:** Docker/Deployment (Produktion-ready Containerisierung)
