# Architecture Decision Record: ADR-005

<!--
Canonical Reference: .pi/architecture/decisions/ADR-005-domain-events.md
Blueprint Source: Guardian Framework v1.2
-->

## Title

Cross-Context Communication via Domain Events

## Status

- [x] Accepted

## Context

The six bounded contexts need to communicate with each other without creating direct dependencies. The backend uses Spring Events for in-process domain event publishing. The frontend must consume these events indirectly — it reads from the REST API, but the event-driven nature of the backend has implications for how and when data appears in the frontend.

The key event-driven flows are:

```
SpecIngestedEvent → BreakingChangeReportedEvent → PolicyEvaluatedEvent → NotificationSentEvent
SpecIngestedEvent → DependencyAddedEvent → DownstreamImpactComputedEvent → NotificationSentEvent
PolicySourceChangedEvent → PolicySyncedEvent → PolicyEvaluatedEvent
```

The Dashboard context aggregates data from all other contexts — it reads from their repositories/services to compute health scores and compliance summaries.

**Tech Stack:** TypeScript (Bun/Node), Next.js (App Router)

## Decision

### 1. Domain Events for Backend Cross-Context Communication

Each bounded context publishes domain events when significant state changes occur. These are:

| Event | Publisher Context | Consumer Contexts |
|-------|------------------|-------------------|
| SpecIngestedEvent | Contract Ingestion | Breaking Change Analysis, Dashboard |
| BreakingChangeReportedEvent | Breaking Change Analysis | Policy Engine, Notification Engine, Dashboard |
| PolicyEvaluatedEvent | Policy Engine | Notification Engine, Dashboard |
| DownstreamImpactComputedEvent | Dependency Graph | Notification Engine, Dashboard |
| NotificationSentEvent | Notification Engine | Dashboard |
| DependencyAddedEvent | Dependency Graph | Dashboard |
| PolicySyncedEvent | Policy Engine | Dashboard |
| HealthScoreRecalculatedEvent | Dashboard | (internal / audit) |

### 2. In-Process Event Bus

Initially, all domain events use an **in-process event bus** (typed EventEmitter pattern). This avoids the operational complexity of a message queue while keeping the event contract clean. The event bus is:

- Typed: each event has a TypeScript interface
- Synchronous: event handlers run in the same process tick
- Subscriber-based: contexts subscribe to events they care about

### 3. Frontend Consumption

The frontend does NOT consume domain events directly. Instead:
- It fetches current state via REST API on view load / switch
- Unread notification badges and live indicators can use periodic polling (30s)
- The backend's event-driven processing guarantees that by the time the frontend fetches, the relevant aggregates are already updated

### 4. Event Schema Convention

Every domain event follows this shape:

```typescript
interface DomainEvent {
  eventId: string;          // UUID
  eventType: string;        // e.g. "spec.ingested"
  context: string;          // producer bounded context
  aggregateId: string;      // the aggregate root ID
  timestamp: string;        // ISO 8601
  payload: Record<string, unknown>;  // event-specific data
}
```

## Alternatives Considered

| Alternative | Pros | Cons | Reason Rejected |
|-------------|------|------|-----------------|
| Direct method calls between contexts | Simple, type-safe | Creates compile-time coupling; violates bounded context isolation | Rejected — defeats the purpose of bounded contexts |
| Message queue (RabbitMQ, Kafka) | Reliable delivery, async processing, durability | Operational overhead; over-engineered for single-process deployment | Rejected — start simple, extract to queue later if needed |
| Shared database | Simplest querying | Tight coupling; schema changes cascade; no domain boundaries | Rejected — violates DDD principles |
| REST API calls between contexts | Language-agnostic, well-understood | Latency overhead; synchronous coupling; circular call risk | Rejected — better suited for external integrations, not internal cross-context |

## Consequences

### Positive
- Loose coupling — contexts only depend on event contracts
- Clear audit trail — all state changes are published as events
- Event schema is testable and versionable
- Can promote to message queue without changing domain logic

### Negative
- Eventual consistency — a view may show slightly stale data after an event
- Event handler failures must be handled (retry, dead-letter)
- Debugging event chains requires tracing infrastructure
- No built-in ordering guarantees (events are processed in subscriber order)

### Neutral
- Event contracts serve as cross-context documentation
- The Dashboard context acts as an event sink — it subscribes to most events

## Implementation

**Affected Modules:**
- All six module docs (every context publishes/subscribes to events)

**Files to Update:**
- `src/shared/events/event-bus.ts` — Typed EventEmitter + subscriber registration
- `src/shared/events/types.ts` — All domain event interfaces
- `src/shared/events/schema.ts` — Event envelope + validation
- `src/modules/*/domain/events/` — Context-specific event definitions

## Validation

**Validators Required:**
- architecture-validator: Verify event contracts, subscriber registration, no cross-context direct calls
- integration-validator: Verify event chains produce correct state changes

## References

- Related ADRs: ADR-001 (DDD with Bounded Contexts — establishes module isolation)
- Domain exploration: `.pi/domain/exploration.md` #domain-events

---

*Decision date: 2026-06-13*
*Decision makers: System Architect*
