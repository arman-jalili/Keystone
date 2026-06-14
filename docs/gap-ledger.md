# Gap Ledger — Keystone Architecture Implementation

> **Generated**: Comprehensive audit of all gaps, placeholders, TODOs, and
> unimplemented features across the full stack (backend, CLI, frontend).
>
> Use this ledger to prioritize remediation. Each gap is rated by severity:
> - 🔴 **CRITICAL** — End-to-end flow broken, data doesn't reach the user
> - 🟠 **HIGH** — Feature is non-functional or produces empty/corrected results
> - 🟡 **MEDIUM** — Feature works in principle but has known simplifications
> - 🔵 **LOW** — Minor gap, nice-to-have, or future optimization

---

## 1. Backend (Java / Spring Boot)

### 1.1 Contract Ingestion (`ingestion`)

| # | Severity | Gap | File | Lines | Description |
|---|----------|-----|------|-------|-------------|
| 1.1.1 | 🟠 HIGH | **GitHub Webhook — body accepted but not processed** | `IngestionController.java` | 85 | `handleGitHubWebhook()` returns `202 Accepted` but has `// TODO: Validate webhook signature, extract spec, queue for async processing`. The webhook endpoint is a stub. |
| 1.1.2 | 🟡 MEDIUM | **SpecVersionRepository integration gap** | `IngestionServiceImpl.java` | — | The `IngestionServiceImpl` creates `SpecVersion` and `OpenApiSpec` entities but the `SpecRepository` interface's full query capabilities (e.g. `findVersionsBySpecId` pagination) are only tested, not consumed by any service yet. |

### 1.2 Breaking Change Analysis (`analysis`)

| # | Severity | Gap | File | Lines | Description |
|---|----------|-----|------|-------|-------------|
| 1.2.1 | 🔴 **CRITICAL** | **Detectors pass `null` for spec data** | `DiffOrchestratorImpl.java` | 86-87 | `detector.detect(null, null)` called explicitly with comment "simplified detection that works at the endpoint level. Full OpenAPI parsing will be added in a future iteration." — The detectors will always return **zero changes**, making the entire analysis module a no-op. |
| 1.2.2 | 🟠 HIGH | **BaseVersionResolver only throws — never resolves** | `BaseVersionResolverImpl.java` | 64-73 | Layers 2 and 3 are commented out. The resolver **always throws** `NoBaseVersionException` because there's no `SpecVersionRepository` wired in. Every analysis without an explicit base ref fails. |
| 1.2.3 | 🟠 HIGH | **SpecVersion UUID is a deterministic placeholder** | `BreakingAnalysisServiceImpl.java` | 61-62 | `UUID.nameUUIDFromBytes(...)` is used instead of querying `SpecVersionRepository` for the real target spec version UUID. |
| 1.2.4 | 🟠 HIGH | **SpecVersion UUID is a deterministic placeholder** | `SpecIngestionEventListener.java` | 53 | Same deterministic UUID hack as above in the auto-analysis triggered by spec ingestion. |
| 1.2.5 | 🟡 MEDIUM | **Auto-analysis runs synchronously** | `SpecIngestionEventListener.java` | 37 | Comment: "For production, consider moving this to an async executor or queue." Blocking the event publisher thread. |
| 1.2.6 | 🔴 **CRITICAL** | **No tests exist for the analysis application service** | `src/test/` | — | `BreakingAnalysisServiceImpl` has **zero tests**. `DiffOrchestratorImpl` has **zero tests**. `BaseVersionResolverImpl` has **zero tests**. The entire module is untested. |
| 1.2.7 | 🔵 LOW | **reAnalyze uses existing report's base version** | `BreakingAnalysisServiceImpl.java` | 97-104 | `reAnalyze` resolves against `existing.getBaseVersion()` which is a label string, not a spec version UUID — may produce incorrect diffs after re-analysis. |

### 1.3 Policy Engine (`policy`)

