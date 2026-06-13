# Architecture Change Log

<!--
Canonical Reference: .pi/architecture/CHANGELOG.md
Blueprint Source: Guardian Framework v1.2
DO NOT EDIT GENERATED FILES - Modify this source only
-->

This document tracks all architecture changes requiring implementation updates.

---

## Change Log Format

Each entry follows this structure:

```markdown
## [YYYY-MM-DD] - [Change Title]

### Changed
- Module: [module-name]
  - [Component]: [what changed]
  - [Component]: [what changed]

### Impact Analysis
- Files affected:
  - src/[path1]
  - src/[path2]
- Canonical refs to update:
  - .pi/architecture/modules/[module].md#[section]
- Validators required:
  - [validator-name]

### Migration Steps
1. [Step 1]
2. [Step 2]
3. [Step 3]

### Status
- [ ] Architecture doc updated
- [ ] CHANGELOG entry added
- [ ] Implementation updated
- [ ] Canonical refs updated
- [ ] Validators run
```

---

## Entries

## 2026-06-13 - Initial Architecture Scaffold

### Changed
- Module: frontend-app
  - All components: Created as single frontend module replacing 6 incorrect backend-mirror modules
  - Architecture: Layered model (View → Shared UI → Layout → Data layer)
  - Components: 19 UI components across 4 layers
  - Data fetching: 11 backend endpoints mapped to 6 views
- Module: (archived) contract-ingestion, breaking-change-analysis, policy-engine, dependency-graph, dashboard, notification-engine
  - Removed as frontend modules — these are backend data sources, not frontend modules
  - The frontend is a read-heavy dashboard that queries these via REST API, not a reimplementation
- Session: 25c57121-7a91-47ce-ae20-5c561181984d
  - Domain exploration confirmed: 6 bounded contexts describe the backend, not the frontend
  - Frontend uses the design specification from `design/` folder (DESIGN.md, tokens.css, components.md, data-schema.md, routes.md, build-instructions.md)

## 2026-06-13 - Design Integration

### Changed
- Module: frontend-app
  - Layout: 232px NavRail + 56px TopBar + scrollable content area
  - Theme: CSS custom properties (OKLch), light/dark, localStorage persistence, FOUC prevention
  - Typography: Serif display + sans body + mono labels (system font stacks, no external fonts)
  - Styling: Sharp corners, no box-shadows, no icon library, mono text abbreviations
  - Components: StatGrid, DataTable, ScoreRing, Pill, RuleCard, DiffBlock, BreakageCard, DependencyGraph, etc.
  - Error handling: AsyncData type, per-view error boundaries, ViewSkeleton, ZeroState
  - Data schema: 10 TypeScript interfaces matching backend DTOs with snake_case→camelCase transform

### Impact Analysis
- Files affected:
  - `.pi/domain/exploration.md` — Domain analysis with actors, FRs, NFRs, assumptions, entities, events
  - `.pi/domain/ubiquitous-language.md` — 32 canonical glossary terms
  - `.pi/architecture/modules/frontend-app.md` — Single frontend module (REPLACES 6 backend-mirror modules)
  - `.pi/architecture/decisions/ADR-001-architecture-pattern.md` — Updated to reference frontend-app.md, removed incorrect module listings
  - `.pi/architecture/decisions/ADR-002-frontend-routing.md` — SPA with SearchParam Routing
  - `.pi/architecture/decisions/ADR-003-api-data-fetching.md` — API Contract & Data Fetching Strategy
  - `.pi/architecture/decisions/ADR-004-theme-design-system.md` — Theme & Design System Strategy
  - `.pi/architecture/decisions/ADR-005-domain-events.md` — Cross-Context Communication via Domain Events
  - `.pi/architecture/decisions/ADR-006-component-composition.md` — Server Components with Client Islands
  - `.pi/architecture/decisions/ADR-007-error-loading-states.md` — Error Handling, Loading & Empty States
  - `.pi/architecture/diagrams/system-context.md` — C4 System Context, Event Flow, Data Flow, Layout diagrams
  - `.pi/architecture/diagrams/data-flow.md` — Sequence diagrams for 6 key flows + component data dependencies
  - `.pi/architecture/diagrams/system-overview.md` — Updated to Keystone-specific architecture (replaced generic Guardian template)

- Canonical refs to update:
  - `.pi/architecture/modules/frontend-app.md#architecture-layer-model`
  - `.pi/architecture/modules/frontend-app.md#views-and-backend-data-sources`
  - `.pi/architecture/modules/frontend-app.md#components`
  - `.pi/architecture/modules/frontend-app.md#theme-architecture`

- Validators required:
  - architecture-validator: Verify frontend module boundary (no domain logic duplication from backend)
  - security-validator: Verify no frontend data storage, token passthrough only
  - integration-validator: Verify API contracts between frontend views and backend endpoints

### Migration Steps
1. Review `design/` folder (DESIGN.md, tokens.css, components.md, data-schema.md, routes.md, build-instructions.md) — these are the source of truth for frontend implementation
2. Follow build-instructions.md step-by-step build order:
   a. Scaffold Next.js project
   b. Copy tokens.css → globals.css
   c. Copy tailwind.config.ts
   d. Build layout shell (AppLayout, NavRail, TopBar)
   e. Build shared components (StatGrid, DataTable, Pill, ViewShell, TwoCol)
   f. Build view-specific components (ScoreRing, DimensionBar, RuleCard, etc.)
   g. Build app/page.tsx with ?view= routing
   h. Wire theme toggle
3. Implement data layer (lib/api/client.ts, types.ts, transform.ts, endpoints.ts, errors.ts)
4. Implement error handling (error boundaries, skeletons, zero-states)
5. Run visual QA checklist from build-instructions.md

### Architecture Decision
**The frontend does NOT implement backend bounded contexts.** It is a single bounded context (Frontend App) that reads from 6 backend data sources:

| Backend Context | Frontend Role | Module File |
|----------------|--------------|-------------|
| Contract Ingestion | Read-only: API Inventory view | ← backend data source |
| Breaking Change Analysis | Read-only: Breaking Changes view | ← backend data source |
| Policy Engine | CRUD via API: Policy Compliance view | ← backend data source |
| Dependency Graph | Read-only: Dependency Graph view | ← backend data source |
| Dashboard | Composed from multiple sources: Overview view | ← backend data source |
| Notification Engine | Polling read-only: Notifications view | ← backend data source |

### Status
- [x] Architecture doc updated — frontend-app.md created, 6 backend modules archived
- [x] CHANGELOG entry added — this entry
- [ ] Implementation updated — greenfield (pending)
- [x] Canonical refs updated — all ADRs reference frontend-app.md
- [ ] Validators run — pending implementation

---

## Template Usage

When making architecture changes:

1. **Before change**: Review existing architecture docs
2. **During change**: Update `.pi/architecture/modules/[module].md`
3. **After change**: Add entry to this CHANGELOG
4. **Implementation**: Follow migration steps, update canonical refs
5. **Validation**: Run `validate-canonical.sh` to verify sync

---

## Architecture Sync Status

Track which changes have been synced to implementation:

| Date | Change | Module | Sync Status | Validator Status |
|------|--------|--------|-------------|------------------|
| 2026-06-13 | Initial Architecture Scaffold | all | pending | pending |
| | | | | |

---

*Last updated: 2026-06-13*
*Framework version: 1.2.0*
