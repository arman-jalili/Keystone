# Gap Ledger — CLI (Go)

> **Audit of all gaps, placeholders, TODOs, and unimplemented features**
> in the Keystone Go CLI.
>
> Severity key:
> - 🔴 **CRITICAL** — End-to-end flow broken, data doesn't reach the user
> - 🟠 **HIGH** — Feature is non-functional or produces empty/corrected results
> - 🟡 **MEDIUM** — Feature works in principle but has known simplifications
> - 🔵 **LOW** — Minor gap, nice-to-have, or future optimization

---

## 1. CLI Entry Point

### 1.1 `main.go`

**File**: `cli/cmd/keystone/main.go`

| # | Severity | Gap | Lines | Detail | Status |
|---|----------|-----|-------|--------|--------|
| 1.1.1 | ~~🔴~~ **CRITICAL** → ✅ | **Entry point is a placeholder — prints and exits** | 35-38 | ~~`fmt.Fprintln(os.Stderr, "keystone-cli: not yet implemented — this is a contract placeholder")` followed by `os.Exit(int(domain.ExitError))`. **The CLI tool cannot run.**~~ **FIXED**: Full flag parsing, wiring, and analysis pipeline implemented. | ✅ **CLOSED** |
| 1.1.2 | ~~🔴~~ **CRITICAL** → ✅ | **No flag parsing** | — | ~~The package docstring specifies flags: `--spec`, `--server`, `--token`, `--cache`, `--verbose`. **Zero flag parsing code exists.** No call to `flag.Parse()`, no `os.Args` processing.~~ **FIXED**: All flags implemented with `flag` package, including `--commit`, `--branch`, `--ci-job`, `--version`. | ✅ **CLOSED** |
| 1.1.3 | ~~🟠~~ **HIGH** → ✅ | **No wiring to any application logic** | — | ~~`main()` does nothing except print and exit. The orchestrator, parser, diff engine, cache, and uploader adapters exist elsewhere but are never instantiated or called.~~ **FIXED**: Full wiring: `KinOpenAPIParser + FilesystemCache + SpecDiffEngine + HTTPUploader → Orchestrator → main()`. | ✅ **CLOSED** |
| 1.1.4 | ~~🟡~~ **MEDIUM** → ✅ | **Exit codes are defined but never used** | `internal/domain/exitcode.go` | ~~Exit code constants (`PASS=0`, `FAIL=1`, `WARN=2`, `ERROR=3`) are documented as frozen contract but only `ExitError` (3) is hardcoded in the stub.~~ **FIXED**: All 4 exit codes are used: `ExitPass` (0) for PASS, `ExitFail` (1) for BREAKING, `ExitWarn` (2) for WARNING, `ExitError` (3) for errors. | ✅ **CLOSED** |

---

## 2. Application Layer

### 2.1 Orchestrator

**File**: `cli/internal/application/service/orchestrator.go`

| # | Severity | Gap | Lines | Detail | Status |
|---|----------|-----|-------|--------|--------|
| 2.1.1 | ~~🔴~~ **CRITICAL** → ✅ | **Orchestrator interface has no implementation** | 22 | ~~The `Orchestrator` interface defines a single method `Analyze(req) (*dto.AnalyzeResponse, error)` but **no implementation file exists** (no `orchestrator.go` in any package). The core coordination layer — parse → cache → diff → upload — has never been written.~~ **FIXED**: `service.Orchestrator` created at `internal/application/service/orchestrator.go`. Full pipeline: parse → cache lookup → diff → cache store → upload → respond. Domain events published at each stage. | ✅ **CLOSED** |
| 2.1.2 | ~~🟠~~ **HIGH** → ✅ | **No dependency injection wiring** | — | ~~Even if the orchestrator were implemented, there's no DI container or manual wiring. The CLI needs to construct: `KinOpenAPIParser + FilesystemCache + SpecDiffEngine + HTTPUploader → OrchestratorImpl`, then pass it to `main()`. None of this exists.~~ **FIXED**: Constructor-based DI in `main.go` — all 5 dependencies (parser, cache, diff, uploader, publisher) are constructed then passed to `NewOrchestrator()`. | ✅ **CLOSED** |
| 2.1.3 | 🔵 LOW | **Cache is content-addressed — can't detect cross-version diffs** | `service/orchestrator.go:100` | Cache key is `target.Checksum` (SHA-256 of current spec content). If a spec changes, the checksum changes and the cache misses. This means **the CLI only detects "seen this exact content before" vs "new content"**. It cannot detect breaking changes between v1 and v2 of the same API. To fix: maintain a path→checksum index file, look up previous checksum by file path, then retrieve by that checksum. | **OPEN** |
| 2.1.4 | 🟡 MEDIUM | **No graceful timeout on analysis** | `service/orchestrator.go:69` | The `Analyze` method doesn't accept a `context.Context` — it can't be cancelled once started. For large specs (>10MB), this could block indefinitely. | **OPEN** |

### 2.2 Uploader Contract Mismatch

**File**: `cli/internal/infrastructure/uploader/http_uploader.go`

