# ADR-002: Local/Remote Split — CLI Orchestrator Architecture

**Status:** Accepted
**Date:** 2026-06-12
**Session:** 7bff170e-8b01-4621-9de1-4397f096b27a

## Context

The original requirement "Must run in CI with <10ms overhead" is impossible for a remote server call (minimum 50–200ms network + processing). Keystone must provide a governance gate that fits into CI pipelines without slowing them down.

Additionally, CI runners often operate in air-gapped or restricted-network environments where they cannot reach external services.

## Decision

Split Keystone into two runtime modes:

**1. CLI Orchestrator (local, in CI runner — separate `keystone-cli` Go repository)**
- Lightweight self-contained Go binary (<10MB)
- Performs spec parsing + diff locally using an embedded engine
- Maintains local cache of previous spec versions (keyed by checksum/commit SHA)
- Produces a verdict within 50ms for specs <1MB
- Exits immediately with status code (0=pass, 1=fail, 2=warn)
- Uploads results to Spring Boot server asynchronously (fire-and-forget)

**2. Keystone Server (server-side — this `keystone-server` Java/Spring repository)**
- Receives async audit uploads from the Go CLI binary
- Runs full policy evaluation and dependency graph updates
- Persists all analysis history in PostgreSQL
- Manages notifications and dashboard
- Can also receive direct webhook events from GitHub/GitLab

## Alternatives Considered

| Alternative | Pros | Cons | Reason Rejected |
|-------------|------|------|-----------------|
| Pure server-side (CLI is API client) | Simple architecture | Every build requires network call → 50-200ms minimum; fails in air-gapped networks | Cannot meet latency target |
| Pure local (no server) | Zero latency, fully air-gapped | No audit trail, no dashboard, no centralized policy management | Loses enterprise governance value |
| WebAssembly plugin in CI runner | Portable, sandboxed | Larger binary, complex build pipeline, limited library support | Over-engineering for initial scope |

## Consequences

### Positive
- CI pipelines get the verdict in <50ms (no blocking network call)
- Air-gapped deployments work without modification (local-only mode)
- Server-side audit is decoupled — failure doesn't block CI
- CLI can cache versions across CI runs for instant diffs

### Negative
- CLI must ship with an embedded diff engine (duplication of server-side logic)
- Local cache management adds complexity (cache invalidation, size limits, cleanup)
- Two deployment artifacts to maintain (CLI binary + server)

### Neutral
- CLI upload protocol must be versioned and backward-compatible
- Local mode and server mode can diverge in policy evaluation if cache is stale

## Implementation

**Affected Modules:**
- `.pi/architecture/modules/cli-orchestrator.md`

**CLI Binary Artifact:**
- Published as `keystone` CLI via npm package, Homebrew, or GitHub Releases
- Versioned independently from server (semver)
- Embedded diff engine mirrors server logic but optimized for size

## Validation

**Validators Required:**
- architecture-validator: Verify module boundaries (CLI does not depend on server modules)
- operations-validator: Verify <50ms latency target under load
- security-validator: Verify CLI binary does not contain server-side secrets

## References

- Related ADRs: ADR-008 (CI Integration Pattern)
- `.pi/architecture/diagrams/sequence-ci-flow.md`
- `.pi/architecture/diagrams/deployment-diagram.md`

---

*Decision date: 2026-06-12*
