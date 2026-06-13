# Frontend Application

<!--
Canonical Reference: .pi/architecture/modules/frontend-app.md
Blueprint Source: Guardian Framework v1.2
-->

## Module Status

**Status:** Planned
**Last reviewed:** 2026-06-13
**Source session:** 25c57121-7a91-47ce-ae20-5c561181984d

## Overview

The Keystone Dashboard is a **read-heavy single-page application** that surfaces API governance data across 6 views. It does NOT implement any backend domain logic — it is purely a **view layer** that fetches from the Keystone REST API and renders data in a print-newspaper design aesthetic.

**Tech Stack:** TypeScript (Bun/Node), Next.js (App Router), Tailwind CSS

## Responsibilities

- Render 6 governance views with consistent layout and navigation
- Fetch and display data from 6 backend data sources via REST API
- Provide light/dark theme with localStorage persistence
- Handle loading, error, and empty states for every view
- Pass auth tokens to backend (no auth logic in frontend)
- No domain logic: no spec ingestion, no diff analysis, no policy DSL evaluation, no BFS graph traversal, no notification dispatch

---

## Architecture Layer Model

```
┌──────────────────────────────────────────────────────┐
│                    View Layer                         │
│  Overview  │  Inventory  │  Breaking  │  Policy     │
│  Graph     │  Notifications                           │
├──────────────────────────────────────────────────────┤
│                  Shared UI Layer                      │
│  StatGrid  │  DataTable  │  Pill  │  ViewShell     │
│  ScoreRing │  DimensionBar   │  TwoCol              │
├──────────────────────────────────────────────────────┤
│                  Layout Layer                         │
│  AppLayout │  NavRail  │  TopBar  │  ThemeToggle    │
├──────────────────────────────────────────────────────┤
│                  Data Layer                           │
│  API Client  │  Types  │  Transform  │  Endpoints    │
└──────────────────────────────────────────────────────┘
        │
        ▼
┌──────────────────────────────────────────────────────┐
│           Keystone Backend (6 data sources)           │
│  REST API at NEXT_PUBLIC_KEYSTONE_API_URL             │
└──────────────────────────────────────────────────────┘
```

---

## Views & Backend Data Sources

| Nav Item | Icon | View ID | Backend Endpoints | Renders |
|----------|------|---------|-------------------|---------|
| Overview | `ov` | `overview` | `GET /dashboard/health`, `GET /dashboard/health-score` | ScoreRing, StatGrid, DimensionBar, RecentBreakages table, TopViolations table |
| API Inventory | `in` | `inventory` | `GET /ingestion/apis`, `GET /ingestion/apis/stale` | DataTable (full), DataTable (stale) |
| Breaking Changes | `br` | `breaking` | `GET /breaking/reports/latest` | StatGrid, BreakageCards, DiffBlock |
| Policy Compliance | `po` | `policy` | `GET /policies`, `GET /policies/summary` | StatGrid, RuleCards |
| Dependency Graph | `dg` | `graph` | `GET /graph/services`, `POST /graph/impact` | SVG DependencyGraph, ImpactCascade |
| Notifications | `nt` | `notifications` | `GET /notifications/channels`, `GET /notifications/{id}` | StatGrid, NotificationFeed, ChannelCard |

---

## Components

### Layout Components

| Component | File | Type | Purpose |
|-----------|------|------|---------|
| AppLayout | `components/layout/AppLayout.tsx` | Server | Root flex row: NavRail + main content area |
| NavRail | `components/layout/NavRail.tsx` | Client | Fixed 232px left nav, view switching, theme toggle, wordmark |
| TopBar | `components/layout/TopBar.tsx` | Client | 56px bar: breadcrumb (left) + live indicator (right) |

### Shared UI Components

| Component | File | Type | Purpose |
|-----------|------|------|---------|
| ViewShell | `components/shared/ViewShell.tsx` | Server | Wraps each view with title + subtitle |
| StatGrid | `components/shared/StatGrid.tsx` | Server | CSS grid of stat cells (value + label + tone) |
| DataTable | `components/shared/DataTable.tsx` | Server | Full-width table with mono headers, tabular-nums |
| Pill | `components/shared/Pill.tsx` | Server | Status badge with tone (critical/high/low/info/pass/fail/warn) |
| SectionLabel | `components/shared/SectionLabel.tsx` | Server | Mono uppercase section header |
| TwoCol | `components/shared/TwoCol.tsx` | Server | CSS grid 1fr 1fr layout |
| ViewSkeleton | `components/shared/ViewSkeleton.tsx` | Server | Loading skeleton matching view layout |
| ErrorState | `components/shared/ErrorState.tsx` | Server | Consistent error display with retry |
| ZeroState | `components/shared/ZeroState.tsx` | Server | Empty state with guidance |

