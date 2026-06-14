# Gap Ledger — Backend (Java / Spring Boot)

> **Audit of all gaps, placeholders, TODOs, and unimplemented features**
> in the Keystone backend Java module.
>
> Severity key:
> - 🔴 **CRITICAL** — End-to-end flow broken, data doesn't reach the user
> - 🟠 **HIGH** — Feature is non-functional or produces empty/corrected results
> - 🟡 **MEDIUM** — Feature works in principle but has known simplifications
> - 🔵 **LOW** — Minor gap, nice-to-have, or future optimization

---

## 1. Contract Ingestion (`ingestion`)

### 1.1 IngestionController

**File**: `backend/src/main/java/com/keystone/ingestion/interfaces/http/IngestionController.java`

| # | Severity | Gap | Lines | Detail |
|---|----------|-----|-------|--------|
| 1.1.1 | 🟠 HIGH | **GitHub Webhook accepted but not processed** | 85 | `handleGitHubWebhook()` returns `202 Accepted` but has `// TODO: Validate webhook signature, extract spec, queue for async processing`. The webhook endpoint accepts the payload then discards it. |
| 1.1.2 | 🟡 MEDIUM | **Webhook response type is a record** | 132 | `WebhookAcceptedResponse` is a simple `record boolean accepted, UUID deliveryId` — no webhook delivery tracking or replay capability is wired. |

### 1.2 IngestionServiceImpl

**File**: `backend/src/main/java/com/keystone/ingestion/application/service/IngestionServiceImpl.java`

| # | Severity | Gap | Lines | Detail |
|---|----------|-----|-------|--------|
| 1.2.1 | 🟡 MEDIUM | **SpecVersionRepository not consumed by services** | — | `SpecVersion` entities are created but the full query surface (`findVersionsBySpecId` with pagination) is never used outside tests. Downstream consumers (analysis, dashboard) can't query spec version history. |
| 1.2.2 | 🔵 LOW | **No async processing queue configured** | — | Spec ingestion runs synchronously. For webhook-triggered ingestion at scale, this should be backed by RabbitMQ (which is already configured in the project). |

---

## 2. Breaking Change Analysis (`analysis`)

### 2.1 DiffOrchestratorImpl

**File**: `backend/src/main/java/com/keystone/analysis/domain/service/impl/DiffOrchestratorImpl.java`

| # | Severity | Gap | Lines | Detail |
|---|----------|-----|-------|--------|
| 2.1.1 | 🔴 **CRITICAL** | **Detectors called with `null` spec data** | 86-87 | `detector.detect(null, null)` is called with comment "simplified detection that works at the endpoint level. Full OpenAPI parsing will be added in a future iteration." **All detectors return zero changes**, making the entire analysis module a no-op. |
| 2.1.2 | 🟠 HIGH | **No endpoint parsing wired before detectors run** | — | The method signature accepts spec data but the calling code never parses or passes real OpenAPI content. The pipeline orchestration is structurally complete but data never flows in. |

### 2.2 BaseVersionResolverImpl

**File**: `backend/src/main/java/com/keystone/analysis/domain/service/impl/BaseVersionResolverImpl.java`

| # | Severity | Gap | Lines | Detail |
|---|----------|-----|-------|--------|
| 2.2.1 | 🟠 HIGH | **Layer 2 and 3 fallbacks are unimplemented** | 64-73 | The resolver should fall through three layers: (1) explicit base ref, (2) previous ingested version via `SpecVersionRepository`, (3) latest version on main branch. Layers 2 and 3 are commented out with "In a real implementation, this would..." **Without explicit base ref, the resolver always throws `NoBaseVersionException`.** |
| 2.2.2 | 🟡 MEDIUM | **No SpecVersionRepository dependency injected** | — | The resolver has no repository dependency. It cannot query for previous versions, making auto-resolution impossible. |

### 2.3 BreakingAnalysisServiceImpl

**File**: `backend/src/main/java/com/keystone/analysis/application/service/BreakingAnalysisServiceImpl.java`

