# ADR-004: Event Sourcing for Audit Trail

**Status:** Accepted
**Date:** 2026-06-12
**Session:** 7bff170e-8b01-4621-9de1-4397f096b27a

## Context

NFR-009 requires an immutable audit trail: "Audit logs must be append-only and immutable." However, CI status updates transition through states (pending → success/failure/error), which looks like an update. A naive implementation would UPDATE rows, breaking immutability.

## Decision

Use **event sourcing** for all audit-relevant entities:

- Every state change is a new append-only event
- The latest state is derived by replaying events (or using a materialized view)
- No in-place UPDATEs on audit tables — only INSERTs

**Audit event types (all append-only):**

| Event | Context | Content |
|-------|---------|---------|
| SpecIngested | Contract Ingestion | Checksum of ingested content, repo, commit SHA |
| SpecParseFailed | Contract Ingestion | Parse error details, raw input excerpt |
| BreakingChangeReported | Breaking Change Analysis | Full report payload with changes |
| ComplianceVerdictReached | Policy Engine | Verdict, violations, policies evaluated |
| ExemptionGranted | Policy Engine | Exemption details, grantor, expiry |
| ExemptionRevoked | Policy Engine | Revocation reason, revoker |
| CiStatusUpdateCreated | Notification Engine | State (pending/success/failure/error), target URL |
| StakeholderNotified | Notification Engine | Channel, recipients, result |

**CiStatusUpdate specifically:**
- Each state change (pending → success, pending → failure, pending → error) is a new immutable `CiStatusUpdateCreated` event
- The notification channel reads the latest event (by timestamp) to determine current status
- No UPDATE on the CI status row — it's a time-ordered sequence of immutable events

## Alternatives Considered

| Alternative | Pros | Cons | Reason Rejected |
|-------------|------|------|-----------------|
| Traditional CRUD with timestamps | Simple implementation | Impossible to audit history; UPDATEs destroy previous state | Violates NFR-009 |
| Write-ahead log + current state table | Both audit log + fast reads | Dual-write complexity; eventual inconsistency risk | Acceptable but event sourcing is the cleaner DDD pattern |

## Consequences

### Positive
- Complete, immutable audit history
- Can replay events to reconstruct state at any point in time
- Naturally supports event-driven architecture (ADR-003)
- NFR-009 is fully satisfied

### Negative
- Higher storage requirements (events accumulate; mitigations: retention policies, archival)
- Querying current state requires replay or materialized views
- Team must learn event sourcing patterns and tooling

### Neutral
- Migration from CRUD to event sourcing later is difficult — committing now is the right call

## Implementation

**Affected Modules:**
- `.pi/architecture/modules/contract-ingestion.md` → AuditEventPublisher
- `.pi/architecture/modules/breaking-change-analysis.md` → ReportEventStore
- `.pi/architecture/modules/policy-engine.md` → PolicyEventStore
- `.pi/architecture/modules/notification-engine.md` → NotificationEventStore
- `.pi/architecture/modules/dashboard.md` → AuditLogReader

**Physical Data Model:**
```sql
CREATE TABLE audit_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type VARCHAR(64) NOT NULL,       -- e.g. 'SpecIngested', 'CiStatusUpdateCreated'
    aggregate_id VARCHAR(128) NOT NULL,     -- e.g. specId, reportId
    version INTEGER NOT NULL,               -- monotonic per aggregate
    payload JSONB NOT NULL,
    metadata JSONB,                         -- source context, timestamp, idempotencyKey
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_aggregate ON audit_events(aggregate_id, version);
CREATE INDEX idx_audit_type_time ON audit_events(event_type, created_at);
```

## Validation

**Validators Required:**
- security-validator: Verify no UPDATE/DELETE permissions on audit_events table
- architecture-validator: Verify all state changes emit events, not direct mutations
- operations-validator: Verify storage growth projections and retention policies

## References

- Related ADRs: ADR-003 (Event-Driven Communication), ADR-007 (Idempotency)
- `.pi/architecture/diagrams/data-model.md`

---

*Decision date: 2026-06-12*
