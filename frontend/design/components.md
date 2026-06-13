# Keystone Dashboard — Component Catalog

Every component described with props, states, and usage. Build these as React Server Components where possible; Client Components only where interactivity requires.

---

## Layout Shell

### AppLayout

Root layout wrapping all views.

```
┌──────────────────────────────────────────────────┐
│ Nav Rail (232px)  │  Top Bar (56px h)            │
│                   ├───────────────────────────────┤
│  Wordmark         │                              │
│  Nav items        │  Content Area                │
│  ...              │  (scrollable)                │
│  Spacer           │                              │
│  Theme toggle     │                              │
└───────────────────┴──────────────────────────────┘
```

**Props:**
- `children: ReactNode`

**State:**
- Active view (from URL path or searchParams)
- Theme (light/dark, persisted to localStorage)

**Files:**
- `components/layout/AppLayout.tsx`
- `components/layout/NavRail.tsx`
- `components/layout/TopBar.tsx`

---

### NavRail

Fixed left navigation, 232px wide.

**Props:** none (reads pathname from `usePathname()`)

**Items:**

| Label            | viewId         | 2-letter icon | Badge |
|------------------|----------------|---------------|-------|
| Overview         | `overview`     | `ov`          | 88 (health score) |
| API Inventory    | `inventory`    | `in`          | 24 (total APIs) |
| Breaking Changes | `breaking`     | `br`          | 7 (open issues) |
| Policy Compliance| `policy`       | `po`          | 12 (violations) |
| Dependency Graph | `graph`        | `dg`          | — |
| Notifications    | `notifications`| `nt`          | 4 (unread) |

**Active state:** `aria-current="true"` → `color: var(--accent)`, `font-weight: 510`.
**Hover state:** `color: var(--fg)`, `background: var(--bg)`.

**Badge colours:** Active = accent bg + white text. Inactive = border bg + fg text.

**Extras:**
- Wordmark at top: "Keystone" in display 22px + "API Governance" mono 10px subtitle.
- Theme toggle at bottom: 28×16px track, 12px knob, CSS-only with `::after`.

---

### TopBar

56px height, full remaining width.

**Props:**
- `breadcrumb: string` (current view label)
- `lastIngestion?: string` (relative time, e.g. "3 min ago")

**Content:**
- Left: breadcrumb — "Keystone / Overview" (mono 11px, current segment bold).
- Right: live indicator — green pulsing dot + "Last ingestion X ago".

---

## View Container

### ViewShell

Wraps each view with title + subtitle.

**Props:**
- `title: string`
- `subtitle: string`
- `children: ReactNode`

**Renders:**
```
<h1 class="view-title">{title}</h1>
<p class="view-subtitle">{subtitle}</p>
{children}
```

---

## Data Display

### StatGrid

**Props:**
- `stats: Array<{ value: string | number; label: string; tone?: 'accent' | 'success' | 'danger' | 'warn' }>`

**Renders:** CSS grid, `auto-fit, minmax(180px, 1fr)`, 1px gap with border background.

---

### ScoreRing (Overview only)

**Props:**
- `score: number` (0-100)
- `size?: number` (default 160)

**Renders:** SVG donut chart. Circumference = 427 (r=68). Dashoffset = 427 * (1 - score/100).

---

### DimensionBar

**Props:**
- `items: Array<{ label: string; value: number | string; pct: number; tone?: 'fg' | 'accent' | 'success' | 'warn' | 'danger' }>`

**Renders:** Row of label (100px right-aligned mono) + bar (6px h) + value (mono tabular).

---

### DataTable

**Props:**
- `columns: Array<{ key: string; label: string; mono?: boolean }>`
- `rows: Array<Record<string, ReactNode>>`

---

### Pill / StatusBadge

**Props:**
- `tone: 'critical' | 'high' | 'low' | 'info' | 'pass' | 'fail' | 'warn'`
- `children: string`

---

### RuleCard (Policy view)

**Props:**
- `name: string`
- `description: string`
- `scope: string`
- `violationCount: number`
- `violatingServices: string[]`
- `status: 'passing' | 'violated'`

**States:**
- Violated: 3px left border `var(--danger)`, danger pill.
- Passing: 3px left border `var(--success)`, pass pill.

---

### DiffBlock (Breaking Changes view)

**Props:**
- `diffText: string` (raw diff with `-` and `+` line markers)

**Renders:** Lines starting with `-` get `diff-removed` class (danger + line-through). Lines starting with `+` get `diff-added` class (success).

---

### BreakageCard (Breaking Changes view)

**Props:**
- `serviceName: string`
- `changeType: 'field-removal' | 'type-change' | 'path-removal' | 'enum-change'`
- `severity: 'critical' | 'high'`
- `detectedAt: string`
- `versionFrom: string`
- `versionTo: string`
- `impactedConsumers: string[]`
- `diffText: string`

---

### DependencyGraph

**Props:**
- `nodes: Array<{ id: string; label: string; subtitle: string; x: number; y: number; impacted?: boolean }>`
- `edges: Array<{ from: string; to: string; impacted?: boolean }>`

**Renders:** SVG with rect nodes + line edges. Impacted nodes get danger stroke + tinted fill. Impacted edges get danger stroke.

---

### NotificationFeed

**Props:**
- `items: Array<NotificationItem>`

**NotificationItem:**
```
{
  id: string
  title: string
  description: string
  severity: 'critical' | 'high' | 'warning'
  channel: 'slack' | 'email' | 'webhook'
  channelDetail: string
  read: boolean
  timestamp: string
  relativeTime: string
}
```

---

### ImpactCascade (Dependency Graph view)

**Props:**
- `sourceService: string`
- `sourceVersion: string`
- `changeDescription: string`
- `downstreamServices: string[]`
- `totalConsumers: number`
- `severity: 'critical' | 'high'`

---

## Two-Column Layout

### TwoCol

**Props:**
- `left: ReactNode`
- `right: ReactNode`

**Renders:** CSS grid `1fr 1fr`, 24px gap.

---

## Scrollable Content Area

The `<main>` content area is a flex column. The content div inside is `overflow-y: auto`. TopBar is fixed at the top of main (flex-shrink: 0).