### View-Specific Components

| Component | File | Type | Purpose |
|-----------|------|------|---------|
| ScoreRing | `components/overview/ScoreRing.tsx` | Server | SVG donut chart (160×160, accent arc, score centre) |
| DimensionBar | `components/overview/DimensionBar.tsx` | Server | Horizontal bar with label + fill + value |
| BreakageCard | `components/breaking/BreakageCard.tsx` | Server | Breaking change detail card with meta + diff + consumers |
| DiffBlock | `components/breaking/DiffBlock.tsx` | Server | Raw diff text (red - / green + lines) |
| RuleCard | `components/policy/RuleCard.tsx` | Server | Policy rule card with 3px left border (green/red) |
| DependencyGraph | `components/graph/DependencyGraph.tsx` | Client | SVG interactive graph with hover states |
| ImpactCascade | `components/graph/ImpactCascade.tsx` | Server | Blast radius summary card |
| NotificationFeed | `components/notifications/NotificationFeed.tsx` | Client | Feed items with read/unread, polling |
| ChannelCard | `components/notifications/ChannelCard.tsx` | Server | Channel config display card |

---

## Data Flow

```
Browser Request (/?view=overview)
    │
    ▼
app/page.tsx (Server Component)
    │ reads searchParams.view
    ▼
Switch on view → call fetch function
    │
    ▼
fetch(NEXT_PUBLIC_KEYSTONE_API_URL/dashboard/health)
    │
    ▼
Keystone Backend → JSON Response (snake_case)
    │
    ▼
lib/api/transform.ts → camelCase conversion
    │
    ▼
ViewComponent renders → HTML streamed via Suspense
```

### Fetch Strategy

| Fetch Type | Mechanism | Revalidation |
|------------|-----------|-------------|
| Initial load (server) | `fetch()` in Server Component | `next: { revalidate: 60 }` |
| View switch (client) | Client-side `fetch()` | Fresh on each switch |
| Notification polling | `setInterval` 30s | Client-side interval |
| Policy CRUD | `POST`/`PUT`/`DELETE` to backend | Refetch after mutation |

---

## File Tree

```
app/
├── globals.css              ← tokens.css content + Tailwind directives
├── layout.tsx               ← AppLayout (Server Component)
├── page.tsx                 ← reads ?view=, fetches data, renders view
├── error.tsx                ← global error boundary
└── overrides/
    └── [view]-error.tsx     ← per-view error boundaries

components/
├── layout/
│   ├── AppLayout.tsx
│   ├── NavRail.tsx
│   └── TopBar.tsx
├── shared/
│   ├── ViewShell.tsx
│   ├── StatGrid.tsx
│   ├── DataTable.tsx
│   ├── Pill.tsx
│   ├── SectionLabel.tsx
│   ├── TwoCol.tsx
│   ├── ViewSkeleton.tsx
│   ├── ErrorState.tsx
│   └── ZeroState.tsx
├── overview/
│   ├── ScoreRing.tsx
│   └── DimensionBar.tsx
├── inventory/
│   └── ApiTable.tsx
├── breaking/
│   ├── DiffBlock.tsx
│   └── BreakageCard.tsx
├── policy/
│   └── RuleCard.tsx
├── graph/
│   ├── DependencyGraph.tsx
│   └── ImpactCascade.tsx
└── notifications/
    ├── NotificationFeed.tsx
    └── ChannelCard.tsx

lib/
├── api/
│   ├── client.ts            ← fetch wrapper, base URL, auth
│   ├── types.ts             ← TypeScript interfaces (camelCase)
│   ├── transform.ts         ← snake_case ↔ camelCase
│   ├── endpoints.ts         ← endpoint URL constants + fetch functions
│   └── errors.ts            ← ApiError type hierarchy
└── theme.ts                 ← theme read/write helpers
```

---

## Dependencies

### Depends On
- **Keystone Backend REST API**: All 6 backend data sources (Ingestion, Analysis, Policy, Graph, Dashboard, Notifications)
- **Design System**: `design/DESIGN.md`, `design/tokens.css`, `design/components.md`, `design/data-schema.md`

### Used By
- End users (API Developers, Compliance Managers, Repository Owners)

---

## Security Considerations

