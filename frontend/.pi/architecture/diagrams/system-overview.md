# System Architecture Overview

<!--
Canonical Reference: .pi/architecture/diagrams/system-overview.md
Blueprint Source: Guardian Framework v1.2
-->

## High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                         External Actors                              │
│   API Developer (CLI/CI)   Compliance Manager   Repository Owner    │
└─────────────────────────────────────────────────────────────────────┘
                              │                        │
              ┌───────────────┘                        ▼
              ▼                            ┌─────────────────────┐
     ┌────────────────┐                    │  Keystone Dashboard │
     │  Keystone CLI  │                    │  (Next.js / RSC)   │
     │  (spec upload) │                    └─────────────────────┘
     └────────────────┘                              │
              │                                      │
              └──────────┬───────────────────────────┘
                         ▼
              ┌─────────────────────┐
              │   REST API Gateway  │  /api/v1/*
              │   (Spring Boot)     │
              └─────────────────────┘
                         │
                         ▼
        ┌────────────────────────────────────────────────┐
        │           Business Logic Layer                  │
        │  ┌──────────────┐  ┌────────────────────────┐  │
        │  │ Contract     │  │ Breaking Change        │  │
        │  │ Ingestion    │  │ Analysis               │  │
        │  └──────────────┘  └────────────────────────┘  │
        │  ┌──────────────┐  ┌────────────────────────┐  │
        │  │ Policy       │  │ Dependency Graph       │  │
        │  │ Engine       │  │                        │  │
        │  └──────────────┘  └────────────────────────┘  │
        │  ┌──────────────┐  ┌────────────────────────┐  │
        │  │ Dashboard    │  │ Notification Engine    │  │
        │  └──────────────┘  └────────────────────────┘  │
        └────────────────────────────────────────────────┘
                         │
                         ▼
              ┌─────────────────────┐
              │      Data Layer      │
              │  ┌──────┐ ┌──────┐  │
              │  │PostgreSQL │ H2 │  │
              │  │(prod)│ │(dev)│  │
              │  └──────┘ └──────┘  │
              └─────────────────────┘
```

---

## Six Bounded Contexts (Backend Data Sources)

| Backend Context | Responsibility | Frontend Consumption |
|----------------|---------------|---------------------|
| Contract Ingestion | OpenAPI spec ingestion, parsing, versioning, dedup | API Inventory view (`GET /ingestion/apis`) |
| Breaking Change Analysis | Diff analysis, change detection, verdict generation | Breaking Changes view (`GET /breaking/reports`) |
| Policy Engine | Policy CRUD, DSL evaluation, exemptions, Git sync | Policy Compliance view (`GET /policies`, `POST /policies`) |
| Dependency Graph | Service registry, dependency edges, BFS impact analysis | Dependency Graph view (`GET /graph/services`, `POST /graph/impact`) |
| Dashboard | Health scores, compliance, audit, violation trends | Overview view (`GET /dashboard/*`) |
| Notification Engine | Multi-channel dispatch, delivery tracking, circuit breaker | Notifications view (`GET /notifications/*`) |

> **Note:** These are backend-only bounded contexts. The frontend does not reimplement them — it is a single frontend-app module that reads from these endpoints. See `.pi/architecture/modules/frontend-app.md`.

---

---

## Cross-Context Event Flow

```
 SpecIngestedEvent
     │
     ├──→ BreakingChangeAnalysis
     │         │
     │         ├──→ BreakingChangeReportedEvent
     │         │       │
     │         │       ├──→ Policy Engine (evaluate spec)
     │         │       ├──→ Dependency Graph (impact analysis)
     │         │       └──→ Notification Engine (alert stakeholders)
     │         │
     │         └──→ Dashboard (update health score)
     │
     └──→ Dashboard (ingestion metrics)
```

---

## Frontend Architecture

```
app/
├── layout.tsx          ← AppLayout (Server Component)
│   ├── NavRail.tsx     ← Client Component (view switch, theme toggle)
│   └── TopBar.tsx      ← Client Component (live indicator)
│
└── page.tsx            ← Root page, reads ?view= search param
    ├── OverviewView    ← Server Component
    ├── InventoryView   ← Server Component
    ├── BreakingView    ← Server Component
    ├── PolicyView      ← Server Component
    ├── GraphView       ← Server Component (with Client SVG interactions)
    └── NotificationsView ← Client Component (polling)
```

---

## Data Flow: Request → View

```
Browser Request (/api/v1/dashboard/summary)
    │
    ▼
Next.js Server Component
    │
    ▼
fetch(NEXT_PUBLIC_KEYSTONE_API_URL/dashboard/summary)
    │
    ▼
Keystone Backend (Spring Boot)
    │
    ├──→ Query Database
    ├──→ Compute Health Score
    ├──→ Aggregate Metrics
    │
    ▼
JSON Response (snake_case)
    │
    ▼
Frontend Transformation Layer
    snake_case → camelCase
    │
    ▼
View Component renders HTML
    │
    ▼
Streamed to browser via Suspense
```

---

## Security Boundaries

| Boundary | Enforcement | Context |
|----------|-------------|---------|
| External → API Gateway | Authentication (Bearer token) | All endpoints |
| API Gateway → Policy Mutations | RBAC (COMPLIANCE_MANAGER role required) | Policy Engine |
| API Gateway → Audit Log | RBAC (COMPLIANCE_MANAGER role required) | Dashboard |
| CLI → Ingestion | Webhook signature verification (GitHub) | Contract Ingestion |
| Frontend → Backend | Token passthrough, CORS | All endpoints |

---

## Deployment Architecture

```
┌────────────────────────────────────────────────────┐
│                   User Browser                      │
│  (Next.js App, served via Node/Bun production)     │
└────────────────────────────────────────────────────┘
                    │
                    ▼
┌────────────────────────────────────────────────────┐
│           Keystone Backend (Spring Boot)            │
│  REST API /api/v1/*                                 │
│  - In-process event bus                             │
│  - Background task scheduling (SyncScheduler)       │
└────────────────────────────────────────────────────┘
                    │
                    ▼
┌────────────────────────────────────────────────────┐
│              PostgreSQL Database                     │
│  Tables: open_api_spec, spec_version,               │
│          breaking_change_report, policy,            │
│          policy_set, evaluation_result,             │
│          service, api_dependency, notification,     │
│          health_score, audit_entry,                 │
│          idempotency_key                            │
└────────────────────────────────────────────────────┘
```

---

## Key Integration Points

| Integration | Protocol | Direction | Context |
|-------------|----------|-----------|---------|
| Frontend ↔ Backend | REST (JSON) | Bidirectional | All contexts |
| GitHub Webhook | HTTP POST (JSON) | GitHub → Ingestion | Contract Ingestion |
| Policy Source Sync | Git Clone | Backend → GitHub | Policy Engine |
| Slack Notification | HTTPS POST | Backend → Slack | Notification Engine |
| Email Notification | SMTP / API | Backend → Email | Notification Engine |
| Webhook Notification | HTTPS POST | Backend → Webhook | Notification Engine |
| CLI Upload | REST (JSON) | CLI → Backend | Contract Ingestion |

---

## Canonical Reference Template

Implementation files should reference this overview when describing system-level behavior:

```typescript
/**
 * Canonical Reference: .pi/architecture/diagrams/system-overview.md#[section]
 * Implements: [component at bounded context]
 */
```

---

*Last updated: 2026-06-13*
*Architecture version: 1.0.0*