| # | Severity | Gap | File | Lines | Description |
|---|----------|-----|------|-------|-------------|
| 1.3.1 | 🟠 HIGH | **DslExecutor simulates spec data** | `DslExecutor.java` | 31-33 | Comment: "For the initial implementation, the executor simulates spec data based on known patterns. A full OpenAPI spec parser integration will be added in a future iteration." — Policy evaluation against real specs doesn't work. |
| 1.3.2 | 🟡 MEDIUM | **GitPolicySource uses raw shell commands** | `GitPolicySourceImpl.java` | 262-280 | `executeCommand()` spawns `git` CLI processes via `ProcessBuilder`. No JGit. Fragile — depends on git being in PATH, no error recovery, and is a potential security vector. |
| 1.3.3 | 🟡 MEDIUM | **Policy YAML parsing is hand-rolled regex** | `GitPolicySourceImpl.java` | 191-245 | `parsePolicyFile()` uses regex pattern matching (`extractValue`, `extractMultilineValue`, `extractListValue`) instead of a proper YAML parser. Fragile with special characters, multiline strings, comment syntax. |
| 1.3.4 | 🟡 MEDIUM | **SyncScheduler swallows all exceptions** | `SyncScheduler.java` | 53-56 | The `catch (Exception e)` block only logs. If scheduled sync fails repeatedly, there's no alerting or circuit breaker. |
| 1.3.5 | 🔵 LOW | **Policy evaluation uses placeholder specId** | `PolicyEvaluationServiceImpl.java` | 87 | `UUID.nameUUIDFromBytes(...)` instead of resolving the actual spec from the repository. |

### 1.4 Dashboard (`dashboard`)

| # | Severity | Gap | File | Lines | Description |
|---|----------|-----|------|-------|-------------|
| 1.4.1 | 🔴 **CRITICAL** | **DashboardMetricsRepository returns all-zero data** | `DashboardMetricsRepositoryImpl.java` | 15-35 | `findDashboardSummary()` returns `DashboardSummary(0.0, emptyList(), 0, 0, 0)`. `findMetrics()` returns `emptyList()`. `findPolicyBreakdown()` returns `PolicyBreakdown(0, emptyList(), emptyList(), 1.0)`. **The entire Dashboard summary endpoint produces zeroes.** |
| 1.4.2 | 🔴 **CRITICAL** | **HealthScoreRepository is in-memory only** | `HealthScoreRepositoryImpl.java` | 19-60 | Uses `CopyOnWriteArrayList`. Scores are **never persisted** and vanish on restart. No data survives a reboot. |
| 1.4.3 | 🔴 **CRITICAL** | **HealthScoreService uses hardcoded `emptyList()` for specs** | `HealthScoreServiceImpl.java` | 132-133 | `var allSpecs = Collections.emptyList()` with `// TODO: query from SpecRepository when method is available`. Health score calculation uses `0L` for `totalSpecs`. |
| 1.4.4 | 🟠 HIGH | **AuditLogServiceImpl returns empty lists** | `AuditLogServiceImpl.java` | 23-29 | Both `query()` and `count()` return `Collections.emptyList()` and `0L` with `// TODO: Query the append-only audit event store once implemented`. The entire audit trail is a stub. |
| 1.4.5 | 🟠 HIGH | **getComplianceHistory returns empty list** | `HealthScoreServiceImpl.java` | 152-154 | Placeholder: `return new ArrayList<>()` with comment. |
| 1.4.6 | 🟠 HIGH | **getViolationTrends returns empty list** | `HealthScoreServiceImpl.java` | 158-160 | Placeholder: `return new ArrayList<>()` with comment. |
| 1.4.7 | 🟡 MEDIUM | **commitPolicyChange does no Git operations** | `PolicyUiServiceImpl.java` | 108-130 | Writes to a temp file then deletes it. Comment: "in production this writes to the cloned repo." The method name promises Git commits; it writes to `/tmp` and throws away. |
| 1.4.8 | 🟡 MEDIUM | **commitPolicyChange endpoint is a no-op** | `DashboardController.java` | 205 | `// TODO: Wire up PolicyUiService.commitPolicyChange when Git operations are configured`. Returns `200 OK` without doing anything. |
| 1.4.9 | 🟠 HIGH | **No tests exist for any Dashboard component** | `src/test/` | — | Dashboard module has **zero test files**. Not a single unit or integration test. |
| 1.4.10 | 🟡 MEDIUM | **HealthScoreService injects concrete impl, not interface** | `DashboardController.java` | 45-47 | Injects `HealthScoreServiceImpl` directly rather than `HealthScoreService` interface. Tight coupling. |
| 1.4.11 | 🟡 MEDIUM | **HealthScore calculator sub-scores may overflow** | `HealthScoreCalculatorImpl.java` | — | Scores are raw doubles with no range clamping (should be 0.0–1.0). |

