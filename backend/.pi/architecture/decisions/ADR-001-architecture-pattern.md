# ADR-001: Domain-Driven Design with Bounded Contexts

**Status:** Accepted
**Date:** 2026-06-12
**Session:** 7bff170e-8b01-4621-9de1-4397f096b27a

## Context

The domain exploration identified 7 bounded contexts that must be implemented as independently evolvable modules. Two separate codebases are required:

**Keystone Server** (this repository):
- Contract Ingestion
- Breaking Change Analysis
- Policy Engine
- Notification Engine
- Dependency Graph
- Dashboard

**Keystone CLI** (separate repository):
- CLI Orchestrator — local CI runner analysis (<50ms latency target)

Key architectural constraints:
- CI overhead target: 50ms local, 1s async remote upload
- Must integrate with GitHub/GitLab commit status APIs
- Decoupled local analysis from server-side audit persistence
- Event sourcing for immutable audit trail (NFR-009)
- Must leverage Guardian framework's full validation capability

## Decision

### Technology Stack

**Keystone Server (this repository):** Java 21 + Spring Boot 3.x
- Spring WebFlux for async HTTP handling
- Spring Data JPA / R2DBC for database access
- Spring Modulith for bounded context modularity (package-level enforcement)
- Domain events via Spring ApplicationEventPublisher (in-process) → future migration to Redis Streams
- Guardian validators: package rings, @Transactional, @PreAuthorize, canonical references, layer compliance

**Keystone CLI (separate repository):** Go
- Lightweight self-contained binary distributed to CI runners
- Guardian validators: golangci-lint, go test, 7 generic validators
- Communicates with server via HTTP (async audit upload)

### Architecture Pattern

We will use **Domain-Driven Design** with bounded contexts as independently evolvable modules following the **Modular Monolith** pattern:

1. **Code-level modularity:** Each bounded context is a separate Spring package with Spring Modulith enforcement. Packages follow a strict dependency direction — inner layers (domain) must not depend on outer layers (infrastructure). Guardian's package ring validators enforce this.

2. **Cross-context communication:** Domain events via Spring `ApplicationEventPublisher` for asynchronous flows; Spring service beans for synchronous queries within the same JVM.

3. **Local/Remote split:** CLI Orchestrator is a separate Go binary in its own repository. All server modules run in a single Spring Boot application.

4. **Event sourcing:** Audit trail is an append-only event stream (no UPDATE). Latest state is derived from the newest event.

5. **Extraction path:** Spring Modulith allows any bounded context to be extracted to a separate microservice without architectural refactoring.

6. **Policy source of truth:** Git repository (YAML DSL) is the source of truth. The database is a read-through cache synced via a Policy Sync Service.

## Consequences

### Positive

- Full Guardian framework validation: package rings, @Transactional, @PreAuthorize, canonical refs, layer compliance
- Spring Modulith enforces bounded context boundaries at the package level
- Event sourcing provides an immutable, auditable history
- Separate CLI repository keeps the binary lean (no server dependencies)
- Spring WebFlux enables non-blocking async processing

### Negative

- Disciplined dependency management required — Spring Modulith enforces this at build time
- CLI (Go) and server (Java) are different languages — team must maintain both
- Modular monolith eventual consistency between contexts
- Message bus (in-process vs Redis) abstraction must be clean for future extraction

### Risks

| Risk | Mitigation |
|------|-----------|
| Modular monolith grows too large | Spring Modulith extraction path is designed-in; extract to separate service + message broker |
| Domain events schema evolution | Versioned events in Java records with backward-compatible serialization |
| CLI Orchestrator bloat | Strict single-responsibility: parse, diff, upload. No server logic. |
| Policy Git sync latency | Webhook triggers near-instant sync; poll fallback every 60s |
| Single PostgreSQL instance contention | Logical schemas isolate bounded contexts; connection pooling per schema |

## Alternatives Considered

| Pattern | Rejected Because |
|---------|-----------------|
| Node.js / Express server | Guardian loses Java/Spring-specific validators (package rings, @Transactional, @PreAuthorize) |
| Go for both server and CLI | Guardian's strongest validators target Java/Spring Boot |
| Microservices from day one | Over-engineering for initial scope; network overhead conflicts with 50ms target |
| Server-only architecture (no local CLI) | Cannot meet <10ms overhead requirement; every build requires a remote call |
| Database as policy source of truth | Violates GitOps principle; no version-controlled policy history |

## Event Flow Architecture

```
┌─────────────────────────────────────────────────────┐
│  CI Runner (separate repo: keystone-cli)             │
│  ┌─────────────────────────────────────────────┐    │
│  │  keystone analyze --spec=openapi.yaml        │    │
│  │  → Parse → Diff → LocalDiffResult (~42ms)    │    │
│  │  → Exit code 0/1/2  ──[async HTTP]──→ Server │    │
│  └─────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────┘
                          │ HTTP POST /api/v1/audit
                          ▼
┌─────────────────────────────────────────────────────┐
│  Keystone Server (this repo: Java 21 + Spring Boot)  │
│                                                       │
│  Contract Ingestion → BreakingChangeAnalysis         │
│       → PolicyEngine → NotificationEngine            │
│       → DependencyGraph, Dashboard                   │
│                                                       │
│  Events: Spring ApplicationEventPublisher            │
│  Database: PostgreSQL (logical schemas per context)   │
│  Policies: Git repo → PolicySyncService → cache      │
└─────────────────────────────────────────────────────┘
```

## Module Boundaries

| Module | Project | Data Store | Tech | Guardian Validators |
|--------|---------|------------|------|-------------------|
| CLI Orchestrator | `keystone-cli` (separate) | Local filesystem cache | Go | golangci-lint, go test, generic |
| Contract Ingestion | `keystone-server` | PostgreSQL (schema: `ingestion`) | Java/Spring | Package rings, @Transactional |
| Breaking Change Analysis | `keystone-server` | PostgreSQL (schema: `analysis`) | Java/Spring | Package rings, canonical refs |
| Policy Engine | `keystone-server` | PostgreSQL (schema: `policy`) + Git repo | Java/Spring | @PreAuthorize, package rings |
| Notification Engine | `keystone-server` | PostgreSQL (schema: `notifications`) | Java/Spring | @Transactional, circuit breaker |
| Dependency Graph | `keystone-server` | PostgreSQL (schema: `graph`) | Java/Spring | Package rings, canonical refs |
| Dashboard | `keystone-server` | PostgreSQL (read replicas) | Java/Spring | @PreAuthorize, layer compliance |

## Guardian Validation

| Validator | What It Enforces |
|-----------|-----------------|
| Package rings | No circular dependencies between bounded contexts |
| @Transactional | All database operations within transactional boundaries |
| @PreAuthorize | RBAC on all API endpoints and service methods |
| Canonical references | Source code matches architecture docs |
| Layer compliance | Domain layer doesn't depend on infrastructure layer |
| CI validators | Build, test, lint pass in CI pipeline |
| Security validator | Injection, auth bypass, secret leakage checks |

## Affected Modules

- **This repository (keystone-server):** Contract Ingestion, Breaking Change Analysis, Policy Engine, Notification Engine, Dependency Graph, Dashboard
- **Separate repository (keystone-cli):** CLI Orchestrator

---

*Amended: 2026-06-12 — Added Java 21 + Spring Boot technology stack; CLI is a separate Go project; Policy source of truth is Git repo; Guardian validator references*
