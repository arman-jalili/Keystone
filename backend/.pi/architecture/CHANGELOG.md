# Architecture Change Log

**Canonical Reference:** `.pi/architecture/CHANGELOG.md`

This document tracks all architecture changes requiring implementation updates.
Each entry follows the format: Title, Changes, Impact Analysis, Status.

---

## [2026-06-12] - contract-ingestion Implemented

### Added
- All 6 interfaces frozen, implemented, and tested (37 tests, all passing)
- IngestionController REST API with /audit, /webhook/github, /idempotency endpoints
- IngestionServiceImpl orchestrating dedup → validate → persist → publish
- DeduplicationFilterImpl using IdempotencyStore (ADR-007)
- SpecValidatorImpl using swagger-parser for OpenAPI 3.x validation
- SpecRepositoryImpl with JPA entities (OpenApiSpecEntity, SpecVersionEntity)
- IngestionEventPublisherImpl via Spring ApplicationEventPublisher
- IdempotencyStoreImpl with TTL cleanup support
- CI proofing scripts: contract validation, coverage enforcement (stage 11)
- Runbook: docs/runbook-contract-ingestion.md
- DR plan: docs/dr-plan-contract-ingestion.md
- Observability: IngestionHealthIndicator, IngestionMetrics (Micrometer)
- Health endpoint with DB connectivity check

### Status
- Contract freeze: COMPLETE
- Implementation: DONE
- CI enforcement: STAGE 11

---

## [2026-06-12] - Initial Architecture Scaffold

### Added
**Module: CLI Orchestrator**
- Created module doc: components (SpecParser, LocalCache, LocalDiffEngine, Uploader, CliMain), TypeScript interfaces, data flow
- Defined exit code contract (0=pass, 1=fail, 2=warn, 3+=error)

**Module: Contract Ingestion**
- Created module doc: DeduplicationFilter with (repo, commit_sha, spec_path) idempotency
- Spec ingestion from two sources: CLI async upload + GitHub/GitLab webhooks

**Module: Breaking Change Analysis**
- Created module doc: 7 built-in change detector plugins (PathRemoval, MethodRemoval, RequiredFieldAdded, FieldRemoval, FieldTypeChanged, OptionalFieldAdded, DeprecatedField)
- Three-layer base version resolver (explicit ref > previous ingested > main branch)

**Module: Policy Engine**
- Created module doc: Policy DSL YAML format with conditions, actions, targets, exemptions
- Pluggable PolicySource interface (file, HTTP, git, OCI registry)

**Module: Notification Engine**
- Created module doc: ChannelRegistry with circuit breaker pattern for external API calls
- Three channels: CI status (GitHub/GitLab), email, Slack

**Module: Dependency Graph**
- Created module doc: keystone.yml declaration format for v1 service registration
- Impact analysis via BFS graph traversal

**Module: Dashboard**
- Created module doc: Governance health score formula
- RBAC-filtered views per actor role

**Decision Records (8 ADRs)**
- ADR-001: Domain-Driven Design with Bounded Contexts (Modular Monolith)
- ADR-002: Local/Remote Split — CLI Orchestrator Architecture
- ADR-003: Event-Driven Communication Between Bounded Contexts
- ADR-004: Event Sourcing for Audit Trail
- ADR-005: Policy DSL Format and Authoring
- ADR-006: Dependency Graph Discovery Strategy
- ADR-007: Idempotency and Deduplication
- ADR-008: CI Integration Pattern — Sync Local Verdict + Async Audit

**Diagrams (6 C4-model documents)**
- system-context.md (C4 Level 1 — actors, external systems, system boundary)
- system-overview.md (C4 Level 2 — container diagram with 7 services, data stores, message bus)
- sequence-ci-flow.md (Dynamic — end-to-end CI pipeline sequence with phase timings)
- domain-event-flow.md (Dynamic — 10 domain events with JSON payload schemas)
- deployment-diagram.md (C4 Level 3 — K8s topology, resource requests, network ports, resilience)
- data-model.md (Entity-relationship diagram with 10 entities, constraints, indices)