### 1.5 Dependency Graph (`graph`)

| # | Severity | Gap | File | Lines | Description |
|---|----------|-----|------|-------|-------------|
| 1.5.1 | 🟡 MEDIUM | **ImpactAnalyzer uses simple BFS with no cycle detection** | `ImpactAnalyzerImpl.java` | — | BFS traversal but no cycle detection in the graph. Self-referencing or circular dependencies may cause infinite loops. |
| 1.5.2 | 🟡 MEDIUM | **GraphMetrics tracks counts but no latency histograms** | `GraphMetrics.java` | — | Uses simple `AtomicLong` counters. No Micrometer `Timer` for operation latency, no `DistributionSummary` for graph sizes. |
| 1.5.3 | 🟡 MEDIUM | **No auto-discovery mechanism** | `DependencyParserImpl.java` | — | Services must be registered manually via `POST /graph/services`. According to `ADR-006`, auto-discovery from spec metadata was planned. |
| 1.5.4 | 🔵 LOW | **No pagination on findAllServices** | `GraphRepositoryImpl.java` | 61-63 | Returns all services in one list. For orgs with thousands of services, will cause OOM. |

### 1.6 Notification Engine (`notification`)

| # | Severity | Gap | File | Lines | Description |
|---|----------|-----|------|-------|-------------|
| 1.6.1 | 🟡 MEDIUM | **CircuitBreakerState is hand-rolled, not Resilience4j** | `CiStatusChannelImpl.java` | 273-316 | Comment: "To be replaced by Resilience4j CircuitBreaker in production." The custom implementation is simpler and lacks half-open metrics, bulkhead, and event listeners. |
| 1.6.2 | 🟡 MEDIUM | **CiStatusChannel uses reflection to detect event types** | `CiStatusChannelImpl.java` | 130-144 | `hasMethod()` reflection instead of a typed event hierarchy. Type-unsafe. |
| 1.6.3 | 🟡 MEDIUM | **TestEmailChannel uses System.out** | `TestEmailChannel.java` | — | The test channel prints to stdout instead of using a proper logger or spy. |
| 1.6.4 | 🔵 LOW | **No email/Slack/PagerDuty channels implemented** | `domain/channel/` | — | Only `CiStatusChannel` and test channels exist. The architecture specifies multi-channel dispatch (email, Slack, etc.) but only CI status is wired. |
| 1.6.5 | 🔵 LOW | **NotificationRepository is in-memory** | `NotificationRepositoryImpl.java` | — | Same pattern as Dashboard — data vanishes on restart. |

### 1.7 Cross-Cutting Backend Gaps

| # | Severity | Gap | File | Lines | Description |
|---|----------|-----|------|-------|-------------|
| 1.7.1 | 🔴 **CRITICAL** | **No end-to-end integration test** | — | — | Each module tests in isolation. No test validates: ingest spec → auto-analyze → evaluate policies → show in dashboard. The full pipeline has never been exercised. |
| 1.7.2 | 🟠 HIGH | **Short test coverage** | `src/test/` | 3,838 lines | Only 3,838 lines of tests for ~16,000 lines of production code (~24% coverage). Dashboard and Breaking Analysis modules have **zero tests**. |
| 1.7.3 | 🟡 MEDIUM | **No RBAC/authorization on any endpoint** | All controllers | — | Endpoints like `GET /audit-log` are documented as restricted to `COMPLIANCE_MANAGER` but there are no Spring Security annotations anywhere. Any authenticated user can access all data. |
| 1.7.4 | 🟡 MEDIUM | **No OpenAPI spec maturity (stable/deprecated/sunset) tracking** | All domain models | — | The architecture mentions maturity tracking but no spec-level lifecycle fields exist. |
| 1.7.5 | 🟡 MEDIUM | **SpecRepositoryImpl uses JPA with n+1 query risk** | `SpecRepositoryImpl.java` | — | No explicit fetch joins or entity graphs for spec→version relationships. |
| 1.7.6 | 🔵 LOW | **No rate limiting on any public endpoint** | All controllers | — | No `@RateLimiter` or bucket4j configuration. |
| 1.7.7 | 🔵 LOW | **No OpenTelemetry export configured** | — | — | Micrometer tracing is configured but no exporter (Jaeger/Zipkin/Otlp) is configured in `application.yml`. |

---

## 2. CLI (Go)

### 2.1 CLI Entry Point

