# Owlsburg OPS – Design Guide

> **Design-Richtung:** "Industrial Precision" – Kontrollraum-Ästhetik, industriell-utilitär, präzise.

---

## Typografie

| Verwendung | Schriftart | Gewichte | CSS-Variable |
|-----------|-----------|----------|-------------|
| Body / UI | **DM Sans** | 400, 500, 600, 700 | `--font-dm-sans` |
| Daten / Mono | **JetBrains Mono** | 400, 500 | `--font-jetbrains-mono` |

### Anwendung

- **DM Sans** für Überschriften, Labels, Fließtext, Navigation
- **JetBrains Mono** für KPI-Werte, Tabellendaten, Status-Badges, Timestamps, Code-Blöcke
- Tailwind: `font-sans` (DM Sans), `font-mono` (JetBrains Mono)

### Größen-Konventionen

| Element | Klasse |
|---------|--------|
| KPI-Label | `text-[11px] font-medium uppercase tracking-wider` |
| KPI-Wert | `font-mono text-2xl font-bold tracking-tight` |
| Status-Badge Text | `font-mono text-[11px] font-medium tracking-wide` |
| Trend-Wert | `font-mono text-[11px] font-medium` |
| Tabellen-Daten | `font-mono text-sm` |

---

## Farb-System

Alle Farben verwenden den **oklch** Farbraum für konsistente Wahrnehmung.

### Core Palette

#### Light Mode (`:root`)

| Token | oklch-Wert | Beschreibung | Verwendung |
|-------|-----------|-------------|------------|
| `--primary` | `oklch(0.52 0.14 195)` | Teal/Cyan | Hauptaktionen, Links, Fokus-Ringe |
| `--primary-foreground` | `oklch(0.993 0 0)` | Weiß | Text auf Primary |
| `--background` | `oklch(0.97 0.005 250)` | Helles Grau-Blau | Seitenhintergrund |
| `--foreground` | `oklch(0.17 0.02 260)` | Fast Schwarz | Primärtext |
| `--card` | `oklch(0.993 0.002 250)` | Fast Weiß | Karten-Hintergrund |
| `--muted` | `oklch(0.935 0.008 250)` | Helles Grau | Deaktivierte Bereiche |
| `--muted-foreground` | `oklch(0.48 0.02 260)` | Mittelgrau | Sekundärtext, Labels |
| `--secondary` | `oklch(0.935 0.01 250)` | Helles Grau | Sekundäre Buttons |
| `--accent` | `oklch(0.935 0.015 195)` | Helles Teal | Hover-States, Highlights |
| `--destructive` | `oklch(0.55 0.22 25)` | Rot | Löschen, Fehler-Actions |
| `--border` | `oklch(0.895 0.01 250)` | Helles Grau | Trennlinien, Card-Borders |
| `--ring` | `oklch(0.52 0.14 195)` | Teal | Fokus-Ringe |
| `--surface` | `oklch(0.983 0.003 250)` | Leichtes Off-White | Erhöhte Flächen |

#### Dark Mode (`.dark`)

| Token | oklch-Wert | Beschreibung |
|-------|-----------|-------------|
| `--primary` | `oklch(0.62 0.14 195)` | Helleres Teal (besserer Kontrast) |
| `--background` | `oklch(0.115 0.015 255)` | Dunkles Blau-Grau |
| `--foreground` | `oklch(0.9 0.008 250)` | Helles Grau |
| `--card` | `oklch(0.16 0.02 255)` | Leicht erhöhtes Dunkel |
| `--muted` | `oklch(0.2 0.015 255)` | Dunkles Grau |
| `--muted-foreground` | `oklch(0.55 0.015 250)` | Mittleres Grau |
| `--border` | `oklch(0.23 0.015 255)` | Subtile Trennlinie |
| `--surface` | `oklch(0.14 0.018 255)` | Leicht erhöht |

### Semantische Farben

| Token | Light | Dark | Verwendung |
|-------|-------|------|------------|
| `--success` | `oklch(0.58 0.16 155)` | `oklch(0.63 0.16 155)` | Erfolg, aktiv, abgeschlossen |
| `--warning` | `oklch(0.7 0.15 75)` | `oklch(0.75 0.15 75)` | Warnung, ausstehend |
| `--error` | `oklch(0.55 0.22 25)` | `oklch(0.58 0.2 22)` | Fehler, kritisch, löschen |
| `--info` | `oklch(0.58 0.14 245)` | `oklch(0.63 0.14 245)` | Information, in Bearbeitung |

