# Architecture Decision Record: ADR-007

<!--
Canonical Reference: .pi/architecture/decisions/ADR-007-error-loading-states.md
Blueprint Source: Guardian Framework v1.2
-->

## Title

Error Handling, Loading States & Empty States

## Status

- [x] Accepted

## Context

The dashboard has six views that each fetch data from the backend. Network failures, empty datasets, partial data, and slow responses must be handled gracefully. The design system specifies a "quietly intelligent" tone — error states should be informative but not alarming.

The backend returns standard HTTP status codes:
- 200: Success
- 201: Created
- 204: No Content
- 400: Bad Request
- 404: Not Found
- 422: Unprocessable Entity
- 500: Internal Server Error

**Tech Stack:** TypeScript (Bun/Node), Next.js (App Router)

## Decision

### 1. Loading States

Every data-dependent view uses **server-side Suspense boundaries** for streaming:

```typescript
// app/page.tsx
export default async function HomePage({ searchParams }: Props) {
  return (
    <Suspense fallback={<ViewSkeleton view={currentView} />}>
      <ActiveView view={currentView} />
    </Suspense>
  );
}
```

Each view has a matching **skeleton component** that matches its layout (stat grid placeholders, card outlines, table row placeholders) using the same CSS grid structure but with muted background fills and an animated pulse.

### 2. Error States

Errors are caught at the **view level** using Next.js `error.tsx` boundaries. Each view has its own error boundary:

```
app/
├── error.tsx                    ← global error boundary
├── overrides/
│   ├── overview-error.tsx
│   ├── inventory-error.tsx
│   └── ... (per-view error boundaries)
```

Error display components follow a consistent pattern:
- Muted mono label: "Unable to load [view name]"
- Description: "The data could not be retrieved. [Actionable guidance]"
- Retry button (client component)
- No red backgrounds, no alert banners — border-left accent line + muted text

### 3. Empty States

When a view's data is valid but empty (no specs ingested, no violations, no policies), a **zero-state component** is shown instead of an empty table:

```typescript
interface ZeroState {
  icon?: never;  // no icons per design system
  label: string;          // mono label e.g. "NO VIOLATIONS"
  description: string;    // body text explaining what "empty" means
  action?: {
    label: string;        // e.g. "Upload a spec"
    href?: string;        // external link
    onClick?: () => void; // client-side action
  };
}
```

Empty tables render a single row with `colspan` showing the zero-state message (no empty table body).

### 4. Loading/Error/Empty via AsyncData Type

All data fetching returns the discriminated `AsyncData<T>` type:

```typescript
type AsyncData<T> =
  | { status: 'loading' }
  | { status: 'success'; data: T }
  | { status: 'error'; error: ApiError }
  | { status: 'empty' };
```

View components destructure this to render the appropriate state.

### 5. No Toast Notifications

The design system has no toast/snackbar component. Error states are inline in the view that failed. Cross-cutting errors (auth failures) redirect to a dedicated error page.

## Alternatives Considered

| Alternative | Pros | Cons | Reason Rejected |
|-------------|------|------|-----------------|
| Global error boundary only | Simple, one place to handle errors | Loses view context; a breaking-change error shouldn't blank the entire dashboard | Rejected — per-view errors preserve the rest of the dashboard |
| Toast notifications for all errors | Visible regardless of scroll position, modern UX | Contradicts design system's quiet aesthetic; stackable toasts add complexity | Rejected — design system explicitly avoids banners and alerts |
| Optimistic UI (show cached data on error) | Best UX for transient failures | Dashboard data changes frequently; cache invalidation is complex | Rejected — governance data should be accurate, not stale |
| Skeleton loading for all views | Smooth perceived performance | Heavy on initial bundle if all skeletons are separate | Rejected — one generic + per-view skeletons is the right balance |

## Consequences

### Positive
- Error states match the editorial-monocle aesthetic (quiet, informative)
- Per-view error boundaries prevent total page failure
- Skeleton loading feels polished without CSS animation libraries
- Empty states guide users to next actions

### Negative
- More boilerplate — each view needs skeleton, error, and zero-state components
- Error boundaries add complexity to the component tree
- AsyncData type must be handled in every view component

### Neutral
- Error boundaries can be promoted to Sentry/Raygun integration later
- Zero-state components double as onboarding guidance

## Implementation

**Affected Modules:**
- `.pi/architecture/modules/frontend-app.md`

**Files to Update:**
- `lib/api/types.ts` — AsyncData type
- `lib/api/errors.ts` — ApiError type hierarchy
- `components/shared/ViewSkeleton.tsx` — Generic view skeleton
- `components/shared/ErrorState.tsx` — Consistent error display
- `components/shared/ZeroState.tsx` — Consistent empty state
- `app/error.tsx` — Global error boundary

## Validation

**Validators Required:**
- operations-validator: Verify error handling coverage, loading state rendering, empty state guidance

## References

- Related ADRs: ADR-003 (API Data Fetching — defines AsyncData type), ADR-006 (Component Composition — Server vs Client boundaries)
- Design docs: `design/components.md`, `design/DESIGN.md`

---

*Decision date: 2026-06-13*
*Decision makers: System Architect*
