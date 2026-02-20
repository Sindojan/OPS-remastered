# Sindoflow OPS – Projektdokumentation

## Projekt

**Sindoflow OPS** – Agentenbasierte Operations-Plattform für eine Auto-Sitz-Manufaktur.
**Repo:** https://github.com/Sindojan/OPS-remastered
**GitHub Account:** Sindojan

## Tech Stack

| Layer | Technologie |
|-------|-------------|
| Frontend | Next.js 16, React 19, shadcn/ui, Tailwind CSS v4, TypeScript |
| Backend | Spring Boot 3, Java 21, Maven |
| Datenbank | PostgreSQL (Multi-Schema per Tenant) |
| Migrations | Flyway |
| Auth | JWT (stateless), Spring Security |
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
    └── src/main/java/com/sindoflow/ops/
        ├── common/                  # BaseEntity, TenantContext, Exceptions
        ├── config/                  # Security, JPA, Flyway, CORS
        ├── auth/                    # JWT, Login
        ├── tenant/                  # Tenant-Management, Schema-Provisionierung
        ├── production/              # Jobs, Stations, Scheduling
        ├── machines/                # Maschinen, Wartung
        ├── people/                  # Mitarbeiter, Zeiterfassung
        ├── inventory/               # Lager, Artikel, Bewegungen
        ├── bom/                     # Stücklisten, Arbeitspläne
        ├── inbox/                   # Support, Tickets
        ├── events/                  # Domain Events
        └── agentinfra/              # Agent-Infrastruktur, Tool Registry
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

### Block 3: Agent Setup & Backend ✅
| Task | Beschreibung | Status |
|------|-------------|--------|
| TASK-SETUP-001 | Claude Agent Struktur & Skill-Files | ✅ |
| TASK-BE-001 | Spring Boot Projektstruktur | ✅ |
| TASK-BE-002 | PostgreSQL Multi-Schema & Flyway | ✅ |

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

## Zuletzt bearbeitet

**Datum:** 2026-02-20
**Session:** Block 1-3 komplett
**Status:** Frontend läuft (Port 4201), Backend kompiliert (Port 8080, braucht PostgreSQL)