| # | Severity | Gap | Lines | Detail |
|---|----------|-----|-------|--------|
| 2.3.1 | 🟠 HIGH | **SpecVersion UUID is a deterministic hash** | 61-62 | `UUID.nameUUIDFromBytes(...)` generates a deterministic UUID from repository+path+sha instead of querying `SpecVersionRepository` for the real spec version UUID. This UUID won't match any persisted `SpecVersion` entity. |
| 2.3.2 | 🟡 MEDIUM | **No async execution** | — | Analysis runs synchronously on the request thread. Large specs may cause HTTP timeouts. |

### 2.4 SpecIngestionEventListener

**File**: `backend/src/main/java/com/keystone/analysis/infrastructure/event/SpecIngestionEventListener.java`

| # | Severity | Gap | Lines | Detail |
|---|----------|-----|-------|--------|
| 2.4.1 | 🟠 HIGH | **Same deterministic UUID hack** | 53 | `UUID.nameUUIDFromBytes(...)` instead of retrieving the actual `SpecVersion` by ID from the ingestion event. |
| 2.4.2 | 🟡 MEDIUM | **Auto-analysis runs synchronously** | 37 | Comment: "For production, consider moving this to an async executor or queue." Blocks the event publisher thread, potentially delaying other listeners. |

### 2.5 Detector Implementations

**Files**: `backend/src/main/java/com/keystone/analysis/domain/detector/impl/*.java`

| # | Severity | Gap | Lines | Detail |
|---|----------|-----|-------|--------|
| 2.5.1 | 🟡 MEDIUM | **Detectors accept `null` — no guards** | — | All detector `detect()` methods accept Object params that are never null-checked. With the current `null, null` call pattern, they return empty lists silently. |
| 2.5.2 | 🔵 LOW | **No schema-level comparison** | — | Detectors only model endpoint/field-level changes. No support for: response code changes, header changes, security scheme changes, request body schema changes. |

### 2.6 Test Gaps

| # | Severity | Gap | Files | Lines | Detail |
|---|----------|-----|-------|-------|--------|
| 2.6.1 | 🔴 **CRITICAL** | **No tests exist for the analysis module** | 0 | — | `BreakingAnalysisServiceImpl`, `DiffOrchestratorImpl`, `BaseVersionResolverImpl`, all 6 detectors — **zero test coverage across the entire module**. |

---

## 3. Policy Engine (`policy`)

### 3.1 DslExecutor

**File**: `backend/src/main/java/com/keystone/policy/dsl/DslExecutor.java`

| # | Severity | Gap | Lines | Detail |
|---|----------|-----|-------|--------|
| 3.1.1 | 🟠 HIGH | **Simulates spec data instead of consuming real specs** | 31-33 | Comment: "For the initial implementation, the executor simulates spec data based on known patterns. A full OpenAPI spec parser integration will be added in a future iteration." **Policy evaluation against real specs doesn't work.** |

### 3.2 GitPolicySourceImpl

**File**: `backend/src/main/java/com/keystone/policy/source/GitPolicySourceImpl.java`

| # | Severity | Gap | Lines | Detail |
|---|----------|-----|-------|--------|
| 3.2.1 | 🟡 MEDIUM | **Uses raw `git` shell commands** | 262-280 | `ProcessBuilder` spawns `git` CLI processes. No JGit. Fragile — depends on `git` being in PATH, no error recovery from mid-clone failures, potential command injection if repo URL contains shell metacharacters. |
| 3.2.2 | 🟡 MEDIUM | **Policy YAML parsing is custom regex** | 191-245 | `parsePolicyFile()` uses `Pattern.compile()` with multiline regex. No SnakeYAML. Fragile with: quotes inside strings, YAML anchors/aliases, multi-document YAML, special characters in values. |
| 3.2.3 | 🟡 MEDIUM | **No SSH key auth tested** | — | `deployKeyPath` is read from config but never used in the actual clone command (which just does `git clone --depth=1` without SSH key configuration). |

