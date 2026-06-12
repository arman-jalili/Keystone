# ADR-007: Idempotency and Deduplication

**Status:** Accepted
**Date:** 2026-06-12
**Session:** 7bff170e-8b01-4621-9de1-4397f096b27a

## Context

NFR-002 requires 100+ concurrent CI pipeline executions. Without idempotency, the same commit can trigger duplicate analysis via:
- Multiple CI pipeline re-runs (retry, manual re-run)
- Both webhook and CLI upload for the same commit
- GitHub/GitLab webhook retries from the same event

Duplicate analysis wastes compute, creates duplicate audit entries, and could post duplicate CI status updates.

## Decision

**Idempotency key:** Composite key = `(repository, commit_sha, spec_path)`

**Enforcement points:**

**1. CLI Orchestrator (local):**
- Uses local filesystem lock per `(repo, spec_path)` to prevent concurrent CLI invocations on the same spec within the same CI runner
- Includes idempotency key in the upload payload for server-side dedup

**2. Contract Ingestion (server-side):**
- `DeduplicationFilter` checks if `(repo, commit_sha, spec_path)` already processed
- If duplicate: return existing `SpecIngested` event ID, skip processing
- Key stored with TTL (7 days by default) to limit storage growth

```typescript
interface IdempotencyKey {
  repository: string;    // e.g. "org/repo"
  commitSha: string;     // full SHA from git
  specPath: string;      // relative path within repo, e.g. "openapi/checkout.yaml"
}

interface DeduplicationFilter {
  isDuplicate(key: IdempotencyKey): Promise<boolean>;
  markProcessed(key: IdempotencyKey, eventId: string): Promise<void>;
  // Returns the original eventId if already processed
}
```

**3. Notification Engine:**
- GitHub/GitLab commit status API is idempotent by design (same state + description → no-op)
- Email/Slack deduplication: cooldown period (5 minutes per `(service, change_type)` tuple) prevents alert storms

**Error handling:**
- If idempotency check fails (database error): fail open — process the spec but log a warning
- If TTL expires and key is re-inserted: treat as new analysis (spec may have genuinely changed back to same state)
- Local filesystem lock timeout: 30 seconds; if exceeded, proceed without lock (degraded mode)

## Alternatives Considered

| Alternative | Pros | Cons | Reason Rejected |
|-------------|------|------|-----------------|
| No deduplication (process everything) | Simple implementation | Duplicate audit records; wasted compute; duplicate CI statuses | Unacceptable for enterprise audit |
| UUID-based idempotency (client generates) | Simple | Client must generate and remember UUID; webhooks don't carry one | Relies on client discipline |
| Git SHA + timestamp composite | More unique | Ties to clock synchronization; more storage | Composite key with SHA alone is sufficient |

## Consequences

### Positive
- Safe to retry CI pipelines without duplicate governance results
- Webhook + CLI dual submission is handled gracefully (webhook triggers server, CLI upload is deduped)
- Idempotency key serves as natural database primary key for fast lookups

### Negative
- Idempotency check adds ~5ms per upload (indexed lookup) — acceptable within budget
- TTL expiration (7 days) could theoretically allow duplicate if same spec is re-pushed after 7+ days (extremely unlikely in practice)
- Local filesystem locks can cause issues in shared CI runner environments (mitigation: lock timeout + fallback)

## Implementation

**Affected Modules:**
- `.pi/architecture/modules/cli-orchestrator.md`
- `.pi/architecture/modules/contract-ingestion.md`
- `.pi/architecture/modules/notification-engine.md`

**Database schema (deduplication table):**
```sql
CREATE TABLE idempotency_keys (
    repository VARCHAR(256) NOT NULL,
    commit_sha VARCHAR(64) NOT NULL,
    spec_path VARCHAR(512) NOT NULL,
    event_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (repository, commit_sha, spec_path)
);

-- TTL cleanup: background job deletes rows older than 7 days
CREATE INDEX idx_idempotency_cleanup ON idempotency_keys(created_at);
```

## Validation

**Validators Required:**
- integration-validator: Verify dedup works under concurrent load (100+ parallel uploads)
- test-validator: Verify TTL edge cases, lock timeout behavior, fail-open scenario

## References

- Related ADRs: ADR-002 (CLI uploads carry idempotency key), ADR-004 (Event sourcing audit keys)
- `.pi/architecture/modules/contract-ingestion.md#DeduplicationFilter`

---

*Decision date: 2026-06-12*
