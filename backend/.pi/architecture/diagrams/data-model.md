# Data Model — Entity Relationships

> Logical data model across all bounded contexts. Physical schema may differ (e.g., event store vs relational).

```mermaid
erDiagram
    %% Contract Ingestion
    OpenApiSpec {
        uuid id PK
        string repository
        string specPath
        string owningService
        datetime createdAt
        jsonb rawSpec
        jsonb parsedEndpoints
    }

    SpecVersion {
        uuid id PK
        uuid specId FK
        string commitSha UK
        string version
        datetime ingestedAt
        text checksum
        jsonb parsedModel
    }

    %% Breaking Change Analysis
    BreakingChangeReport {
        uuid id PK
        uuid baseVersionId FK
        uuid targetVersionId FK
        string verdict
        int changeCount
        datetime createdAt
    }

    Change {
        uuid id PK
        uuid reportId FK
        string severity
        string path
        string method
        text description
        jsonb details
    }

    %% Policy Engine
    Policy {
        uuid id PK
        string name UK
        string environment
        string state
        jsonb spec
        datetime createdAt
        datetime updatedAt
    }

    ComplianceResult {
        uuid id PK
        uuid reportId FK
        uuid policyId FK
        string verdict
        jsonb violations
        datetime evaluatedAt
    }

    Exemption {
        uuid id PK
        string changeId
        uuid policyId FK
        datetime expiresAt
        string grantedBy
        string reason
        datetime createdAt
    }

    %% Notification Engine
    Notification {
        uuid id PK
        string channel
        string status
        jsonb payload
        int retryCount
        datetime createdAt
        datetime deliveredAt
    }

    %% Dependency Graph
    Service {
        uuid id PK
        string name UK
        string team
        jsonb metadata
    }

    ApiDependency {
        uuid id PK
        uuid producerId FK
        uuid consumerId FK
        uuid specId FK
        datetime discoveredAt
    }

    %% Dashboard
    AuditEntry {
        uuid id PK
        string action
        string actor
        string target
        jsonb payload
        datetime timestamp
    }

    %% Relationships
    OpenApiSpec ||--o{ SpecVersion : "has versions"
    SpecVersion ||--o{ BreakingChangeReport : "base version"
    SpecVersion ||--o{ BreakingChangeReport : "target version"
    BreakingChangeReport ||--o{ Change : "contains"
    BreakingChangeReport ||--o{ ComplianceResult : "evaluated in"
    Policy ||--o{ ComplianceResult : "produces"
    Policy ||--o{ Exemption : "has"
    Service ||--o{ ApiDependency : "produces (as producer)"
    Service ||--o{ ApiDependency : "consumes (as consumer)"
    OpenApiSpec ||--o{ ApiDependency : "referenced by"
```

## Entity Type Summary

| Entity | Type | Context | Persistence |
|--------|------|---------|------------|
| OpenApiSpec | Aggregate Root | Contract Ingestion | PostgreSQL |
| SpecVersion | Value Object | Contract Ingestion | PostgreSQL (part of OpenApiSpec) |
| ParsedEndpoint | Value Object | Contract Ingestion | JSONB in SpecVersion |
| BreakingChangeReport | Aggregate Root | Breaking Change Analysis | PostgreSQL |
| Change | Entity | Breaking Change Analysis | PostgreSQL |
| ChangeSeverity | Value Object | Breaking Change Analysis | Enum in Change |
| Policy | Aggregate Root | Policy Engine | PostgreSQL |
| PolicyRule | Value Object | Policy Engine | JSONB in Policy |
| ComplianceResult | Value Object | Policy Engine | PostgreSQL |
| Exemption | Entity | Policy Engine | PostgreSQL |
| Notification | Aggregate Root | Notification Engine | PostgreSQL |
| CiStatusUpdate | Value Object | Notification Engine | JSONB in Notification |
| ApiDependency | Aggregate Root | Dependency Graph | PostgreSQL |
| Service | Entity | Dependency Graph | PostgreSQL |
| ImpactAnalysisResult | Value Object | Dependency Graph | JSONB (ephemeral) |
| GovernanceHealthScore | Value Object | Dashboard | Computed (no persistence) |
| AuditEntry | Entity | Dashboard | PostgreSQL (`audit` schema, append-only event store) |
| LocalAnalysisRequest | Value Object | CLI Orchestrator | Ephemeral (in-memory, in `keystone-cli` Go binary) |
| CachedSpec | Value Object | CLI Orchestrator | Local filesystem (in `keystone-cli` binary) |
| LocalDiffResult | Value Object | CLI Orchestrator | Ephemeral (stdout, in `keystone-cli` binary) |

## Physical Database Layout

All server-side bounded contexts share a **single PostgreSQL instance** with **logical schemas** for isolation:

```
PostgreSQL (keystone_db)
├── ingestion     → OpenApiSpec, SpecVersion
├── analysis      → BreakingChangeReport, Change
├── policy        → Policy, ComplianceResult, Exemption (cache only; source of truth = Git)
├── notifications → Notification
├── graph         → Service, ApiDependency
└── audit         → AuditEntry (event store)
```

**Rules:**
- Each Spring `@Service` only accesses its own schema via a dedicated `DataSource` bean
- Cross-schema queries are prohibited at the application layer (use domain events instead)
- The `audit` schema is append-only — INSERT only, no UPDATE/DELETE
- The `policy` schema is a cache — the Git repository is the source of truth

## Key Constraints & Indices

| Table | Index | Type | Purpose |
|-------|-------|------|---------|
| spec_versions | (spec_id, commit_sha) | UNIQUE | Idempotency key for dedup |
| policies | (name, environment) | UNIQUE | No duplicate policy names per env |
| services | name | UNIQUE | Single registration per service |
| api_dependencies | (producer_id, consumer_id, spec_id) | UNIQUE | No duplicate dependency edges |
| audit_entries | timestamp | BRIN | Time-range queries for dashboard |
| changes | report_id | B-tree | Fast lookup by report |
| exemptions | (policy_id, change_id) | UNIQUE | No duplicate exemptions for same change |
