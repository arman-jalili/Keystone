# Gap Ledger — Frontend (Next.js / React / TypeScript)

> **Audit of all gaps, placeholders, TODOs, and unimplemented features**
> in the Keystone frontend.
>
> Severity key:
> - 🔴 **CRITICAL** — End-to-end flow broken, data doesn't reach the user
> - 🟠 **HIGH** — Feature is non-functional or produces empty/corrected results
> - 🟡 **MEDIUM** — Feature works in principle but has known simplifications
> - 🔵 **LOW** — Minor gap, nice-to-have, or future optimization

---

## 1. Data Layer Gaps

### 1.1 API Client (`lib/api/client.ts`)

| # | Severity | Gap | Lines | Detail |
|---|----------|-----|-------|--------|
| 1.1.1 | 🟡 MEDIUM | **No authentication/authorization** | 11-14 | `apiClient` only sends `Content-Type: application/json`. No `Authorization: Bearer <token>` header is ever set. All backend calls are unauthenticated. |
| 1.1.2 | 🟡 MEDIUM | **No retry logic for transient failures** | 43-51 | `fetch()` is called once. Any network blip, 429 (rate limit), or 503 (service unavailable) immediately throws to the `safeFetch` error handler, which returns empty fallback data. No retry with backoff. |
| 1.1.3 | 🟡 MEDIUM | **No request deduplication** | — | If a user rapidly switches between views, multiple identical in-flight requests may compete. No request dedup or cancellation of stale in-flight requests. |
| 1.1.4 | 🔵 LOW | **Base URL could race during hydration** | 13-15 | `BASE_URL` reads from `window.__NEXT_PUBLIC_KEYSTONE_API_URL` on the client but falls back to env var on the server. During SSR hydration, the server-rendered URL and client-rendered URL could differ. |

### 1.2 Data Service (`lib/api/data-service.ts`)

| # | Severity | Gap | Lines | Detail |
|---|----------|-----|-------|--------|
| 1.2.1 | 🟠 HIGH | **Breaking changes endpoint may not exist on backend** | 118 | `fetchBreakingChanges()` calls `GET /breaking/reports/latest` with comment "this endpoint needs to be added to the backend." The frontend makes API calls to an endpoint that hasn't been implemented on the server. |

#### All Data Flows — Known Failure Modes

| Flow | Backend Called | Backend State | Frontend Renders |
|------|---------------|---------------|------------------|
| **Governance Health** | `GET /dashboard/summary` + `GET /dashboard/health-score` | Returns hardcoded zeroes (see backend gap 4.1.1) | `overallScore: 0`, `totalApis: 0`, `breakingChanges30d: 0` |
| **API Inventory** | `GET /ingestion/apis` | ✅ Works (real data) | ✅ Works correctly |
| **Stale APIs** | `GET /ingestion/apis/stale` | ✅ Works (real data) | ✅ Works correctly |
| **Breaking Changes** | `GET /breaking/reports/latest` | 🔴 Endpoint may not exist | Falls back to `{total30d:0, items:[]}` |
| **Policies** | `GET /policies` | ✅ Works (but empty by default) | `activePolicies: 0`, `passRate: 0` |
| **Dependency Graph** | `GET /graph/services` | ✅ Works (real data) | ✅ Works (nodes render, edges may be empty) |
| **Impact Cascades** | `POST /graph/impact` | Sends `{serviceName: ''}` | Always returns empty cascades |
| **Notifications** | `GET /notifications` | ✅ Works (real data) | ✅ Works correctly |

| # | Severity | Gap | Lines | Detail |
|---|----------|-----|-------|--------|
| 1.2.2 | 🟡 MEDIUM | **Impact cascades called with empty service name** | 167 | `fetchImpactCascades()` posts `{serviceName: ''}`. The graph view needs a selected service, but there's no "user clicks a node → fetch cascades for that node" wiring. |
| 1.2.3 | 🟡 MEDIUM | **Dashboard always shows zeroes due to backend gaps** | 74-84 | `fetchGovernanceHealth()` composes two backend calls. Both return zeroes (backend gaps 4.1.1, 4.3.1). The frontend rendering is correct but the data is meaningless. |
| 1.2.4 | 🟡 MEDIUM | **Policy page shows empty state by default** | 131-141 | `fetchPolicies()` calls `GET /policies`. With no policies configured (default), the UI shows `activePolicies: 0`, `passRate: 0`, `openViolations: 0`. No onboarding/zero-state guidance. |
| 1.2.5 | 🔵 LOW | **All fetches use `Promise.allSettled` but only use first value** | 89 | `fetchGovernanceHealth()` calls `Promise.allSettled([summary, healthScore])` but heavily weights `healthScore` results over `summary`. If `summary` succeeds and `healthScore` fails, the overall score is 0. |