### 3.3 SyncScheduler

**File**: `backend/src/main/java/com/keystone/policy/sync/SyncScheduler.java`

| # | Severity | Gap | Lines | Detail |
|---|----------|-----|-------|--------|
| 3.3.1 | 🟡 MEDIUM | **Swallows all exceptions** | 53-56 | `catch (Exception e)` only logs. Repeated sync failures produce no alerts, no circuit breaker, no health degradation signal. |
| 3.3.2 | 🔵 LOW | **Fixed 60-second interval, no cron expression** | 40 | `@Scheduled(fixedRateString = ...)` is simpler but doesn't support complex schedules (e.g., "every 5 minutes during business hours"). |

### 3.4 PolicyEvaluationServiceImpl

**File**: `backend/src/main/java/com/keystone/policy/evaluator/PolicyEvaluationServiceImpl.java`

| # | Severity | Gap | Lines | Detail |
|---|----------|-----|-------|--------|
| 3.4.1 | 🔵 LOW | **Placeholder specId** | 87 | `UUID.nameUUIDFromBytes(...)` instead of resolving the actual spec from the repository. Same pattern as the analysis module. |

### 3.5 PolicySyncServiceImpl

**File**: `backend/src/main/java/com/keystone/policy/sync/PolicySyncServiceImpl.java`

| # | Severity | Gap | Lines | Detail |
|---|----------|-----|-------|--------|
| 3.5.1 | 🟡 MEDIUM | **No conflict resolution for concurrent syncs** | — | If two syncs run concurrently (scheduled + webhook-triggered), there's no locking or CAS. May produce duplicate entries or race conditions. |

---

## 4. Dashboard (`dashboard`)

### 4.1 DashboardMetricsRepositoryImpl

**File**: `backend/src/main/java/com/keystone/dashboard/infrastructure/repository/impl/DashboardMetricsRepositoryImpl.java`

| # | Severity | Gap | Lines | Detail |
|---|----------|-----|-------|--------|
| 4.1.1 | 🔴 **CRITICAL** | **All dashboard metrics are hardcoded zeroes** | 25-35 | `findDashboardSummary()` → `DashboardSummary(0.0, emptyList(), 0, 0, 0)`. `findMetrics()` → `emptyList()`. `findPolicyBreakdown()` → `PolicyBreakdown(0, emptyList(), emptyList(), 1.0)`. **The entire Dashboard summary endpoint produces meaningless data.** |
| 4.1.2 | 🟡 MEDIUM | **In-memory only** | 19-35 | No database persistence. Metrics vanish on restart. |

### 4.2 HealthScoreRepositoryImpl

**File**: `backend/src/main/java/com/keystone/dashboard/infrastructure/repository/impl/HealthScoreRepositoryImpl.java`

| # | Severity | Gap | Lines | Detail |
|---|----------|-----|-------|--------|
| 4.2.1 | 🔴 **CRITICAL** | **In-memory storage — data lost on restart** | 19-60 | Uses `CopyOnWriteArrayList`. No database backing. **All health scores vanish when the server restarts.** |
| 4.2.2 | 🟡 MEDIUM | **Trend detection always returns `STABLE`** | 40-46 | `HealthTrend.TrendDirection` computation is a stub — always returns `STABLE` regardless of actual score direction. |

### 4.3 HealthScoreServiceImpl

**File**: `backend/src/main/java/com/keystone/dashboard/application/service/impl/HealthScoreServiceImpl.java`

