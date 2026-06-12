# Contract Freeze: cli-orchestrator

## Summary

Defines and freezes all public interfaces, contracts, and schemas for the `cli-orchestrator` epic before any implementation begins. This prevents architecture drift by ensuring implementation satisfies contracts, not the other way around.

## Changes

**19 new files** across 7 layers of a Clean Architecture Go project:

### Domain Layer (`internal/domain/`)
- `spec.go` — Core domain entities: `SpecDocument`, `Endpoint`, `CachedSpec`
- `diff.go` — Diff domain: `Verdict` (PASS/BREAKING/WARNING), `Severity`, `Change`, `DiffResult`
- `context.go` — `AnalysisContext` for CI environment metadata
- `errors.go` — Domain error types: `SpecParseError`, `CacheMissError`, `CacheCorruptError`, `UploadFailedError`
- `exitcode.go` — Frozen exit code contract: 0=PASS, 1=FAIL, 2=WARN, 3=ERROR
- `event/events.go` — 7 domain event types with envelope format

### Application Layer (`internal/application/`)
- `interfaces/parser.go` — `SpecParser` interface (OpenAPI 3.x parse+validate)
- `interfaces/cache.go` — `SpecCache` interface (local filesystem cache)
- `interfaces/diff.go` — `DiffEngine` interface (spec comparison with verdict)
- `interfaces/uploader.go` — `Uploader` interface (async HTTP audit upload)
- `interfaces/orchestrator.go` — `Orchestrator` top-level pipeline coordinator
- `dto/requests.go` — `AnalyzeRequest`, `AuditUploadPayload` DTOs with validation
- `dto/responses.go` — `AnalyzeResponse`, `AuditUploadResponse` DTOs

### Infrastructure Layer (`internal/infrastructure/`)
- `repository/interfaces.go` — `SpecRepository` DAO interface
- `event/publisher.go` — `Publisher` event dispatch interface

### HTTP API Contracts (`internal/interfaces/http/`)
- `contracts.go` — Frozen endpoint contracts (`POST /api/v1/ingestion/audit`, `GET /api/v1/health`), retry policy, error response format

### CLI Entry Point (`cmd/keystone/`)
- `main.go` — CLI flag contract and exit code contract

### Documentation
- `internal/CONTRACTS.md` — Complete frozen contract reference

## Acceptance Criteria

| # | Criterion | Status |
|---|-----------|--------|
| 1 | All component interfaces defined | ✅ 5 service interfaces + 2 infra interfaces |
| 2 | Contracts reviewed and frozen | ✅ PR review required |
| 3 | DTO schemas documented | ✅ CONTRACTS.md + JSON field tags |
| 4 | No implementation | ✅ Interface-only, zero business logic |

## Related Issues

- Closes #24 — Contract Freeze: Define interfaces and contracts
- Relates to #23 — cli-orchestrator epic tracking
