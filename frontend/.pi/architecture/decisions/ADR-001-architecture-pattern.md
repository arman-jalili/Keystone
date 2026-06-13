# Architecture Decision Record: ADR-001

<!--
Canonical Reference: .pi/architecture/decisions/ADR-001-architecture-pattern.md
Blueprint Source: Guardian Framework v1.2
-->

## Title

Domain-Driven Design with Bounded Contexts (Modular Monolith)

## Status

- [x] Accepted

## Context

The domain exploration (session 25c57121-7a91-47ce-ae20-5c561181984d) identified six distinct business capabilities that must be implemented as independently evolvable modules:

1. **Contract Ingestion** — Ingest, parse, version, and deduplicate OpenAPI specs
2. **Breaking Change Analysis** — Diff spec versions, classify changes, produce verdicts
3. **Policy Engine** — Manage policy lifecycle, evaluate specs against DSL rules, handle exemptions
4. **Dependency Graph** — Register services, track dependency edges, compute blast radius
5. **Dashboard** — Aggregate governance health, compliance, audit logs, violation trends
6. **Notification Engine** — Dispatch notifications across channels, manage delivery status

These capabilities have clear domain boundaries, distinct data ownership, and communicate through events. A naive layered architecture would blur these boundaries and create coupling.

**Tech Stack:** TypeScript (Bun/Node), Next.js (App Router)

## Decision

We will use **Domain-Driven Design** with **bounded contexts** implemented as a **Modular Monolith** — independently evolvable modules within a single deployable, with strict dependency rules and event-driven cross-context communication.

### Module Structure Rules

- Each bounded context is a directory under `src/modules/[context]/`
- Layered within each context: `domain/`, `application/`, `infrastructure/`, `interfaces/`
- A context may only depend on another context's **domain events** (not domain models or services)
- Cross-context data queries must go through an anti-corruption layer (ACL)

### Communication

- Synchronous: REST API calls from frontend to backend (`/api/v1/*`)
- Asynchronous: Domain events via an in-process event bus (can be promoted to message queue later)
- Frontend-to-backend: HTTP only (no direct module access from frontend)

## Alternatives Considered

| Alternative | Pros | Cons | Reason Rejected |
|-------------|------|------|-----------------|
| Monolith without domain boundaries | Simple to start, fast to build | No separation of concerns, tight coupling, hard to evolve | Rejected — domain has clear boundaries that would erode without enforcement |
| Microservices from day one | Independent deployability, team autonomy | Operational complexity, distributed transactions, latency, over-engineering for current scale | Rejected — premature complexity for a single-team project |
| Layered Architecture (presentation/domain/infrastructure) | Well-known, simple to understand | Does not enforce domain boundaries; changes to one domain leak into others | Rejected — the domain exploration shows distinct bounded contexts that need isolation |
| Clean Architecture (hexagonal) | Strong isolation, testable | Boilerplate-heavy for frontend; better suited to backend services | Rejected — over-engineered for a frontend application; Modular Monolith with DDD layers is sufficient |

## Consequences

### Positive
- Each bounded context can be developed, tested, and refactored independently
- Domain boundaries provide natural team alignment and ownership
- Event-driven communication prevents coupling while enabling reactive features (e.g., auto-trigger breaking analysis on spec ingestion)
- Modular Monolith can be split into microservices later if needed without redesign
- Ubiquitous language is enforced within each context, reducing ambiguity

### Negative
- Requires disciplined dependency management — no shortcuts across context boundaries
- Eventual consistency for cross-context data (events are async)
- Initial overhead of defining interfaces and events before implementation
- Developers must understand bounded context boundaries — learning curve

### Neutral
- Domain events will be in-process initially; may need promotion to message queue for reliability
- Some contexts (Dashboard) read from multiple contexts — ACL pattern required

## Implementation

**Affected Modules:**
- `.pi/architecture/modules/frontend-app.md` — Frontend application structure

Note: The backend bounded contexts (Contract Ingestion, Breaking Change Analysis, Policy Engine, Dependency Graph, Dashboard, Notification Engine) are **data sources** consumed by the frontend, not frontend modules. The frontend has a single bounded context organized by view layer and UI components, not backend domain duplication. See `.pi/architecture/modules/frontend-app.md#architecture-layer-model` for details.

**Files to Update:**
- `app/page.tsx` — Root server component with view switching
- `app/layout.tsx` — AppLayout shell (NavRail + TopBar + content area)
- `app/globals.css` — Design tokens from design/tokens.css
- `components/layout/` — NavRail, TopBar, AppLayout
- `components/shared/` — StatGrid, DataTable, Pill, etc.
- `components/views/` — One directory per view
- `lib/api/` — API client, types, transform, endpoints

**Canonical References:**
Implementation files should reference: `.pi/architecture/modules/frontend-app.md

## Validation

**Validators Required:**
- architecture-validator: Verify module boundaries, dependency direction, no cross-context domain model leakage
- integration-validator: Verify event contracts between contexts

## References

- Related ADRs: None (first ADR)
- External references: "Domain-Driven Design" (Evans), "Implementing Domain-Driven Design" (Vernon)
- Domain exploration: `.pi/domain/exploration.md`

---

*Decision date: 2026-06-13*
*Decision makers: System Architect*