### Agent-Status-Farben

| Token | Light | Dark | Verwendung |
|-------|-------|------|------------|
| `--agent-idle` | `oklch(0.58 0.16 155)` | `oklch(0.63 0.16 155)` | Agent bereit (= Success) |
| `--agent-busy` | `oklch(0.58 0.14 245)` | `oklch(0.63 0.14 245)` | Agent arbeitet (= Info) |
| `--agent-degraded` | `oklch(0.7 0.15 75)` | `oklch(0.75 0.15 75)` | Agent eingeschränkt (= Warning) |
| `--agent-quarantine` | `oklch(0.55 0.22 25)` | `oklch(0.58 0.2 22)` | Agent gesperrt (= Error) |

### Chart-Farben

| Token | Light | Hue | Verwendung |
|-------|-------|-----|------------|
| `--chart-1` | `oklch(0.52 0.14 195)` | Teal | Primäre Datenreihe |
| `--chart-2` | `oklch(0.58 0.13 160)` | Grün | Sekundäre Reihe |
| `--chart-3` | `oklch(0.5 0.12 280)` | Violett | Tertiäre Reihe |
| `--chart-4` | `oklch(0.68 0.14 80)` | Gelb | Vierte Reihe |
| `--chart-5` | `oklch(0.58 0.17 340)` | Pink | Fünfte Reihe |

### Sidebar (immer dunkel, beide Themes)

| Token | Light | Dark |
|-------|-------|------|
| `--sidebar` | `oklch(0.165 0.025 255)` | `oklch(0.09 0.015 255)` |
| `--sidebar-foreground` | `oklch(0.82 0.01 250)` | `oklch(0.72 0.01 250)` |
| `--sidebar-primary` | `oklch(0.58 0.14 195)` | `oklch(0.62 0.14 195)` |
| `--sidebar-accent` | `oklch(0.22 0.025 255)` | `oklch(0.16 0.02 255)` |
| `--sidebar-border` | `oklch(0.26 0.02 255)` | `oklch(0.2 0.015 255)` |

---

## Radius-System

Base-Radius: `0.5rem` (8px)

| Token | Wert | Verwendung |
|-------|------|------------|
| `--radius-sm` | 4px | Kleine Elemente (Chips, Inline-Badges) |
| `--radius-md` | 6px | Buttons, Inputs |
| `--radius-lg` | 8px | Cards, Dialoge |
| `--radius-xl` | 12px | Große Container |

---

## Komponenten-Patterns

### Status-Badge (`DomainStatusBadge`)

Einheitliches Status-Anzeige-Element mit farbcodiertem Punkt und Mono-Text.

```
┌──────────────────┐
│ ● IN_PRODUCTION  │   ← rounded-md, border, font-mono 11px
└──────────────────┘
```

**Varianten:**

| Variant | Hintergrund | Text | Dot | Verwendung |
|---------|------------|------|-----|------------|
| `success` | `emerald-500/10` | `emerald-600` | `emerald-500` | Aktiv, Abgeschlossen, Genehmigt |
| `warning` | `amber-500/10` | `amber-600` | `amber-500` | Wartend, Wartung, Urlaub |
| `error` | `red-500/10` | `red-600` | `red-500` | Fehler, Kritisch, Abgelehnt |
| `info` | `blue-500/10` | `blue-600` | `blue-500` | Offen, Medium, Freigegeben |
| `neutral` | `muted` | `muted-foreground` | `muted-foreground/50` | Entwurf, Inaktiv, Niedrig |
| `primary` | `primary/10` | `primary` | `primary` | In Produktion, Produkt-Typ |

**Puls-Animation:** `pulse={true}` auf BUSY-Status → 2s ease-in-out Opacity-Animation.

### KPI-Card

```
┌─────────────────────────────────┐
│ OFFENE AUFTRÄGE          ╱╲╱╲  │  ← Sparkline (rechts, opacity 60%)
│ 42                       ╱╲╱╲  │  ← font-mono text-2xl bold
│ ↑ +12%                         │  ← Trend (success/error/neutral)
└─────────────────────────────────┘
```

- Subtiler Top-Akzent: `bg-gradient-to-r from-transparent via-primary/20 to-transparent`
- Hover: `shadow-md` Transition
- Sparkline: `recharts` LineChart, Stroke `#2ba8a0` (Teal)

### DataTable

- shadcn/ui Table-Basis
- Mono-Font für Datenfelder (Nummern, IDs, Zeitstempel)
- Sortierbare Spaltenköpfe
- Toolbar mit Such-Input + Filter-Buttons + "Neu erstellen"-Button