---

## 2. Component Gaps

### 2.1 Inventory View

**File**: `frontend/components/inventory/InventoryView.tsx`

| # | Severity | Gap | Lines | Detail |
|---|----------|-----|-------|--------|
| 2.1.1 | 🔵 LOW | **No health trend sparkline per API** | — | Each inventory row shows `health` as a label. No mini trend chart showing health over time. |

### 2.2 Breaking View

**File**: `frontend/components/breaking/BreakingView.tsx`

| # | Severity | Gap | Lines | Detail |
|---|----------|-----|-------|--------|
| 2.2.1 | 🟡 MEDIUM | **Breaking view depends on non-existent endpoint** | — | The component expects `BreakingChangeSummary` data from `fetchBreakingChanges()` which calls a backend endpoint that may not exist. When data is empty, the view shows "no changes" — indistinguishable from "there was an error." |

### 2.3 Graph View

**File**: `frontend/components/graph/GraphView.tsx`

| # | Severity | Gap | Lines | Detail |
|---|----------|-----|-------|--------|
| 2.3.1 | 🟡 MEDIUM | **No interactive node selection for impact analysis** | — | The graph renders service nodes and edges but clicking a node doesn't trigger `fetchImpactCascades(serviceName)`. The impact analysis feature is visually present but functionally disconnected. |
| 2.3.2 | 🔵 LOW | **Nodes have fixed positions (x: 0, y: 0)** | — | `fetchDependencyGraph()` returns `{x: 0, y: 0}` for all nodes. No force-directed layout or position calculation. All nodes render on top of each other unless the component implements its own layout. |

### 2.4 Notifications View

**File**: `frontend/components/notifications/NotificationsView.tsx`

| # | Severity | Gap | Lines | Detail |
|---|----------|-----|-------|--------|
| 2.4.1 | 🔵 LOW | **No real-time updates** | — | Notifications are fetched on page load. No WebSocket or polling for new notifications arriving after page render. |

### 2.5 Layout

**File**: `frontend/components/layout/`

| # | Severity | Gap | Lines | Detail |
|---|----------|-----|-------|--------|
| 2.5.1 | 🔵 LOW | **Theme preference not persisted** | `ThemeToggle.tsx` | Theme toggle works for the current session but doesn't save to `localStorage`. The theme resets on page reload. |

### 2.6 Breaking View (detail)

| # | Severity | Gap | Lines | Detail |
|---|----------|-----|-------|--------|
| 2.6.1 | 🟡 MEDIUM | **No detail expansion or drill-down** | — | Breaking changes are listed in a flat table. Clicking a change doesn't expand to show full `diffText`, `impactedConsumers`, or `versionFrom`→`versionTo` comparison. |

---

## 3. Shared Component Gaps

### 3.1 DataTable

**File**: `frontend/components/shared/DataTable.tsx`

| # | Severity | Gap | Lines | Detail |
|---|----------|-----|-------|--------|
| 3.1.1 | 🔵 LOW | **No client-side sorting** | — | Table renders data in API order. Users can't click headers to sort by column. |
| 3.1.2 | 🔵 LOW | **No pagination** | — | All rows rendered at once. With 500+ APIs, the DOM will be large. |

### 3.2 ViewShell

**File**: `frontend/components/shared/ViewShell.tsx`

| # | Severity | Gap | Lines | Detail |
|---|----------|-----|-------|--------|
| 3.2.1 | 🔵 LOW | **Not used by any view** | 23 | `ViewShell` is defined but no view imports or wraps it. The layout is handled directly in `page.tsx`. Either dead code or intended for future use. |

---

## 4. Page-Level Gaps

### 4.1 Root Page (`app/page.tsx`)

