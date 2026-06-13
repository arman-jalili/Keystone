# Architecture Decision Record: ADR-008

<!--
Canonical Reference: .pi/architecture/decisions/ADR-008-frontend-module-boundary.md
Blueprint Source: Guardian Framework v1.2
-->

## Title

Frontend Module Boundary — No Backend Domain Duplication

## Status

- [x] Accepted

## Context

The initial architecture scaffold created 6 module docs in `.pi/architecture/modules/` that mirrored the backend's bounded contexts (Contract Ingestion, Breaking Change Analysis, Policy Engine, Dependency Graph, Dashboard, Notification Engine). This was a misinterpretation of the domain exploration — it assumed the frontend needed to implement these same domains.

In reality, the frontend is a **read-heavy dashboard** that:
- Does NOT ingest or store OpenAPI specs (CLI/CI does that)
- Does NOT perform diff analysis (server-side compute)
- Does NOT evaluate policy DSL (server-side only)
- Does NOT compute BFS graph traversal (server-side)
- Does NOT dispatch notifications (server-side event handlers)
- Does NOT calculate health scores (server-side aggregation)

The frontend's only domain logic is: **fetch data, render it, handle view switching, and persist theme preference.**

**Tech Stack:** TypeScript (Bun/Node), Next.js (App Router), Tailwind CSS

## Decision

The frontend is a **single bounded context** (`frontend-app`) with a layered architecture:

```
View Layer       — 6 views (Overview, Inventory, Breaking, Policy, Graph, Notifications)
Shared UI Layer  — StatGrid, DataTable, Pill, ScoreRing, etc.
Layout Layer     — AppLayout, NavRail, TopBar, Theme
Data Layer       — API client, types, transformation, endpoints
```

The backend's 6 bounded contexts are treated as **external data sources** — the frontend reads from them via REST API. There is:

- **No frontend module** for Contract Ingestion
- **No frontend module** for Breaking Change Analysis
- **No frontend module** for Policy Engine
- **No frontend module** for Dependency Graph
- **No frontend module** for Notification Engine

The Dashboard context exists only as a backend aggregation service — the frontend has `components/overview/` components that render dashboard data.

**Architecture Rule:** A frontend module should only be created if it implements domain logic that lives in the browser. Pure data display does not warrant a domain module.

## Alternatives Considered

| Alternative | Pros | Cons | Reason Rejected |
|-------------|------|------|-----------------|
| Mirror all 6 backend contexts as frontend modules | Symmetrical architecture, easy mapping | Misleading — implies frontend implements domain logic; creates empty modules with no actual domain code | Rejected — violates YAGNI; code organization should reflect actual responsibilities |
| Create frontend modules per API endpoint | Granular, clear data source mapping | Over-engineered for 11 endpoints; view-based organization is simpler | Rejected — view-based grouping is more natural for a dashboard |
| No module organization at all | Simplest, minimal overhead | No architectural intent; hard to enforce boundaries as app grows | Rejected — even a thin layer model helps with consistency |
| Create frontend modules per user role | Role-aware UI organization | Roles are backend-enforced; frontend role awareness would duplicate RBAC logic | Rejected — roles are a backend concern; frontend conditionally shows/hides based on API response |

## Consequences

### Positive
- Clear architecture boundary: frontend is a **view layer**, not a backend reimplementation
- No empty modules with no domain code
- Easier onboarding — developers understand "the frontend fetches and renders"
- Design system is the real source of truth for the frontend architecture
- ADRs and module docs are consistent with actual responsibilities

### Negative
- Backend context docs must be maintained in the backend project (`~/project/Keystone/backend/.pi/`)
- If the frontend ever needs to implement domain logic (e.g., client-side caching, offline support), a new module should be created
- The view-based structure may need reorganization if the number of views grows significantly (10+)

### Neutral
- The `design/` folder serves as the primary frontend specification — module docs reference it
- Backend data source documentation lives in the backend's `.pi/` directory

## Validation

**Validators Required:**
- architecture-validator: Verify no frontend module duplicates backend domain logic
- integration-validator: Verify frontend only calls backend endpoints, doesn't reimplement them

### Checklist
- [ ] No TypeScript file in the frontend contains domain logic from the backend
- [ ] No frontend module directory duplicates a backend bounded context name
- [ ] All data fetching goes through `lib/api/` — no direct domain model manipulation
- [ ] Design tokens and component catalog in `design/` are the source of truth for UI

## References

- Related ADRs: ADR-001 (DDD with Bounded Contexts — clarified frontend applies differently)
- Module docs: `.pi/architecture/modules/frontend-app.md`
- Design system: `design/DESIGN.md`, `design/components.md`, `design/data-schema.md`
- Backend architecture: `~/project/Keystone/backend/.pi/architecture/modules/`

---

*Decision date: 2026-06-13*
*Decision makers: System Architect*