| # | Severity | Gap | Lines | Detail |
|---|----------|-----|-------|--------|
| 4.3.1 | 🔴 **CRITICAL** | **Spec list is hardcoded empty** | 132-133 | `var allSpecs = Collections.emptyList()` with `// TODO: query from SpecRepository when method is available`. `totalSpecs` is hardcoded to `0L`. GovernanceHealthScore cannot compute anything meaningful. |
| 4.3.2 | 🟠 HIGH | **getComplianceHistory returns empty list** | 152-154 | Placeholder: `return new ArrayList<>()` — no compliance history data flows through. |
| 4.3.3 | 🟠 HIGH | **getViolationTrends returns empty list** | 158-160 | Placeholder: `return new ArrayList<>()` — no violation trend data flows through. |
| 4.3.4 | 🟡 MEDIUM | **Score weights are hardcoded** | 141 | `specComplianceRate * 0.4 + policyPassRate * 0.4 + (1 - exemptionRate) * 0.2`. These weights should be configurable. |

### 4.4 AuditLogServiceImpl

**File**: `backend/src/main/java/com/keystone/dashboard/application/service/impl/AuditLogServiceImpl.java`

| # | Severity | Gap | Lines | Detail |
|---|----------|-----|-------|--------|
| 4.4.1 | 🟠 HIGH | **Audit trail is entirely stubbed** | 23-29 | `query()` returns `Collections.emptyList()`. `count()` returns `0L`. Comment: "the audit event store is not yet implemented." |
| 4.4.2 | 🟡 MEDIUM | **No event sourcing infrastructure** | — | ADR-004 specifies event sourcing for the audit trail. No event store, no event stream, no replay capability exists. |

### 4.5 PolicyUiServiceImpl

**File**: `backend/src/main/java/com/keystone/dashboard/application/service/impl/PolicyUiServiceImpl.java`

| # | Severity | Gap | Lines | Detail |
|---|----------|-----|-------|--------|
| 4.5.1 | 🟡 MEDIUM | **commitPolicyChange writes to temp dir, not Git** | 108-130 | The method promises to commit policy changes to a Git repository. Instead, it writes YAML to `Files.createTempDirectory()`, logs, then deletes the temp file. **No actual Git operation occurs.** |
| 4.5.2 | 🟡 MEDIUM | **Deploy key path is read but never used** | 53 | `deployKeyPath` is injected via `@Value` but never referenced in any method. |

### 4.6 DashboardController

**File**: `backend/src/main/java/com/keystone/dashboard/interfaces/http/DashboardController.java`

| # | Severity | Gap | Lines | Detail |
|---|----------|-----|-------|--------|
| 4.6.1 | 🟡 MEDIUM | **commitPolicyChange endpoint is a no-op** | 205 | `// TODO: Wire up PolicyUiService.commitPolicyChange when Git operations are configured`. Returns `200 OK` but does nothing. |
| 4.6.2 | 🟡 MEDIUM | **Injects concrete impl instead of interface** | 45-47 | Injects `HealthScoreServiceImpl` directly instead of `HealthScoreService` interface. Violates Clean Architecture dependency inversion. |

### 4.7 Test Gaps

| # | Severity | Gap | Files | Lines | Detail |
|---|----------|-----|-------|-------|--------|
| 4.7.1 | 🟠 HIGH | **No tests exist for the Dashboard module** | 0 | — | `DashboardQueryServiceImpl`, `HealthScoreServiceImpl`, `AuditLogServiceImpl`, `PolicyUiServiceImpl`, `DashboardController`, `DashboardMetricsRepositoryImpl`, `HealthScoreRepositoryImpl` — **zero test coverage across the entire module**. |

---

## 5. Dependency Graph (`graph`)

### 5.1 ImpactAnalyzerImpl

**File**: `backend/src/main/java/com/keystone/graph/domain/service/ImpactAnalyzerImpl.java`

| # | Severity | Gap | Lines | Detail |
|---|----------|-----|-------|--------|
| 5.1.1 | 🟡 MEDIUM | **No cycle detection in BFS traversal** | — | BFS-based downstream impact analysis. Self-referencing or circular service dependencies may cause infinite loops. |
| 5.1.2 | 🔵 LOW | **No depth limit configurable** | — | Impact analysis depth is unbounded. A deeply nested dependency chain could cause excessive computation. |

### 5.2 DependencyParserImpl

**File**: `backend/src/main/java/com/keystone/graph/domain/service/DependencyParserImpl.java`