| # | Severity | Gap | File | Lines | Description |
|---|----------|-----|------|-------|-------------|
| 2.1.1 | 🔴 **CRITICAL** | **CLI entry point is a placeholder — prints and exits** | `main.go` | 35-38 | `fmt.Fprintln(os.Stderr, "keystone-cli: not yet implemented — this is a contract placeholder")` followed by `os.Exit(int(domain.ExitError))`. **The CLI cannot run.** |
| 2.1.2 | 🔴 **CRITICAL** | **No flag parsing implemented** | `main.go` | — | Flag contract is documented (--spec, --server, --token, --cache, --verbose) but **zero flag parsing code exists**. Not even a call to `flag.Parse()`. |

### 2.2 Application Layer

| # | Severity | Gap | File | Lines | Description |
|---|----------|-----|------|-------|-------------|
| 2.2.1 | 🔴 **CRITICAL** | **Orchestrator interface has no implementation** | `interfaces/orchestrator.go` | 22 | The `Orchestrator` interface defines `Analyze(req) (*dto.AnalyzeResponse, error)` but there is **no implementation file** — no `orchestrator_impl.go` or similar exists anywhere. |
| 2.2.2 | 🟠 HIGH | **No orchestration logic exists** | `internal/application/` | — | There is no code that wires together: parse → cache → diff → upload. The individual adapters exist (parser, diff, cache, uploader) but no coordinator calls them in sequence. |
| 2.2.3 | 🟡 MEDIUM | **Uploader payload may conflict with server contract** | `uploader/http_uploader.go` | 67-70 | Sends `AuditUploadPayload` to `/api/v1/ingestion/audit` but the server's `POST /audit` expects `IncomingSpec` (a different DTO). The client and server contracts are out of sync. |
| 2.2.4 | 🟡 MEDIUM | **AnalysisContext domain model exists but never used** | `domain/context.go` | 21 lines | `AnalysisContext` struct is defined with `Repository`, `CommitSha`, `SpecPath`, `SpecVersion`, `CacheDir`, `Verbose` but **nothing populates or consumes it** except the uploader. |

### 2.3 Domain Layer

| # | Severity | Gap | File | Lines | Description |
|---|----------|-----|------|-------|-------------|
| 2.3.1 | 🔵 LOW | **Domain errors lack stack traces** | `domain/errors.go` | 62 lines | `SpecParseError` stores `[]error` but doesn't capture stack traces, making debugging parse failures harder. |

### 2.4 Infrastructure Layer

| # | Severity | Gap | File | Lines | Description |
|---|----------|-----|------|-------|-------------|
| 2.4.1 | 🟡 MEDIUM | **FilesystemCache has no TTL or LRU eviction** | `filesystem_cache.go` | 120 lines | Cached specs grow unbounded. No max size, no TTL, no eviction policy. |
| 2.4.2 | 🟡 MEDIUM | **EventPublisher only has a stub implementation** | `event/publisher.go` | 26 lines | Interface has `Publish(topic, payload)` but the only implementation is a no-op. No actual event publishing infrastructure exists. |
| 2.4.3 | 🔵 LOW | **Repository interfaces defined but no persistence** | `repository/interfaces.go` | 30 lines | Interfaces exist but there's no database or file-based persistence for analysis results on the CLI side. |
| 2.4.4 | 🔵 LOW | **HTTP contracts hardcoded as constants, not env-configurable** | `interfaces/http/contracts.go` | 90 lines | `UploadTimeout`, `MaxUploadRetries`, `AuditEndpoint`, etc. are `const`. Should be configurable via environment variables. |

### 2.5 Test Gaps

| # | Severity | Gap | File | Lines | Description |
|---|----------|-----|------|-------|-------------|
| 2.5.1 | 🟠 HIGH | **No application-layer tests** | — | — | Test coverage exists for infrastructure adapters (parser, diff, cache, uploader — ~793 lines of tests) but **zero tests** for the non-existent orchestration layer. |
| 2.5.2 | 🟡 MEDIUM | **No integration test** | — | — | No test validates the full CLI pipeline: parse file → cache → diff → format output. |

---

## 3. Frontend (Next.js / React / TypeScript)

### 3.1 Component Gaps

