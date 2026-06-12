# Disaster Recovery Plan: cli-orchestrator

> **Canonical Reference:** `.pi/architecture/modules/cli-orchestrator.md`
> **Component:** keystone-cli
> **RTO:** N/A (CLI tool, no uptime requirement)
> **RPO:** N/A (no persistent state beyond cache)

## Overview

The keystone CLI is a stateless, disposable CI tool. It has no database, no server process, and no persistent state that cannot be regenerated. The only data at risk is the local spec cache (`~/.keystone/cache/`), which is always recoverable from the source spec files in version control.

## Backup Strategy

The local cache at `~/.keystone/cache/` contains temporary JSON files keyed by SHA-256 checksum. If the cache is lost, subsequent runs will diff against an empty baseline (all endpoints classified as ADDITIVE). This is safe but produces noisier results on first post-cache-loss run.

- **Schedule**: None — cache is ephemeral
- **Method**: N/A — spec files in VCS are the source of truth
- **RTO**: N/A — no recovery time objective (CLI tool, no server)
- **RPO**: N/A — no recovery point objective (cache is regenerable)

## Restore Procedure

### Scenario: Local cache corrupted or deleted
1. **Impact**: Single run diff may show false positives (all additive)
2. **Recovery**: Re-run the CLI — cache is auto-created
3. **Verification**: Second run will have a valid cache and produce accurate diffs

### Scenario: CLI binary corrupted
1. **Recovery**: Rebuild from source: `cd cli && go build -o keystone ./cmd/keystone/`
2. **Verification**: Run `keystone analyze --spec=<test-spec>`

### Scenario: Server unavailable for audit upload
1. **Impact**: Audit records not persisted on server
2. **Recovery**: No action needed — CLI exit code is unaffected
3. **Verification**: Re-run when server is restored

## Failover Plan

The CLI has no failover requirements — it runs as a single-shot process in CI. If the binary fails to execute, the CI pipeline retries the job. There is no leader election, clustering, or load balancing.
