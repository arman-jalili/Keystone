# System Context Diagram

## Context

We are building **Keystone**, an enterprise API contract governance platform.
Detects breaking changes in OpenAPI specs before production.

## Repository Split

- **keystone-server** (this repo): Java 21 + Spring Boot — all server-side bounded contexts
- **keystone-cli** (separate repo): Go binary — local CI runner analysis

## Bounded Contexts Flow

```mermaid
graph TB
    subgraph "CI Runner (seperate repo: keystone-cli)"
        CLI[CLI Orchestrator<br/>Go Binary]
    end

    subgraph "Keystone Server (this repo: Java 21 + Spring Boot)"
        CI[Contract Ingestion]
        BCA[Breaking Change Analysis]
        PE[Policy Engine]
        PS[Policy Sync Service<br/>← Git → DB cache]
        NE[Notification Engine]
        DG[Dependency Graph]
        DB[Dashboard]
    end

    subgraph "External"
        GH[GitHub]
        GL[GitLab]
        SL[Slack / Email]
        PG[(Git Policy Repository<br/>Source of Truth)]
    end

    %% Local flow
    GH -- "webhook / commit status" --> CLI
    CLI -- "async upload" --> CI

    %% Server-side event chain (Spring ApplicationEventPublisher)
    CI -- "SpecIngested" --> BCA
    BCA -- "BreakingChangeReported" --> PE
    PE -- "ComplianceVerdictReached" --> NE

    %% Policy source of truth
    PG -- "webhook / poll" --> PS
    PS -- "syncs to cache" --> PE
    DB -. "commits policy changes" .-> PG

    %% Notifications
    NE -- "CiStatusUpdated" --> GH
    NE -- "CiStatusUpdated" --> GL
    NE -- "StakeholderNotified" --> SL

    %% Cross-cutting
    BCA -. "queries" .-> DG
    DG -. "registers services" .-> CI
    DB -. "reads all" .-> CI
    DB -. "reads all" .-> BCA
    DB -. "reads all" .-> PE
    DB -. "reads all" .-> NE
    DB -. "reads all" .-> DG

    classDef local fill:#e1f5fe,stroke:#0288d1
    classDef server fill:#f3e5f5,stroke:#7b1fa2
    classDef external fill:#fff3e0,stroke:#f57c00
    class CLI local
    class CI,BCA,PE,PS,NE,DG,DB server
    class GH,GL,SL,PG external
```

## Event Flow (Server-Side)

```
CLI HTTP upload → Contract Ingestion
    → SpecIngested event (Spring ApplicationEventPublisher)
    → Breaking Change Analysis
    → BreakingChangeReported event
    → Policy Engine (reads from DB cache; source of truth is Git)
    → ComplianceVerdictReached event
    → Notification Engine
    → CiStatusUpdated (GitHub/GitLab) + StakeholderNotified (Slack/Email)
```

## Key Architectural Decisions

- **Java 21 + Spring Boot** for full Guardian validator support (package rings, @Transactional, @PreAuthorize)
- **CLI is a separate Go project** — keeps binary lean, no JVM dependency in CI runner
- **Policy source of truth = Git repository** — database is a read-through cache synced by PolicySyncService
- **In-process event bus** (Spring `ApplicationEventPublisher`) for v1 modular monolith
- **Single PostgreSQL instance** with logical schemas per bounded context
- **Dependency Graph** uses explicit `keystone.yml` declarations in v1 (no automated discovery)

---

*Generated from session: 7bff170e-8b01-4621-9de1-4397f096b27a*
*Date: 2026-06-12*
*Updated: 2026-06-12 — Java/Spring stack, separate CLI repo, Git policy source, single PostgreSQL*
