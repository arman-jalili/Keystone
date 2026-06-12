# Runbook: cli-orchestrator

> **Canonical Reference:** `.pi/architecture/modules/cli-orchestrator.md`
> **Component:** keystone-cli
> **Language:** Go
> **Last Updated:** 2026-06-12

## Overview

The keystone CLI orchestrator is a lightweight Go binary that runs inside CI
pipelines. It parses OpenAPI 3.x specs, diffs against cached previous versions,
produces a breaking-change verdict, and uploads results to the Keystone server
for audit persistence.

## Startup Sequence

1. **Binary invocation**: CI pipeline calls `keystone analyze --spec=<path>`
2. **Flag parsing**: CLI parses flags (spec path, server URL, token, cache dir)
3. **Spec parsing**: `SpecParser` loads and validates the OpenAPI 3.x file
4. **Cache lookup**: `LocalCache` retrieves previous version by SHA-256 checksum
5. **Diff computation**: `DiffEngine` compares base vs target specs
6. **Cache update**: Current spec saved to local cache for future runs
7. **Upload** (async): `Uploader` sends `DiffResult` to Keystone server
8. **Exit**: Process exits with code 0 (PASS), 1 (FAIL), 2 (WARN), or 3 (ERROR)

## Dependencies

| Dependency | Type | Required | Notes |
|-----------|------|----------|-------|
| `~/.keystone/cache/` | Filesystem | No | Auto-created if missing |
| Keystone Server | HTTP | No | Upload is best-effort |
| OpenAPI 3.x spec file | Filesystem | Yes | Provided via `--spec` flag |

## Configuration Reference

| Variable | Flag | Default | Description |
|----------|------|---------|-------------|
| `KEYSTONE_API_TOKEN` | `--token` | "" | Bearer token for server auth |
| — | `--spec` | — | Path to OpenAPI spec file (required) |
| — | `--server` | "" | Keystone server URL for audit upload |
| — | `--cache` | `~/.keystone/cache` | Cache directory override |
| — | `--verbose` | false | Enable verbose logging |

## Graceful Shutdown

The CLI process is single-shot — it runs to completion and exits. No graceful
shutdown is needed. The async upload goroutine has a 10-second timeout and will
complete (or fail) before the process exits.

## Common Failure Modes

### 1. Spec file not found
- **Symptom**: `SpecParseError` with "load from file" detail
- **Exit code**: 3 (ERROR)
- **Recovery**: Verify the spec path is correct

### 2. Spec validation failure
- **Symptom**: `SpecParseError` with "validation" detail
- **Exit code**: 3 (ERROR)
- **Recovery**: Fix spec to conform to OpenAPI 3.x schema

### 3. Cache miss (expected)
- **Symptom**: `CacheMissError` — logged as info
- **Exit code**: Depends on diff result (0/1/2)
- **Recovery**: None needed — diff is against empty baseline

### 4. Cache corrupt
- **Symptom**: `CacheCorruptError` — logged as warning
- **Exit code**: Depends on diff result (0/1/2)
- **Recovery**: Delete corrupt file or clear `~/.keystone/cache/`

### 5. Upload failure
- **Symptom**: `UploadFailedError` — logged as warning
- **Exit code**: Determined by diff verdict only
- **Recovery**: Check server availability and API token

## Health Check

The CLI has no persistent health endpoint. Connectivity can be verified with:

```bash
keystone analyze --spec=<path> --server=<url> --verbose
```

## Logging

All output goes to stderr. Format:

```
<timestamp> [<level>] <message>
```

Levels: INFO, WARN, ERROR

## Metrics

The CLI self-reports `analysisMs` in the JSON output, indicating diff
computation time in milliseconds.
