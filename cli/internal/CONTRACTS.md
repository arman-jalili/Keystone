# cli-orchestrator — Frozen Contracts

> **Canonical Reference:** `.pi/architecture/modules/cli-orchestrator.md`
> **Status:** FROZEN — do not modify interfaces without an ADR and coordinated change across all consumers.

## Component Interface Map

| Component | Interface File | Contract File | Status |
|-----------|---------------|---------------|--------|
| **Orchestrator** | `internal/application/interfaces/orchestrator.go` | Coordinates parse → cache → diff → upload pipeline | FROZEN |
| **SpecParser** | `internal/application/interfaces/parser.go` | Parses and validates OpenAPI 3.x specs | FROZEN |
| **SpecCache** | `internal/application/interfaces/cache.go` | Reads/writes cached spec versions by SHA-256 key | FROZEN |
| **DiffEngine** | `internal/application/interfaces/diff.go` | Compares two specs, produces DiffResult with Verdict | FROZEN |
| **Uploader** | `internal/application/interfaces/uploader.go` | Async HTTP upload of audit results to server | FROZEN |

## Domain Types

| Type | File | Description |
|------|------|-------------|
| `SpecDocument` | `internal/domain/spec.go` | Canonical representation of a parsed OpenAPI spec |
| `Endpoint` | `internal/domain/spec.go` | Single path+method combination |
| `CachedSpec` | `internal/domain/spec.go` | Serialised form for filesystem cache |
| `Verdict` | `internal/domain/diff.go` | PASS / BREAKING / WARNING |
| `Severity` | `internal/domain/diff.go` | ADDITIVE / BREAKING / WARNING / INFO |
| `Change` | `internal/domain/diff.go` | Single change between two spec versions |
| `DiffResult` | `internal/domain/diff.go` | Combined diff output with verdict |
| `AnalysisContext` | `internal/domain/context.go` | CI environment metadata |
| `ExitCode` | `internal/domain/exitcode.go` | Process exit codes (0=PASS, 1=FAIL, 2=WARN, 3=ERROR) |

## Error Types

| Error | File | Severity | Recovery |
|-------|------|----------|----------|
| `SpecParseError` | `internal/domain/errors.go` | Terminal (exit 3) | Fix spec file |
| `CacheMissError` | `internal/domain/errors.go` | Non-terminal | Diff against empty baseline |
| `CacheCorruptError` | `internal/domain/errors.go` | Non-terminal | Discard entry, diff against empty |
| `UploadFailedError` | `internal/domain/errors.go` | Non-terminal (warning) | Log and continue |

## DTO Contracts

| DTO | File | Direction |
|-----|------|-----------|
| `AnalyzeRequest` | `internal/application/dto/requests.go` | CLI → Application |
| `AuditUploadPayload` | `internal/application/dto/requests.go` | CLI → Server (HTTP POST body) |
| `AnalyzeResponse` | `internal/application/dto/responses.go` | Application → CLI |
| `AuditUploadResponse` | `internal/application/dto/responses.go` | Server → CLI (HTTP response) |

## HTTP API Contracts

| Endpoint | Method | Contract File |
|----------|--------|---------------|
| `/api/v1/ingestion/audit` | POST | `internal/interfaces/http/contracts.go` |
| `/api/v1/health` | GET | `internal/interfaces/http/contracts.go` |

### Audit Upload Contract

**Request:**
```
POST /api/v1/ingestion/audit
Content-Type: application/json
Authorization: Bearer <token>
X-Request-ID: <correlation-id>

{
  "result": {
    "verdict": "PASS|BREAKING|WARNING",
    "changes": [
      {
        "severity": "ADDITIVE|BREAKING|WARNING|INFO",
        "path": "/pets/{petId}",
        "method": "get",
        "description": "..."
      }
    ],
    "analysisMs": 42
  },
  "context": {
    "specPath": "./openapi.yaml",
    "gitCommitSha": "abc123",
    "branchName": "feature/my-change",
    "ciJobId": "12345",
    "cacheHit": true
  }
}
```

**Success Response:** `202 Accepted`
```json
{ "status": "accepted" }
```

**Error Response:** `4xx` / `5xx`
```json
{
  "code": "VALIDATION_ERROR",
  "message": "Human-readable description",
  "requestId": "correlation-id"
}
```

## Event Schemas

All events defined in `internal/domain/event/events.go`:

| Event Type | Payload | When Emitted |
|-----------|---------|-------------|
| `spec.parsed` | `SpecParsedPayload` | After successful spec parse |
| `diff.computed` | `DiffComputedPayload` | After diff computation |
| `upload.initiated` | `UploadInitiatedPayload` | Before each upload attempt |
| `upload.completed` | `UploadCompletedPayload` | After successful upload |
| `upload.failed` | `UploadFailedPayload` | After all retries exhausted |
| `cache.hit` | (empty) | When cached spec found |
| `cache.miss` | (empty) | When cached spec not found |

## Infrastructure Contracts

| Interface | File | Implementations |
|-----------|------|-----------------|
| `SpecRepository` | `internal/infrastructure/repository/interfaces.go` | filesystem (cli/cache.go) |
| `Publisher` | `internal/infrastructure/event/publisher.go` | stdout JSON, message queue |

## Exit Code Contract

| Code | Constant | Meaning |
|------|----------|---------|
| 0 | `ExitPass` | No breaking changes |
| 1 | `ExitFail` | Breaking changes detected |
| 2 | `ExitWarn` | Only additive/warning changes |
| 3 | `ExitError` | Internal error (parse failure, I/O, etc.) |