| # | Severity | Gap | Lines | Detail | Status |
|---|----------|-----|-------|--------|--------|
| 2.2.1 | 🟡 MEDIUM | **Uploader payload may not match server contract** | 67-70 | The CLI uploader sends `dto.AuditUploadPayload{Result, Context}` to `POST /api/v1/ingestion/audit`. The backend's `IngestionController` expects `IncomingSpec` (a different DTO with `content`, `repository`, `commitSha`, `specPath`, `specFormat`). **The client and server contracts are out of sync.** | **OPEN** |
| 2.2.2 | 🔵 LOW | **No response body parsing** | — | `doPost()` returns `nil` on any 2xx without parsing the response body. The server returns `SpecIngestedResponse` with `specId`, `eventId`, `duplicate` flag, but the client ignores it all. | **OPEN** |

### 2.3 Event Publisher

**File**: `cli/internal/infrastructure/event/stdout_publisher.go`

| # | Severity | Gap | Lines | Detail | Status |
|---|----------|-----|-------|--------|--------|
| 2.3.1 | ~~🟡~~ **MEDIUM** → ✅ | **Event publisher is a stub** | 26 | ~~Interface defines `Publish(topic string, payload interface{}) error`. The only implementation appears to be a no-op. No actual publishing infrastructure (AMQP, file, stdout) is wired.~~ **FIXED**: `StdoutPublisher` created at `internal/infrastructure/event/stdout_publisher.go`. Writes structured JSON events to stderr, one per line. `PublishBatch` also implemented. | ✅ **CLOSED** |

### 2.4 Repository Interfaces

**File**: `cli/internal/infrastructure/repository/interfaces.go`

| # | Severity | Gap | Lines | Detail | Status |
|---|----------|-----|-------|--------|--------|
| 2.4.1 | 🔵 LOW | **Defined but no persistence exists** | 30 | Repository interfaces exist for analysis result storage but no database or file-backed implementation is provided. Currently unused — the cache serves as the storage layer. | **OPEN** |

---

## 3. Infrastructure Layer

### 3.1 Filesystem Cache

**File**: `cli/internal/infrastructure/cache/filesystem_cache.go`

| # | Severity | Gap | Lines | Detail | Status |
|---|----------|-----|-------|--------|--------|
| 3.1.1 | 🟡 MEDIUM | **No TTL or eviction policy** | 120 | Cached specs grow unbounded. No maximum size, no TTL, no LRU/MRU eviction. A CI system running daily on 500 APIs will accumulate cache entries indefinitely. | **OPEN** |
| 3.1.2 | 🔵 LOW | **No cache corruption detection** | — | No checksum validation on cache reads. If the cache file is truncated (power loss during write), the deserialization may silently return partial data. | **OPEN** |

### 3.2 HTTP Contracts

**File**: `cli/internal/interfaces/http/contracts.go`

| # | Severity | Gap | Lines | Detail | Status |
|---|----------|-----|-------|--------|--------|
| 3.2.1 | 🔵 LOW | **Constants not environment-configurable** | 90 | `UploadTimeout`, `MaxUploadRetries`, `AuditEndpoint`, `HeaderAuthorization`, etc. are hardcoded `const`. Should be configurable via environment variables for different server deployments. | **OPEN** |

### 3.3 Shared infrastructure gaps

| # | Severity | Gap | Files | Detail | Status |
|---|----------|-----|-------|--------|--------|
| 3.3.1 | 🔵 LOW → ✅ | **No structured logging** | `main.go` | ~~Uses `fmt.Fprintln` for output. No `slog` or `logrus` for structured JSON logging.~~ **FIXED**: Uses `log/slog` with `slog.NewTextHandler` for structured text logging. Log level configurable via `--verbose`. Source locations in debug mode. | ✅ **CLOSED** |
| 3.3.2 | 🔵 LOW → ✅ | **No graceful shutdown** | `main.go` | ~~`os.Exit()` is called directly in the stub. No signal handling for SIGTERM/SIGINT. In-flight uploads would be interrupted.~~ **FIXED**: SIGINT/SIGTERM handler via `signal.Notify` with `context.WithCancel`. | ✅ **CLOSED** |

---

## 4. Domain Layer

### 4.1 Error Handling

**File**: `cli/internal/domain/errors.go`

| # | Severity | Gap | Lines | Detail | Status |
|---|----------|-----|-------|--------|--------|
| 4.1.1 | 🔵 LOW | **No stack traces in errors** | 62 | `SpecParseError` stores `[]error` details but doesn't capture stack traces. Debugging parse failures requires manual correlation. | **OPEN** |

### 4.2 Analysis Context

**File**: `cli/internal/domain/context.go`

| # | Severity | Gap | Lines | Detail | Status |
|---|----------|-----|-------|--------|--------|
| 4.2.1 | 🟡 MEDIUM → ✅ | **AnalysisContext defined but never populated** | 21 | ~~`AnalysisContext` struct has `Repository`, `CommitSha`, `SpecPath`, `SpecVersion`, `CacheDir`, `Verbose` fields. Nothing in the codebase instantiates or populates this struct except the uploader.~~ **FIXED**: `AnalysisContext` is now populated by the orchestrator with `SpecPath`, `GitCommitSHA`, `BranchName`, `CIJobID`, and `CacheHit`. Passed to the uploader as intended. | ✅ **CLOSED** |