### Changed
- **Performance target NFR-001:** `<10ms` → `50ms p99 local / 1s async remote upload`
- **CI integration model:** `remote API call` → `local CLI binary + async audit upload`
- **Audit model NFR-009:** `CRUD with timestamps` → `Event sourcing (append-only events, no UPDATE)`
- **Cross-context communication:** `direct API calls` → `Domain events via message bus`
- **Architecture pattern:** `monolith` → `Modular Monolith with extraction path to microservices`

### Fixed
- **Tech stack:** Node.js/Go → Java 21 + Spring Boot for `keystone-server`; Go for `keystone-cli` (separate repo)
- **CLI is separate project:** CLI Orchestrator moved to its own `keystone-cli` Go repository
- **Policy source of truth:** Database → Git repository. Added PolicySyncService to sync Git → DB cache
- **API Gateway renamed:** Separate API Gateway container → Spring Boot HTTP Server (entry point router)
- **Multiple PostgreSQL instances:** Single PostgreSQL instance with logical schemas per bounded context
- **Message bus clarified:** In-process Spring `ApplicationEventPublisher` for v1; Redis/Kafka as future evolution
- **Dependency Graph v1 scope noted:** Only explicit `keystone.yml` declarations; no automated discovery
- **All diagrams, ADRs, and module docs** updated to reflect Java/Spring stack and Git policy source
- **Code language in module docs:** All TypeScript code snippets replaced with Java 21 (server) and Go (CLI)
  - `.ts` file paths → `.java` (server) and `.go` (CLI)
  - TypeScript interfaces → Java interfaces with Spring annotations and Go interfaces with structs
  - npm dependencies → Maven/Gradle (kin-openapi for CLI, Spring Boot starters for server)
  - `import` statements → Java `package` declarations and Go `import`
  - Error handling: `class SpecParseError extends Error` → `public class SpecParseException extends RuntimeException` (Java) and `type SpecParseError struct` (Go)
  - All module docs now use Mermaid sequence diagrams with proper Java/Go class names
- **Canonical anchors:** All sections now have explicit `{#section-id}` anchors matching ADR references

### Impact Analysis
- **Files affected (new):**
  - `.pi/architecture/modules/cli-orchestrator.md`
  - `.pi/architecture/modules/contract-ingestion.md`
  - `.pi/architecture/modules/breaking-change-analysis.md`
  - `.pi/architecture/modules/policy-engine.md`
  - `.pi/architecture/modules/notification-engine.md`
  - `.pi/architecture/modules/dependency-graph.md`
  - `.pi/architecture/modules/dashboard.md`
  - `.pi/architecture/decisions/ADR-001.md` through `ADR-008.md`
  - `.pi/architecture/diagrams/system-context.md`, `system-overview.md`, `sequence-ci-flow.md`, `domain-event-flow.md`, `deployment-diagram.md`, `data-model.md`
- **Documents updated:**
  - `.pi/domain/exploration.md` — all 8 tables filled, 20 glossary terms, 7 bounded contexts
  - `.pi/domain/ubiquitous-language.md` — 20 glossary terms with code examples
- **Validators required:**
  - architecture-validator: Verify module boundaries, event flow, and aggregate root invariants
  - security-validator: Verify RBAC model, encryption, webhook signature verification
  - operations-validator: Verify <50ms CLI latency, event sourcing storage projections
  - integration-validator: Verify event delivery, check-run integration, dedup under load

### Status
- [x] Architecture docs created (8 ADRs, 7 modules, 6 diagrams)
- [x] Domain exploration validated (20 terms, all sections pass)
- [x] CHANGELOG entry created
- [ ] Source implementation started
- [ ] Canonical refs added to source files
- [ ] Validators run

---

## Architecture Sync Status

| Date | Change | Affected Module | Sync Status | Notes |
|------|--------|----------------|-------------|-------|
| 2026-06-12 | Initial scaffold — all ADRs, modules, diagrams | All 7 modules | Pending | Ready for implementation |
| — | — | — | — | — |

---

*Last updated: 2026-06-12*
*Architecture version: v1.0.0*