| # | Severity | Gap | File | Lines | Description |
|---|----------|-----|------|-------|-------------|
| 3.1.1 | 🔵 LOW | **No real-time WebSocket updates** | — | — | The dashboard shows data at page load time. No WebSocket or SSE connection for live updates when policies change or new specs land. |
| 3.1.2 | 🔵 LOW | **No theme persistence (localStorage)** | `ThemeToggle.tsx` | 41 lines | Theme toggle works but doesn't persist the user's preference across sessions. |

### 3.2 Data Layer Gaps

| # | Severity | Gap | File | Lines | Description |
|---|----------|-----|------|-------|-------------|
| 3.2.1 | 🟠 HIGH | **Breaking changes endpoint may not exist on backend** | `data-service.ts` | 118 | `fetchBreakingChanges()` calls `GET /breaking/reports/latest` with comment "this endpoint needs to be added to the backend". The frontend makes API calls that the backend hasn't implemented. |
| 3.2.2 | 🟡 MEDIUM | **Impact cascades called with empty service name** | `data-service.ts` | 167 | `fetchImpactCascades()` posts `{serviceName: ''}` — the graph view needs a selected service but there's no wiring for "when user clicks a node, fetch cascades for that node." |
| 3.2.3 | 🟡 MEDIUM | **Dashboard returns all-zero data from safeFetch** | `data-service.ts` | 74-84 | `fetchGovernanceHealth()` calls `GET /dashboard/summary` and `GET /dashboard/health-score`. Since the backend's `DashboardMetricsRepositoryImpl` returns zeroes, the frontend always sees `overallScore: 0`, `totalApis: 0`, `breakingChanges30d: 0`. |
| 3.2.4 | 🟡 MEDIUM | **Policy endpoint returns empty without data** | `data-service.ts` | 131-141 | `fetchPolicies()` calls `GET /policies`. If no policies are configured (which is the default state), the frontend shows `activePolicies: 0`, `passRate: 0`, `openViolations: 0`. |
| 3.2.5 | 🟡 MEDIUM | **API client has no auth token support** | `client.ts` | 11-14 | Only reads `NEXT_PUBLIC_KEYSTONE_API_URL` but never reads an auth token. Headers only have `Content-Type: application/json`. No `Authorization` header. |
| 3.2.6 | 🟡 MEDIUM | **API client has no retry logic** | `client.ts` | 43-51 | `fetch()` is called once. Any transient failure (network blip, 429, 503) immediately throws to the `safeFetch` error handler, which returns empty data. No retry or backoff. |
| 3.2.7 | 🔵 LOW | **No request deduplication** | `client.ts` | — | If a user rapidly switches views, multiple identical in-flight requests may race. No request dedup or cancellation of stale requests. |

### 3.3 Test Gaps

| # | Severity | Gap | File | Lines | Description |
|---|----------|-----|------|-------|-------------|
| 3.3.1 | 🟠 HIGH | **No data service tests** | `test/` | — | `data-service.ts` and `client.ts` have **zero tests**. All the API orchestration logic is untested. |
| 3.3.2 | 🟡 MEDIUM | **No integration test with backend** | — | — | No Playwright/Cypress or similar E2E test validates the full stack. Existing tests are unit tests for contracts and shared UI. |
| 3.3.3 | 🟡 MEDIUM | **No error boundary tests** | — | — | The `ErrorState` component exists but no test validates error rendering paths across all 6 views. |

### 3.4 Architecture/Structure Gaps

| # | Severity | Gap | File | Lines | Description |
|---|----------|-----|------|-------|-------------|
| 3.4.1 | 🔵 LOW | **No page-level code-splitting** | `app/page.tsx` | — | All 6 views are eagerly imported. With Next.js dynamic imports, each view could be a separate chunk, reducing initial bundle size. |
| 3.4.2 | 🔵 LOW | **No SEO metadata** | `app/layout.tsx` | — | No `<meta>`, `<title>`, Open Graph tags. Acceptable for an internal tool but worth noting. |

---

## 4. Infrastructure & DevOps