| # | Severity | Gap | Lines | Detail |
|---|----------|-----|-------|--------|
| 5.2.1 | 🟡 MEDIUM | **No auto-discovery from OpenAPI specs** | — | Per ADR-006, auto-discovery from `x-keystone-*` vendor extensions was planned. Currently, services must be registered manually. |
| 5.2.2 | 🔵 LOW | **No OpenAPI spec reference extraction** | — | Cannot parse `$ref` patterns between specs to auto-detect dependencies. |

### 5.3 GraphMetrics

**File**: `backend/src/main/java/com/keystone/graph/infrastructure/config/GraphMetrics.java`

| # | Severity | Gap | Lines | Detail |
|---|----------|-----|-------|--------|
| 5.3.1 | 🟡 MEDIUM | **Simple counters, no latency histograms** | — | Uses `AtomicLong` for counts. No Micrometer `Timer` for operation latencies, no `DistributionSummary` for graph sizes, no percentiles exposed. |

### 5.4 GraphRepositoryImpl

**File**: `backend/src/main/java/com/keystone/graph/infrastructure/repository/GraphRepositoryImpl.java`

| # | Severity | Gap | Lines | Detail |
|---|----------|-----|-------|--------|
| 5.4.1 | 🔵 LOW | **No pagination on findAllServices** | 61-63 | Loads all services into memory. For orgs with thousands of services, risks OOM. |

---

## 6. Notification Engine (`notification`)

### 6.1 CiStatusChannelImpl

**File**: `backend/src/main/java/com/keystone/notification/domain/channel/CiStatusChannelImpl.java`

| # | Severity | Gap | Lines | Detail |
|---|----------|-----|-------|--------|
| 6.1.1 | 🟡 MEDIUM | **Circuit breaker is hand-rolled** | 273-316 | Comment: "To be replaced by Resilience4j CircuitBreaker in production." The custom implementation lacks: bulkhead isolation, event listeners, metrics export, half-open probe configuration. |
| 6.1.2 | 🟡 MEDIUM | **Reflection-based event type detection** | 130-144 | `hasMethod()` uses reflection to detect event types instead of a typed interface or visitor pattern. Fragile — method rename on the event class silently breaks detection. |

### 6.2 NotificationChannel Implementations

**File**: `backend/src/main/java/com/keystone/notification/domain/channel/`

| # | Severity | Gap | Lines | Detail |
|---|----------|-----|-------|--------|
| 6.2.1 | 🔵 LOW | **Only CI status channel is production-ready** | — | The architecture specifies multi-channel dispatch (email, Slack, PagerDuty, webhook). Only `CiStatusChannelImpl` is implemented. `TestEmailChannel` and `TestCiStatusChannel` are test-only. |
| 6.2.2 | 🔵 LOW | **TestEmailChannel uses `System.out`** | — | Test channels print to stdout instead of using a proper logging framework or spy. |

### 6.3 NotificationRepositoryImpl

**File**: `backend/src/main/java/com/keystone/notification/infrastructure/repository/NotificationRepositoryImpl.java`

| # | Severity | Gap | Lines | Detail |
|---|----------|-----|-------|--------|
| 6.3.1 | 🟡 MEDIUM | **In-memory storage** | — | Same pattern as Dashboard — notification history vanishes on restart. |

---

## 7. Cross-Cutting & Infrastructure

### 7.1 Configuration & Application

**File**: `backend/src/main/resources/application.yml`

| # | Severity | Gap | Lines | Detail |
|---|----------|-----|-------|--------|
| 7.1.1 | 🟡 MEDIUM | **No OpenTelemetry exporter configured** | — | Micrometer tracing is enabled but no exporter (Jaeger/Zipkin/OTLP) is configured. Traces are generated but never sent anywhere. |
| 7.1.2 | 🔵 LOW | **No rate limiting configured** | — | No bucket4j, Resilience4j RateLimiter, or Spring Cloud Gateway rate limiting on any endpoint. |
| 7.1.3 | 🔵 LOW | **Health check probes don't check downstream deps** | — | `/actuator/health` is enabled but doesn't reflect RabbitMQ or database connectivity in the overall status. A server with a dead database still reports "UP". |