| Concern | Mitigation | Validator |
|---------|------------|-----------|
| Auth token exposure | Tokens in Bearer header only, never in URLs | security-validator |
| CORS | Backend configures allowed origins | security-validator |
| XSS | React's default escaping + no `dangerouslySetInnerHTML` | security-validator |
| FOUC (theme flash) | Inline `<script>` in `<head>` reads localStorage before hydration | operations-validator |

**Authentication/Authorization:**
- Frontend passes Bearer token to backend (no login UI)
- RBAC enforced server-side; UI conditionally shows/hides compliance-manager controls

**No frontend data storage:**
- View preference stored in `localStorage` (non-sensitive)
- Theme preference stored in `localStorage` (non-sensitive)
- No API keys, no secrets, no user data stored on client

---

## Testing Requirements

| Test Type | Coverage Target | Files |
|-----------|-----------------|-------|
| Unit | 80%+ | `components/**/*.test.tsx`, `lib/**/*.test.ts` |
| Integration | 70%+ | `components/views/*.test.tsx` (data flow) |
| E2E | Key flows | Playwright: view loading, theme toggle, view switch, error states |

**Key Test Scenarios:**
- All 6 views render with mock data
- Loading states show skeletons
- API errors show ErrorState with retry
- Empty data shows ZeroState
- Theme toggle persists across refresh
- View switching updates URL and renders correct view
- Notification badge shows unread count

---

## Error Handling

All data fetching returns the discriminated `AsyncData<T>` type:

```typescript
type AsyncData<T> =
  | { status: 'loading' }
  | { status: 'success'; data: T }
  | { status: 'error'; error: ApiError }
  | { status: 'empty' };
```

- **Loading**: Suspense boundaries with ViewSkeleton per-view
- **Error**: Per-view error boundaries with ErrorState (muted, border-accent, retry button)
- **Empty**: ZeroState with guidance text per-view
- **No toast/snackbar**: Design system prohibits toast notifications

---

## Theme Architecture

```
tokens.css ──→ globals.css (CSS custom properties on :root / [data-theme="dark"])
      │
      ├──→ tailwind.config.ts (@theme colors reference CSS vars)
      │
      └──→ NavRail (theme toggle → set data-theme + localStorage)
                 │
                 └──→ Inline <script> in layout.tsx (pre-hydration FOUC prevention)
```

### Token Values

| Token | Light (OKLch) | Dark (OKLch) | Usage |
|-------|---------------|--------------|-------|
| `--bg` | `97% 0.012 80` | `16% 0.012 70` | Page background |
| `--surface` | `99% 0.005 80` | `20% 0.010 70` | Cards, nav, header |
| `--fg` | `20% 0.02 60` | `92% 0.006 80` | Primary text |
| `--accent` | `58% 0.16 35` | `64% 0.16 35` | Score ring, links, CTAs |

---

## View Content Map

### Overview (`?view=overview`)
1. View title + subtitle
2. Score ring (160×160) + 5 dimension bars (compliance, breaking, coverage, staleness, impact)
3. Stat grid (5 cells: total APIs, active policies, breaking changes 30d, services at risk, dependency edges)
4. Two-col: Recent Breakages table + Top Policy Violations table

### API Inventory (`?view=inventory`)
1. View title + subtitle
2. Full API table (service, version, spec format, health pill, last analyzed, owner)
3. Section label: "Stale Specifications"
4. Stale APIs table (name, last ingested, days stale, version)

### Breaking Changes (`?view=breaking`)
1. View title + subtitle
2. Stat grid (4 cells: total 30d, critical, high, non-breaking)
3. Section label: "Critical Breakages"
4. Breakage cards with severity, meta, diff block, impacted consumers

### Policy Compliance (`?view=policy`)
1. View title + subtitle
2. Stat grid (4 cells: active policies, pass rate, open violations, APIs covered)
3. Section label: "Policy Rules"
4. Rule cards (3px left border, green for passing, red for violated)

### Dependency Graph (`?view=graph`)
1. View title + subtitle
2. SVG graph container (900×420, 15 nodes, 12 edges)
3. Legend row
4. Section label: "Impact Cascade"
5. Impact cascade card

### Notifications (`?view=notifications`)
1. View title + subtitle
2. Stat grid (4 cells: total 7d, unread, active channels, delivery rate)
3. Section label: "Unread"
4. Notification feed
5. Section label: "Channels"
6. Two-col: Slack + Email channel cards

---

## Change Log References

| Date | Change | Section | Status |
|------|--------|---------|--------|
| 2026-06-13 | Initial architecture scaffold — single frontend module replacing 6 backend-mirror modules | all | synced |

See full details in `.pi/architecture/CHANGELOG.md`

---

*Last updated: 2026-06-13*
*Module version: 1.0.0*