### 4.3 Spec Document Limitations

**File**: `cli/internal/domain/spec.go`

| # | Severity | Gap | Lines | Detail | Status |
|---|----------|-----|-------|--------|--------|
| 4.3.1 | 🔵 LOW | **No schema-level data in SpecDocument** | — | The domain model only captures endpoints (path+method). No schema info (request/response bodies, parameters, security schemes, headers). The diff engine is limited to endpoint-level comparison. | **OPEN** |

### 4.4 Missing ChecksumBytes Function

**File**: `cli/internal/domain/spec.go`

| # | Severity | Gap | Lines | Detail | Status |
|---|----------|-----|-------|--------|--------|
| 4.4.1 | ~~🔴~~ **CRITICAL** → ✅ | **`ChecksumBytes` referenced but not defined** | 98 in parser | Parser called `domain.ChecksumBytes(rawData)` but the function didn't exist. Code failed to compile. **FIXED**: Added `ChecksumBytes()` using `crypto/sha256`. | ✅ **CLOSED** |
| 4.4.2 | ~~🟡~~ **MEDIUM** → ✅ | **Missing Go module dependencies** | `go.mod` | `go.mod` had no `require` block, but `go.sum` contained dependency hashes. `go mod tidy` was never run. **FIXED**: `go mod tidy` resolved all dependencies (`kin-openapi`, `testify`, `yaml.v3`, etc.). | ✅ **CLOSED** |

---

## 5. Test Gaps

| # | Severity | Gap | Files | Lines | Detail | Status |
|---|----------|-----|-------|-------|--------|--------|
| 5.1.1 | 🟠 HIGH | **No application-layer tests** | 0 | — | The orchestrator (which now exists) has no tests. Integration of `parser → cache → diff → upload` has never been tested. | **OPEN** |
| 5.1.2 | 🟡 MEDIUM | **Infrastructure tests exist but are narrow** | 4 files | ~793 lines | `filesystem_cache_test.go`, `spec_diff_test.go`, `kinopenapi_parser_test.go`, `http_uploader_test.go` exist and test individual adapters in isolation. But no test validates the wiring between them. | **OPEN** |
| 5.1.3 | 🟡 MEDIUM | **No integration test with real server** | — | — | The uploader test uses mocks. No test starts a real Keystone server and validates the full CLI→server flow. | **OPEN** |

---

## 6. Gap Count

| Severity | Count |
|----------|-------|
| 🔴 **CRITICAL** | 0 (was 4 — **all closed**) |
| 🟠 **HIGH** | 1 (was 2 — **1 closed**) |
| 🟡 **MEDIUM** | 6 |
| 🔵 **LOW** | 7 (was 8 — **1 closed**) |
| **Total** | **14 (was 20 — 6 closed)** |

---

## 7. Progress Summary

### ✅ Fixed (6 gaps closed)

| Gap | What was done |
|-----|---------------|
| **1.1.1** CLI entry point stub | Full `main.go` with flag parsing, DI wiring, analysis pipeline, exit codes |
| **1.1.2** No flag parsing | All 9 flags implemented (`--spec`, `--server`, `--token`, `--cache`, `--verbose`, `--commit`, `--branch`, `--ci-job`, `--version`) |
| **1.1.3** No wiring | `KinOpenAPIParser + FilesystemCache + SpecDiffEngine + HTTPUploader → Orchestrator → main()` |
| **1.1.4** Exit codes unused | All 4 exit codes wired: PASS→0, FAIL→1, WARN→2, ERROR→3 |
| **2.1.1** Orchestrator missing | `service.Orchestrator` at `internal/application/service/orchestrator.go` — 250+ lines |
| **2.1.2** No DI | Constructor-based DI in `main.go` — all adapters wired |
| **2.3.1** Event publisher stub | `StdoutPublisher` at `internal/infrastructure/event/stdout_publisher.go` — JSON events to stderr |
| **3.3.1** No structured logging | `log/slog` with text handler, configurable level, source in debug |
| **3.3.2** No graceful shutdown | SIGINT/SIGTERM handler with context |
| **4.2.1** AnalysisContext never populated | Orchestrator populates all fields before upload |
| **4.4.1** ChecksumBytes missing | `crypto/sha256` implementation added to `spec.go` |
| **4.4.2** Missing go.mod deps | `go mod tidy` resolved all dependencies |

### 🔜 Next priorities

1. **Cache design limitation (2.1.3)** — Content-addressed cache can't detect cross-version breaking changes. Need path→checksum index.
2. **Uploader contract mismatch (2.2.1)** — CLI sends `AuditUploadPayload`, backend expects `IncomingSpec`.
3. **Orchestrator tests (5.1.1)** — Critical new code with zero tests.
4. **Cache eviction (3.1.1)** — Unbounded growth in CI environments.
5. **Graceful cancellation (2.1.4)** — Context propagation through the pipeline.