| # | Severity | Gap | Lines | Detail |
|---|----------|-----|-------|--------|
| 4.1.1 | 🔵 LOW | **No code-splitting — all views eagerly imported** | 8-20 | All 6 view components are imported at the top level. Every view's code is included in the initial bundle. With Next.js dynamic imports, each view could be its own chunk. |
| 4.1.2 | 🔵 LOW | **No SEO metadata** | — | No `<title>`, `<meta>`, or Open Graph tags. Acceptable for an internal admin tool but worth noting. |
| 4.1.3 | 🔵 LOW | **URL state only tracks view** | 44-46 | The `?view=` parameter controls which view is shown. No URL-encoded filters, search terms, or pagination state. Deep linking to specific data is limited. |

### 4.2 Root Layout (`app/layout.tsx`)

| # | Severity | Gap | Lines | Detail |
|---|----------|-----|-------|--------|
| 4.2.1 | 🔵 LOW | **No global error boundary** | — | No `error.tsx` or `global-error.tsx` for the Next.js App Router. Unhandled render errors show a white screen. |

---

## 5. Type System Gaps

### 5.1 Contracts (`lib/contracts/types.ts`)

| # | Severity | Gap | Lines | Detail |
|---|----------|-----|-------|--------|
| 5.1.1 | 🟡 MEDIUM | **`recentBreakages` and `topViolations` always empty** | — | `GovernanceHealth.recentBreakages` and `.topViolations` are arrays but the data service always populates them as `[]` because the backend dashboard endpoints return zeroes. The frontend types are correct but no data ever flows through. |
| 5.1.2 | 🔵 LOW | **No discriminated unions for view states** | — | Each view receives `data?: SomeType`. No discriminated union for loading/success/error states at the component prop level (orchestrated in `page.tsx` instead). |

### 5.2 View Config (`lib/contracts/view-config.ts`)

| # | Severity | Gap | Lines | Detail |
|---|----------|-----|-------|--------|
| 5.2.1 | 🔵 LOW | **No view-level authorization** | — | All 6 views are available to any user. No role-based filtering of which views are accessible. |

---

## 6. Test Gaps

| # | Severity | Gap | Files | Lines | Detail |
|---|----------|-----|-------|-------|--------|
| 6.1.1 | 🟠 HIGH | **No data service tests** | 0 | — | `data-service.ts` (321 lines) and `client.ts` (~100 lines) have **zero tests**. All API orchestration, error handling, and data transformation logic is untested. |
| 6.1.2 | 🟡 MEDIUM | **No E2E or integration tests** | — | — | No Playwright/Cypress tests validate the full stack. The existing 4 test files (`contracts.test.ts`, `shared-ui.test.tsx`, `view-components.test.tsx`, `setup.ts`) are unit tests that test components in isolation with mock data. |
| 6.1.3 | 🟡 MEDIUM | **No error boundary tests** | — | — | `ErrorState` component exists but no test validates error rendering paths across all 6 views for different failure modes. |

### Existing Test Coverage (for reference)

| Test File | Lines | Covers |
|-----------|-------|--------|
| `test/contracts.test.ts` | — | Type contract validation |
| `test/shared-ui.test.tsx` | — | DataTable, StatGrid rendering |
| `test/view-components.test.tsx` | — | View component rendering with mock data |
| `test/setup.ts` | — | Test environment configuration |

---

## 7. Build & Configuration Gaps

| # | Severity | Gap | Files | Detail |
|---|----------|-----|-------|--------|
| 7.1 | 🔵 LOW | **No environment-specific configs** | — | Only `.env` (or env vars at runtime). No `env.development`, `env.staging`, `env.production` with different API URLs, logging levels, or feature flags. |
| 7.2 | 🔵 LOW | **No bundle analysis configured** | `next.config.ts` | No `@next/bundle-analyzer` configured. Hard to identify large dependencies or code-splitting opportunities. |
| 7.3 | 🔵 LOW | **No PWA support** | — | No service worker, offline support, or manifest.json. The dashboard requires network connectivity. |

---

## 8. Gap Count

| Severity | Count |
|----------|-------|
| 🔴 **CRITICAL** | 0 |
| 🟠 **HIGH** | 2 |
| 🟡 **MEDIUM** | 11 |
| 🔵 **LOW** | 17 |
| **Total** | **30** |
