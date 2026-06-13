# Architecture Decision Record: ADR-003

<!--
Canonical Reference: .pi/architecture/decisions/ADR-003-api-data-fetching.md
Blueprint Source: Guardian Framework v1.2
-->

## Title

API Contract & Data Fetching Strategy

## Status

- [x] Accepted

## Context

The frontend must consume data from the Keystone backend REST API. The backend (Spring Boot, Java) exposes endpoints under `/api/v1/` with snake_case JSON responses. The frontend needs a consistent strategy for:

1. API endpoint discovery and type alignment
2. Data fetching (server-side vs. client-side)
3. Error handling and loading states
4. Type safety and validation

The frontend uses Next.js App Router which supports React Server Components (RSC) for server-side data fetching and client components for interactive features.

**Tech Stack:** TypeScript (Bun/Node), Next.js (App Router)

## Decision

### 1. Shared Type Definitions

We will maintain **hand-authored TypeScript interfaces** that mirror the backend DTOs. These live in `src/lib/api/types.ts` and are organized by bounded context. No code generation — the API surface is small and stable enough to maintain manually, and hand-written types give better control over optional fields and transformations.

All types use **camelCase** (not backend snake_case). A thin transformation layer converts between snake_case API responses and camelCase frontend types.

### 2. Data Fetching Pattern

| Fetch Type | When | Mechanism |
|------------|------|-----------|
| Initial page data | First load / SSR | `fetch()` in Server Component (`app/page.tsx`) |
| View switch | Client navigation | Client-side `fetch()` in a React Query / SWR hook |
| Background refresh | Polling for notifications | `setInterval` with 30s cadence |

### 3. API Client Layer

A thin `apiClient` module wraps `fetch()` with:
- Base URL from `NEXT_PUBLIC_KEYSTONE_API_URL` env var
- Auth token injection (Bearer header, from a configurable source)
- Response validation against expected types
- Error normalization into typed `ApiError` objects
- Request/response logging in development

### 4. Status Handling

Each data fetch returns a discriminated union:

```typescript
type AsyncData<T> =
  | { status: 'loading' }
  | { status: 'success'; data: T }
  | { status: 'error'; error: ApiError }
  | { status: 'empty' };
```

### 5. No GraphQL, No tRPC

The backend is REST-only. The frontend does not introduce additional protocols.

## Alternatives Considered

| Alternative | Pros | Cons | Reason Rejected |
|-------------|------|------|-----------------|
| OpenAPI code generation (openapi-typescript) | Auto-sync with backend, eliminates drift | Generated types can be verbose; overkill for ~15 endpoints; trust in backend stability | Rejected — manual types give better DX for this scale |
| tRPC | End-to-end type safety, no manual types | Requires backend to support tRPC; backend is Java/Spring, not TypeScript | Rejected — incompatible tech stack |
| GraphQL (Apollo) | Flexible queries, single endpoint | Over-engineering for a dashboard that reads aggregate data; adds Apollo client bundle | Rejected — REST is sufficient for read-heavy dashboard |
| Raw fetch without abstraction | Simple, no dependencies | No consistent error handling, no type safety, verbose in every component | Rejected — apiClient abstraction is minimal and provides consistency |

## Consequences

### Positive
- Hand-authored types stay in sync with design/data-schema.md
- Server-side fetching gives fast initial paint for all views
- Consistent error handling across all views
- Thin abstraction layer doesn't lock us into a heavy library
- camelCase types match frontend conventions while backend stays snake_case

### Negative
- Manual type maintenance — must update when backend DTOs change
- No automated drift detection between frontend types and backend DTOs
- Transformation layer adds a small runtime cost

### Neutral
- Can introduce React Query / SWR later if caching needs grow
- Auth token strategy is deferred (cookie vs. header configurable)

## Implementation

**Affected Modules:**
- `.pi/architecture/modules/frontend-app.md`

**Files to Update:**
- `lib/api/client.ts` — fetch wrapper, base URL, auth, error handling
- `lib/api/types.ts` — TypeScript interfaces matching backend DTOs
- `lib/api/transform.ts` — snake_case ↔ camelCase conversion
- `lib/api/endpoints.ts` — endpoint URL constants and fetch functions
- `lib/api/errors.ts` — typed error hierarchy

## Validation

**Validators Required:**
- integration-validator: Verify frontend types match backend DTOs
- operations-validator: Verify error handling, loading states, empty states

## References

- Related ADRs: ADR-002 (Frontend Routing — determines when data is fetched)
- Design docs: `design/data-schema.md`
- Backend endpoints: `src/main/java/com/keystone/*/interfaces/http/*Controller.java`

---

*Decision date: 2026-06-13*
*Decision makers: System Architect*