### 7.2 Security

| # | Severity | Gap | Files | Lines | Detail |
|---|----------|-----|-------|-------|--------|
| 7.2.1 | 🟡 MEDIUM | **No RBAC/authorization on any endpoint** | All controllers | — | Endpoints are documented as restricted (e.g., `GET /audit-log` → `COMPLIANCE_MANAGER`) but there are zero `@PreAuthorize` or `@Secured` annotations. No Spring Security filter chain configured. |
| 7.2.2 | 🟡 MEDIUM | **No CSRF protection configured** | — | — | Not an issue for REST APIs with Bearer tokens, but worth noting for any cookie-based auth path. |
| 7.2.3 | 🔵 LOW | **No input sanitization beyond Jakarta Validation** | — | — | Relies on `@Valid` annotations. No content security policy, no HTML sanitization for policy names/descriptions rendered in the dashboard. |

### 7.3 Database & Migrations

| # | Severity | Gap | Files | Lines | Detail |
|---|----------|-----|-------|-------|--------|
| 7.3.1 | 🟡 MEDIUM | **No database migration tool** | — | — | Uses Hibernate `ddl-auto: validate` in prod. No Flyway or Liquibase. Schema changes must be applied manually or rely on Hibernate's auto-DDL (dangerous in production). |
| 7.3.2 | 🟡 MEDIUM | **PostgreSQL init scripts don't exist** | — | — | `docker-compose.yml` mounts `./backend/src/main/resources/db/init:/docker-entrypoint-initdb.d:ro` but that directory **does not exist**. PostgreSQL starts with no schema. |
| 7.3.3 | 🟡 MEDIUM | **n+1 query risk in SpecRepositoryImpl** | — | — | No explicit `JOIN FETCH` or `@EntityGraph` for spec→version relationships. Loading specs with versions will trigger n+1 queries. |

### 7.4 Test Coverage Summary

| # | Severity | Gap | Lines | Detail |
|---|----------|-----|-------|--------|
| 7.4.1 | 🔴 **CRITICAL** | **No end-to-end or integration tests** | — | Each module tests in isolation. No test validates: ingest → analyze → evaluate → display. The full pipeline has never been exercised as a unit. |
| 7.4.2 | 🟠 HIGH | **Low overall coverage (~24%)** | 3,838 test / ~16,000 prod | Dashboard module: **0 tests**. Analysis module: **0 tests**. The only modules with reasonable coverage are Policy (~1,600 lines) and Ingestion (~550 lines). |
| 7.4.3 | 🟡 MEDIUM | **No contract tests** | — | No Spring Cloud Contract or Pact tests to validate frontend↔backend interface compatibility. |

### 7.5 Docker & Deployment

| # | Severity | Gap | Files | Lines | Detail |
|---|----------|-----|-------|-------|--------|
| 7.5.1 | 🟡 MEDIUM | **Backend Dockerfile is not multi-stage** | `backend/Dockerfile` | — | Maven and JDK end up in the same image as the running JAR. Should use multi-stage build to minimize image size. |
| 7.5.2 | 🔵 LOW | **No `.dockerignore` per module** | — | — | Root `.dockerignore` exists but backend should ignore `.git`, `node_modules`, etc. at the Docker context level. |
| 7.5.3 | 🔵 LOW | **No Prometheus/Grafana dashboards** | — | — | Metrics are exposed at `/actuator/prometheus` but no `prometheus.yml` or `grafana-dashboard.json` is provided. |

---

## 8. Dependency Graph — Gap Count

| Severity | Count |
|----------|-------|
| 🔴 **CRITICAL** | 6 |
| 🟠 **HIGH** | 11 |
| 🟡 **MEDIUM** | 22 |
| 🔵 **LOW** | 11 |
| **Total** | **50** |