### Cards

- Hintergrund: `var(--card)`
- Border: `var(--border)`
- Radius: `rounded-lg`
- Kein Shadow by Default, `shadow-md` on Hover bei interaktiven Cards

---

## Layout-Architektur

```
┌──────────────────────────────────────────────────────────┐
│  Sidebar (immer dunkel)  │  Header (Breadcrumb + User)   │
│  ─────────────────────── │  ───────────────────────────── │
│  ● Mein Tag              │                                │
│  ● Konsole               │  ┌──────────────────────────┐ │
│  ● Produktion            │  │                          │ │
│  ● Maschinen             │  │    Content Area          │ │
│  ● Lager                 │  │    (dot-grid BG)         │ │
│  ● Teile & Prozesse      │  │                          │ │
│  ─────────────────────── │  │                          │ │
│  ● Mitarbeiter           │  │                          │ │
│  ● Kunden                │  │                          │ │
│  ● Posteingang           │  │                          │ │
│  ─────────────────────── │  │                          │ │
│  ● Wissensdatenbank      │  │                          │ │
│  ● Berichte              │  └──────────────────────────┘ │
│  ● Einstellungen         │                                │
│  ─────────────────────── │  ┌──────────────────────────┐ │
│  [Theme Toggle]          │  │  Agent Panel (rechts)    │ │
│                          │  │  (ausklappbar)           │ │
│                          │  └──────────────────────────┘ │
└──────────────────────────────────────────────────────────┘
```

- **Sidebar:** Permanent dunkel (Slate-Blau), collapsible, zeigt Icons + Labels
- **Content Area:** `dot-grid` Hintergrund (24px Raster, subtile Punkte)
- **Agent Panel:** Rechts ausklappbar, SSE-Streaming Chat mit dem KI-Agenten

### Dot-Grid Hintergrund

```css
.dot-grid {
  background-image: radial-gradient(circle, oklch(0.45 0.02 260 / 0.1) 1px, transparent 1px);
  background-size: 24px 24px;
}
/* Dark Mode: geringere Opacity */
.dark .dot-grid {
  background-image: radial-gradient(circle, oklch(0.45 0.02 250 / 0.07) 1px, transparent 1px);
}
```

---

## Animationen

| Name | Dauer | Easing | Verwendung |
|------|-------|--------|------------|
| `status-pulse` | 2s | ease-in-out | BUSY-Status Badge Puls |
| `line-flow` | – | linear | Deko-Linien (Kontrollraum-Ästhetik) |
| `shadow-md` Transition | 200ms | default | Card Hover |
| `opacity` Transition | default | default | Sparkline Hover (60% → 100%) |

---

## Icons

**Library:** `lucide-react`

| Bereich | Icons |
|---------|-------|
| Navigation | `Sun`, `Bot`, `Factory`, `Cog`, `Package`, `Puzzle`, `Users`, `Building2`, `Inbox`, `BookOpen`, `BarChart3`, `Settings` |
| Status | `TrendingUp`, `TrendingDown`, `Minus` |
| Actions | `Shield` (Security), `Zap` (Quick Actions) |
| System | `ChevronsLeft/Right` (Sidebar Toggle) |

---

## Spacing & Grid

- Basis-Einheit: **4px** (Tailwind `1` = 4px)
- Card-Padding: `p-4` (16px)
- Sektions-Gap: `gap-4` oder `gap-6`
- Page-Padding: `p-6`
- Sidebar-Width: Expanded ~240px, Collapsed ~64px

---

## Design-Regeln

1. **Kein Custom-CSS** – Nur shadcn/ui + Tailwind-Utilities
2. **Monospace für Daten** – Alles was gemessen, gezählt oder berechnet wird: `font-mono`
3. **Konsistente Status-Farben** – Immer `DomainStatusBadge` mit den 6 Varianten verwenden
4. **Sidebar bleibt dunkel** – In beiden Themes (Light + Dark) permanent dunkles Slate-Blau
5. **oklch Farbraum** – Niemals hex/rgb für Theme-Tokens, immer oklch
6. **Industrielle Zurückhaltung** – Keine Farbe ohne Funktion, keine Dekoration ohne Zweck
7. **Dot-Grid als Textur** – Content-Bereich hat subtiles Punkt-Raster als Kontrollraum-Referenz
8. **Sprache: Deutsch** – Alle UI-Labels, Buttons, Fehlermeldungen auf Deutsch