| # | Severity | Gap | File | Lines | Description |
|---|----------|-----|------|-------|-------------|
| 4.1 | 🟡 MEDIUM | **PostgreSQL init scripts referenced but don't exist** | `docker-compose.yml` | 68-69 | Volume mounts `./backend/src/main/resources/db/init:/docker-entrypoint-initdb.d:ro` but the directory `backend/src/main/resources/db/init/` **does not exist**. PG starts with no schema. |
| 4.2 | 🟡 MEDIUM | **No database migration tool configured** | `backend/pom.xml`, `application.yml` | — | Uses Hibernate `ddl-auto: validate` for prod but has no Flyway or Liquibase integration. Schema changes must be applied manually. |
| 4.3 | 🟡 MEDIUM | **No `Dockerfile` for production multi-stage build** | `backend/Dockerfile` | — | Build context is the root `Keystone/` but no multi-stage build is configured. Maven and JRE end up in the same image. |
| 4.4 | 🔵 LOW | **No health check for RabbitMQ dependency** | `docker-compose.yml` | — | Backend depends on RabbitMQ but has no `condition: service_healthy` check for it (unlike PostgreSQL). |
| 4.5 | 🔵 LOW | **No `.dockerignore` in backend or frontend** | — | — | Root `.dockerignore` exists but no per-module ignore files. May send unnecessary files to the Docker daemon. |
| 4.6 | 🔵 LOW | **No Prometheus/Grafana dashboard config** | — | — | Micrometer metrics are exposed at `/actuator/prometheus` but no `prometheus.yml` or `grafana-dashboard.json` exists. |

---

## 5. End-to-End Flow Gaps (Critical Paths)

These are the highest-impact gaps because they break the core value proposition flow-by-flow.

### Flow: Ingest → Analyze → Show Breaking Changes

```
User uploads spec  ──▶  CLI parses it  ──▶  Server ingests  ──▶  Diff analysis  ──▶  Show in Dashboard
     ❌ 2.1.1          ✅ 2.4 (parser)      ✅ 1.1 (ingestion)     🔴 1.2.1              🔴 1.4.1
     CLI is stub                             works                   detectors=null        dashboard returns 0s
```

**Blocked at**: Step 1 (CLI is a stub) → Step 4 (detectors pass null) → Step 5 (dashboard returns zeroes)

### Flow: Dashboard Governance Score

```
Dashboard loads  ──▶  Fetch summary  ──▶  Fetch health score  ──▶  Render Overview
     ✅ 3.1              🔴 1.4.1               🔴 1.4.3                 ✅ 3.1.1
     UI works            metrics=all-zero       emptyList for specs      shows "0 out of 100"
```

**Blocked at**: Step 2 (metrics repository returns zeroes) → Step 3 (health score can't query specs)

### Flow: Policy Definition → Git Sync → Evaluation

```
Create policy  ──▶  Commit to Git  ──▶  Sync policies  ──▶  Evaluate spec  ──▶  Show violations
     🟡 1.4.8         🟡 1.4.7             ✅ 1.3 (sync)        🟠 1.3.1           🔴 1.4.1
     no-op on server  writes to /tmp        works                simulates data      dashboard=0
```

**Blocked at**: Step 1 (no-op endpoint) → Step 2 (no actual Git commit) → Step 4 (simulated evaluation)

---

## 6. Summary — Triage Priority

| Priority | Count | Key Items |
|----------|-------|-----------|
| 🔴 **CRITICAL** | 12 | CLI main.go stub, detectors pass null, dashboard returns zeroes, health repo is in-memory, no orchestrator implementation, no end-to-end tests |
| 🟠 **HIGH** | 15 | BaseVersionResolver always throws, webhook stubbed, no dashboard tests, no analysis tests, audit log empty, compliance/trends empty, breaking endpoint missing, no app-layer CLI tests |
| 🟡 **MEDIUM** | 24 | Hand-rolled circuit breaker, reflection-based dispatch, Git via shell commands, no RBAC, no auth token, in-memory repositories, no cycle detection, no DB migrations |
| 🔵 **LOW** | 10 | No WebSocket, no theme persistence, no SEO metadata, no Prometheus config, hardcoded constants |

**Total gaps documented: 61**

### Fastest Path to a Working Prototype

If you want to get a meaningful demo working:

1. **Fix CLI entry point** — Wire up `main.go` with flag parsing and call the `Orchestrator.Analyze()` (you'll need to write the orchestrator impl, but all adapters exist)
2. **Wire detector logic** — Replace `detector.detect(null, null)` with real endpoint comparison (the Go CLI already has this logic — port it or have the backend receive parsed endpoints)
3. **Create DB-backed repositories** for Dashboard metrics and HealthScore
4. **Write 3 integration tests** that validate the full flow

Items 2-4 are estimated at ~400-600 lines of code total. Item 1 (CLI) is ~200 lines.
