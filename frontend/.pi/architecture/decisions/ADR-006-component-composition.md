# Architecture Decision Record: ADR-006

<!--
Canonical Reference: .pi/architecture/decisions/ADR-006-component-composition.md
Blueprint Source: Guardian Framework v1.2
-->

## Title

Component Composition Strategy — Server Components with Client Islands

## Status

- [x] Accepted

## Context

The dashboard has 6 views, each composed of multiple components (StatGrid, ScoreRing, DataTable, RuleCard, BreakageCard, DependencyGraph, NotificationFeed, etc.). The design system specifies a precise component catalog with defined props and states.

Next.js App Router supports React Server Components (RSC) by default — components that render on the server and send only HTML to the client. Client Components are explicitly marked with `"use client"`.

The challenge: most of the dashboard is static content (tables, stats, cards) that could be Server Components, but view switching and theme toggling require client-side interactivity.

**Tech Stack:** TypeScript (Bun/Node), Next.js (App Router)

## Decision

We will use a **Server Components with Client Islands** pattern:

### 1. Server Components by Default

Every component that does not require interactivity is a **Server Component**. This includes:
- `ViewShell` — layout wrapper (title, subtitle, content)
- `StatGrid` — pure data display
- `ScoreRing` — SVG rendering (data comes from server)
- `DimensionBar` — data display
- `DataTable` — data rendering without client-side sorting/filtering
- `RuleCard` — policy display card
- `BreakageCard` — breaking change display card
- `DiffBlock` — diff text rendering
- `Pill` / `StatusBadge` — status indicators
- `TwoCol` — layout helper

### 2. Client Components ("Islands")

Only components with interactivity need `"use client"`:
- `NavRail` — view switching via searchParams, theme toggle
- `TopBar` — live indicator pulse animation
- `ThemeToggle` — CSS class toggle + localStorage write
- `ViewSwitcher` (in page.tsx) — reads searchParams, conditionally renders views
- `NotificationFeed` — polling for updates, read/unread toggling
- `DependencyGraph` — SVG hover interactions (but initial render is server)

### 3. Data Loading Strategy

Data is fetched in the root `page.tsx` (Server Component) based on the active `?view=` param. The fetched data is passed as props to view components:

```typescript
// app/page.tsx (Server Component)
export default async function HomePage({ searchParams }: Props) {
  const view = searchParams?.view ?? 'overview';
  switch (view) {
    case 'overview':
      const data = await fetchGovernanceHealth();
      return <OverviewView data={data} />;
    // ...
  }
}
```

### 4. Composition Rules

- Server Components can import and render Client Components
- Client Components cannot import Server Components (but can accept them as `children` via props)
- Data fetching functions are pure async functions, not hooks — usable in Server Components
- Use `Suspense` boundaries around data-dependent sections for streaming

## Alternatives Considered

| Alternative | Pros | Cons | Reason Rejected |
|-------------|------|------|-----------------|
| All Client Components | Simple, uniform, familiar hooks | No server rendering; larger bundle; slower initial load | Rejected — loses RSC benefits; most dashboard components are pure display |
| All Server Components (no client interactivity) | Fastest initial load | Cannot handle view switching, theme toggling, or user interaction | Rejected — the app needs client-side interactivity |
| Full SSR with hydration | Familiar, works everywhere | All components ship JS bundle; hydrating 6 views on every load is wasteful | Rejected — "Client Islands" sends JS only for interactive parts |
| Static Site Generation (SSG) | Fastest possible load | Dashboard data changes frequently; SSG would show stale data | Rejected — governance data must be fresh on every load |

## Consequences

### Positive
- Minimal client-side JavaScript — only interactive components ship JS
- Fast initial load — most HTML is server-rendered
- Data fetching happens before React hydration
- Clear boundary between server and client concerns
- Components are testable without a browser (Server Components)

### Negative
- Component splitting requires discipline — accidentally making a Server Component a Client Component is easy
- Client Components cannot directly access server-side data sources (must receive props)
- Dev tooling for Server Components is still maturing
- Client Components at higher levels of the tree force all children to be client-rendered

### Neutral
- Can gradually move boundary between server/client as needs evolve
- Suspense boundaries provide streaming UX without extra effort

## Implementation

**Affected Modules:**
- `.pi/architecture/modules/frontend-app.md`

**Files to Update:**
- `app/layout.tsx` — Root layout (Server Component)
- `app/page.tsx` — Root page (Server Component with data fetching)
- `components/layout/NavRail.tsx` — Client Component
- `components/layout/TopBar.tsx` — Client Component
- `components/shared/*.tsx` — Shared UI (Server Components)
- `components/overview/ScoreRing.tsx` — Server Component
- `components/graph/DependencyGraph.tsx` — Client Component
- `components/notifications/NotificationFeed.tsx` — Client Component

## Validation

**Validators Required:**
- architecture-validator: Verify "use client" boundaries, no server-only imports in client components
- operations-validator: Verify bundle size, hydration, streaming behavior

## References

- Related ADRs: ADR-002 (Frontend Routing), ADR-003 (API Data Fetching)
- Design docs: `design/components.md`
- Next.js docs: Server Components, Client Components, Streaming

---

*Decision date: 2026-06-13*
*Decision makers: System Architect*
