# Architecture Decision Records — Index

## Active ADRs

| ADR | Title | Status | Date | File |
|-----|-------|--------|------|------|
| 001 | Domain-Driven Design with Bounded Contexts (Modular Monolith) | Accepted | 2026-06-12 | `ADR-001-architecture-pattern.md` |
| 002 | Local/Remote Split — CLI Orchestrator Architecture | Accepted | 2026-06-12 | `ADR-002-cli-local-remote-split.md` |
| 003 | Event-Driven Communication Between Bounded Contexts | Accepted | 2026-06-12 | `ADR-003-event-driven-communication.md` |
| 004 | Event Sourcing for Audit Trail | Accepted | 2026-06-12 | `ADR-004-event-sourcing-audit.md` |
| 005 | Policy DSL Format and Authoring | Accepted | 2026-06-12 | `ADR-005-policy-dsl-format.md` |
| 006 | Dependency Graph Discovery Strategy | Accepted | 2026-06-12 | `ADR-006-dependency-graph-discovery.md` |
| 007 | Idempotency and Deduplication | Accepted | 2026-06-12 | `ADR-007-idempotency-deduplication.md` |
| 008 | CI Integration Pattern — Sync Local Verdict + Async Audit | Accepted | 2026-06-12 | `ADR-008-ci-integration-pattern.md` |

## Decision Log

| ADR | Decision | Rationale |
|-----|----------|-----------|
| 001 | Modular Monolith (not microservices) | Over-engineering for initial scope; extraction path is designed-in |
| 002 | Local/Remote split (not pure server-side) | <10ms constraint impossible remotely; air-gapped support needed |
| 003 | Domain events (not RPC) for cross-context | Loose coupling; eventual consistency acceptable for audit |
| 004 | Event sourcing (not CRUD) for audit log | Immutability requirement (NFR-009); CI status transitions as new events |
| 005 | Policy DSL as source of truth, UI commits to git | GitOps compatibility + compliance manager UX |
| 006 | Explicit keystone.yml for v1 (no auto-discovery) | OpenAPI doesn't declare consumers; auto-discovery is deferred to post-v1 |
| 007 | (repo, commit_sha, spec_path) as idempotency key | Natural composite key; handles webhook + CLI dual submission |
| 008 | Sync local verdict + async server check-run | <50ms CI feedback; detailed results in PR via GitHub Checks API |

## Superseded ADRs

*(none)*
