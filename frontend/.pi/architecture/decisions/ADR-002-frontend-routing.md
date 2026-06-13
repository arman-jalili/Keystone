# Architecture Decision Record: ADR-002

<!--
Canonical Reference: .pi/architecture/decisions/ADR-002-frontend-routing.md
Blueprint Source: Guardian Framework v1.2
-->

## Title

Single-Page Application with SearchParam Routing

## Status

- [x] Accepted

## Context

The frontend must serve six distinct views: Overview, API Inventory, Breaking Changes, Policy Compliance, Dependency Graph, and Notifications. The design system specifies a desktop-first SPA with a fixed left nav rail, where view switching should feel instantaneous without full page navigation.

The project uses **Next.js (App Router)** — a framework that natively supports both server-side rendering (SSR) and client-side navigation.

**Tech Stack:** TypeScript (Bun/Node), Next.js (App Router)

## Decision

We will use a **single-page application pattern** with the active view determined by a `?view=` search parameter. This is implemented within Next.js App Router as a single root page (`app/page.tsx`) that reads `searchParams` and conditionally renders the appropriate view component.

### Routing Structure

```
/                           → redirects to /?view=overview
/?view=overview             → Overview / Health Score
/?view=inventory            → API Inventory / Catalog
/?view=breaking             → Breaking Change Analysis
/?view=policy               → Policy Compliance
/?view=graph                → Dependency Graph
/?view=notifications        → Notification Center
```

### Data Fetching

All data fetching happens in the root page based on the active `view` param. Each view imports its data-fetching function and calls it conditionally. Data fetching uses `fetch()` to the Keystone backend at `NEXT_PUBLIC_KEYSTONE_API_URL`.

- **Initial load**: Server-side fetch on page request
- **View switches**: Client-side navigation via `next/navigation` `useRouter` + `useSearchParams`
- **Client components**: Only where interactivity requires (view switching, theme toggle, notification polling)

## Alternatives Considered

| Alternative | Pros | Cons | Reason Rejected |
|-------------|------|------|-----------------|
| Separate routes per view (`/overview`, `/policy`, etc.) | Deep-linkable, SEO-friendly, clear URL hierarchy | Full page navigation on every view switch; more boilerplate with parallel layouts | Rejected — SPA feel is a design requirement; view switching must be instant |
| Nested routes with layout groups (`/(dashboard)/overview`) | Combines RSC benefits with clean URLs | Requires layout.tsx for each route group; view count is small enough that nesting adds complexity | Rejected — unnecessary nesting for 6 views |
| Pure client-side SPA (no Next.js) | Full control over routing, smallest bundle | Loses SSR benefits, no RSC streaming, no built-in code splitting | Rejected — Next.js is the project standard; server components for initial data load improve perceived performance |
| Hash routing (`/#/overview`) | No server round-trip on navigation | Breaks SSR, ugly URLs, no history API integration | Rejected — search params are cleaner and work with Next.js App Router |

## Consequences

### Positive
- Instant view switching preserves SPA feel
- Search params are bookmarkable and shareable
- Server components handle initial data load (fast first paint)
- Single page reduces boilerplate vs. multi-route approach
- `localStorage` persistence of last-view provides continuity

### Negative
- All views' data is fetched through one page — potential over-fetching
- Deep links require the app to parse `?view=` on the client for dynamic content
- Error boundaries must be per-view, not per-route
- Browser back/forward requires careful searchParam sync

### Neutral
- View components are lazy-loaded based on active param
- Theme is persisted separately in `localStorage` (`keystone-theme`)

## Implementation

**Affected Modules:**
- `.pi/architecture/modules/frontend-app.md`

**Files to Update:**
- `components/layout/NavRail.tsx`
- `components/layout/AppLayout.tsx`
- `app/page.tsx`
- `lib/view-config.ts`

## Validation

**Validators Required:**
- architecture-validator: Verify view loading pattern, searchParam handling
- integration-validator: Verify data fetching for each view matches backend endpoints

## References

- Related ADRs: ADR-003 (API Contract & Data Fetching), ADR-004 (Theme & Design System)
- Design docs: `design/DESIGN.md`, `design/routes.md`
- Domain exploration: `.pi/domain/exploration.md`

---

*Decision date: 2026-06-13*
*Decision makers: System Architect*
