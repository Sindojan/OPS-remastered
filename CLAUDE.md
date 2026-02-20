# Sindoflow OPS – Projektdokumentation

## Projekt

**Sindoflow OPS** – Agentenbasierte Operations-Plattform für eine Auto-Sitz-Manufaktur.
**Repo:** https://github.com/Sindojan/OPS-remastered
**GitHub Account:** Sindojan
**Dev Server Port:** 4201

## Tech Stack

- **Framework:** Next.js 16.1.6 (App Router, Turbopack)
- **UI:** React 19, shadcn/ui (new-york style), Radix UI
- **Styling:** Tailwind CSS v4 (oklch Farbraum), CSS Custom Properties
- **Typografie:** DM Sans (body) + JetBrains Mono (mono/data)
- **Icons:** lucide-react
- **Charts:** recharts (für Sparklines)
- **Theme:** next-themes (dark/light, class-basiert)
- **TypeScript:** strict mode

## Design Direction: "Industrial Precision"

- Kontrollraum-Ästhetik, industriell-utilitär
- Teal/Cyan Primary (oklch hue 195)
- Sidebar permanent dunkel (Slate-Blau) in beiden Themes
- Dot-Grid Hintergrund im Content-Bereich
- Monospace für Datenwerte (JetBrains Mono)
- StatusBadge mit rounded-md, Puls-Animation für "busy"
- Uppercase Tracking-Wider für Labels und Spaltenüberschriften

## Ordnerstruktur

```
frontend/
├── app/                          # Routen (App Router)
│   ├── layout.tsx                # Root Layout mit ThemeProvider + AppShell
│   ├── page.tsx                  # Redirect → /agents
│   ├── loading.tsx               # Global Loading Skeleton
│   ├── not-found.tsx             # 404 Seite
│   ├── globals.css               # Theme Tokens, Tailwind Config
│   ├── agents/                   # /agents, /agents/hierarchy
│   ├── production/               # /production, /production/planner
│   ├── inventory/                # /inventory
│   ├── parts/                    # /parts
│   ├── process-plans/            # /process-plans
│   ├── inbox/                    # /inbox
│   ├── reports/                  # /reports
│   ├── employees/                # /employees
│   ├── my-day/                   # /my-day
│   ├── knowledge/                # /knowledge
│   └── settings/                 # /settings
├── components/
│   ├── layout/                   # Layout-Komponenten
│   │   ├── app-shell.tsx         # Drei-Bereich-Shell (Sidebar + Content + AgentPanel)
│   │   ├── sidebar.tsx           # Kollabierbare Navigation mit Sektionen
│   │   ├── topbar.tsx            # Header mit dynamischen Breadcrumbs
│   │   ├── agent-panel.tsx       # 380px rechtes Panel (togglebar)
│   │   └── theme-provider.tsx    # next-themes Wrapper
│   ├── shared/                   # Wiederverwendbare fachliche Komponenten
│   │   ├── status-badge.tsx      # 7 Status-Varianten mit Puls-Animation
│   │   ├── theme-toggle.tsx      # Sun/Moon Toggle
│   │   ├── kpi-card.tsx          # KPI Card mit Trend + Sparkline
│   │   ├── data-table.tsx        # Generische DataTable<T> mit allem
│   │   ├── page-header.tsx       # Seitenkopf mit Breadcrumb + Actions
│   │   ├── confirmation-dialog.tsx # Bestätigungsdialog (default/destructive)
│   │   └── skeleton-variants.tsx # SkeletonTable, SkeletonCard, SkeletonPanel
│   └── ui/                       # shadcn Basis-Komponenten
│       ├── badge.tsx, button.tsx, card.tsx, checkbox.tsx
│       ├── dialog.tsx, dropdown-menu.tsx, input.tsx
│       ├── popover.tsx, select.tsx, separator.tsx
│       ├── skeleton.tsx, table.tsx, tooltip.tsx
│       └── (weitere nach Bedarf via `npx shadcn@latest add`)
├── lib/
│   └── utils.ts                  # cn() Utility
├── types/                        # TypeScript Typen (noch leer)
└── .env.local.example            # NEXT_PUBLIC_API_URL Platzhalter
```

## Abgeschlossene Tasks

### Block 1: Frontend Foundation
- [x] **TASK-FE-001** – Next.js Projekt initialisieren (Commit: `ea88b3a`)
- [x] **TASK-FE-002** – Drei-Bereich-Layout Shell (Commit: `bc8e4e7`)
- [x] **TASK-FE-003** – Navigationsrouten & leere Pages (Commit: `33f17c6`)
- [x] **TASK-FE-004** – Design Tokens & globale Styles (Commit: `1b8722b`)

### Block 2: Shared Component Library
- [x] **TASK-FE-005** – KPI Card Komponente (recharts Sparkline, Trend-Indikator)
- [x] **TASK-FE-006** – DataTable<T> Komponente (Search, Sort, Selection, Pagination, Actions)
- [x] **TASK-FE-007** – PageHeader, ConfirmationDialog, Skeleton-Varianten

**Block 2 noch nicht committed/gepusht.**

## Offene Tasks / Nächste Schritte

### Block 3: Agent Console (TASK-FE-005 ff. laut ursprünglichem Plan)
- [ ] Agent Console Screen – erster inhaltlicher Screen
- [ ] Weitere Feature-Screens

## Wichtige Konventionen

- **Kein Custom CSS** außerhalb von Tailwind + CSS-Variablen
- **shadcn-Komponenten** als Basis, eigene in `components/shared/`
- **Keine externe Table-Library** – eigene DataTable-Logik
- **Keine Backend-Anbindung** bisher – alles reine Darstellung
- **Pfad-Aliases:** `@/components`, `@/lib`, `@/types`
- **Commits:** Conventional Commits mit Task-Nummern, Co-Authored-By Claude

## Zuletzt bearbeitet

**Datum:** 2026-02-20
**Session:** Block 1 + Block 2 komplett, Design-Überarbeitung (Industrial Precision)
**Status:** Dev-Server läuft auf Port 4201, Build fehlerfrei (17/17 Pages)
