# Data Flow & Sequence Diagrams

## Overview

This document contains data flow diagrams and key sequence flows for the Keystone system. Each diagram illustrates how data moves between bounded contexts, actors, and external systems.

---

## 1. Spec Ingestion Flow

Flow when an API Developer submits a spec for audit, triggering the full governance pipeline.

```mermaid
sequenceDiagram
    participant Dev as API Developer
    participant CLI as Keystone CLI
    participant Ingestion as Contract Ingestion
    participant Analysis as Breaking Change Analysis
    participant Policy as Policy Engine
    participant Graph as Dependency Graph
    participant Notif as Notification Engine

    Dev->>CLI: keystone audit spec.yaml
    CLI->>Ingestion: POST /api/v1/ingestion/audit
    
    alt Duplicate spec
        Ingestion-->>CLI: 200 OK (duplicate=true)
        CLI-->>Dev: Spec already ingested
    else New spec
        Ingestion->>Ingestion: Parse & validate OpenAPI
        Ingestion->>Ingestion: Store spec + version
        Ingestion-->>CLI: 201 Created (specId, checksum)
        CLI-->>Dev: Ingestion complete
        
        Note over Ingestion,Policy: Domain Event Chain
        Ingestion->>Analysis: SpecIngestedEvent(specId, commitSha)
        Ingestion->>Dashboard: SpecIngestedEvent(metadata)
        
        Analysis->>Analysis: Resolve base version
        Analysis->>Analysis: Run change detectors
        Analysis-->>Analysis: Produce BreakingChangeReport
        
        alt Breaking changes found
            Analysis->>Policy: BreakingChangeReportedEvent(reportId)
            Analysis->>Graph: BreakingChangeReportedEvent(specPath)
            Analysis->>Notif: BreakingChangeReportedEvent(summary)
            
            Graph->>Graph: BFS downstream traversal
            Graph-->>Graph: ImpactAnalysisResult
            
            Policy->>Policy: Evaluate spec against policies
            Policy-->>Policy: PolicyEvaluationResult
            
            alt Policy violated
                Policy->>Notif: PolicyEvaluatedEvent(violations)
            end
            
            Notif->>Notif: Dispatch via channels
        end
    end
```

---

## 2. Dashboard Data Fetching Flow

Flow when a user navigates to the dashboard and views data.

```mermaid
sequenceDiagram
    participant User as User
    participant Browser as Browser
    participant NextJS as Next.js (Server)
    participant Backend as Keystone API
    participant DB as Database

    User->>Browser: Navigate to /?view=overview
    Browser->>NextJS: GET /?view=overview
    
    par Fetch GovernanceHealth
        NextJS->>Backend: GET /api/v1/dashboard/summary
        Backend->>DB: Query aggregated metrics
        DB-->>Backend: Health score data
        Backend-->>NextJS: DashboardSummaryResponse
    and Fetch Health Score Detail
        NextJS->>Backend: GET /api/v1/dashboard/health-score?period=LAST_30_DAYS
        Backend->>DB: Query compliance history
        DB-->>Backend: GovernanceHealthScore
        Backend-->>NextJS: GovernanceHealthScore
    end
    
    Note over NextJS: Server Component renders HTML
    
    NextJS-->>Browser: Streamed HTML (Suspense boundaries)
    
    User->>Browser: Switch to ?view=breaking
    Browser->>NextJS: Client navigation (no full reload)
    NextJS->>Backend: GET /api/v1/breaking/reports/latest
    Backend-->>NextJS: AnalysisResponse[]
    NextJS-->>Browser: Updated view HTML
```

---

## 3. Policy Management Flow

Flow when a Compliance Manager creates or modifies a policy.

```mermaid
sequenceDiagram
    participant Manager as Compliance Manager
    participant UI as Dashboard UI
    participant PolicySvc as Policy Management Service
    participant Git as Git Repository
    participant Backend as Keystone API

    Manager->>UI: Create new policy
    UI->>Backend: POST /api/v1/policies
    Backend->>PolicySvc: createPolicy(request)
    PolicySvc->>PolicySvc: Parse DSL expression
    PolicySvc->>PolicySvc: Validate syntax
    
    alt Valid DSL
        PolicySvc->>PolicySvc: Create Policy domain object
        PolicySvc->>Git: Commit policy YAML to repo
        Git-->>PolicySvc: Commit SHA
        PolicySvc-->>Backend: PolicySummaryResponse
        Backend-->>UI: 201 Created (policy summary)
        UI-->>Manager: Policy created
    else Invalid DSL
        PolicySvc-->>Backend: PolicyParseException
        Backend-->>UI: 422 Unprocessable Entity
        UI-->>Manager: Show parse error
    end
```

---

## 4. Dependency Graph & Impact Analysis Flow

Flow when a breaking change triggers impact analysis across the service dependency graph.

```mermaid
sequenceDiagram
    participant Analysis as Breaking Change Analysis
    participant GraphSvc as Dependency Graph Service
    participant Impact as Impact Analyzer
    participant Policy as Policy Engine
    participant Notif as Notification Engine

    Analysis->>GraphSvc: BreakingChangeReportedEvent
    
    Note over GraphSvc: Spec changed: "openapi/payment.yaml"
    
    GraphSvc->>Impact: analyzeImpact(specPath, reportId)
    
    Note over Impact: BFS Traversal
    Impact->>Impact: Find producers of payment.yaml
    Impact->>Impact: Find direct consumers (depth 1)
    Impact->>Impact: Find transitive consumers (depth 2+)
    Impact->>Impact: Classify impact severity (DIRECT / TRANSITIVE)
    Impact-->>GraphSvc: ImpactAnalysisResult
    
    alt Downstream services affected
        GraphSvc->>Notif: DownstreamImpactComputedEvent
        GraphSvc->>Policy: DownstreamImpactComputedEvent
    end
    
    Note over GraphSvc: Result cached for dashboard queries
```

---

## 5. Notification Dispatch Flow

Flow when a governance event triggers multi-channel notification delivery.

```mermaid
sequenceDiagram
    participant Source as Event Source
    participant Dispatcher as Notification Dispatcher
    participant ChannelReg as Channel Registry
    participant SlackCh as Slack Channel
    participant EmailCh as Email Channel
    participant WebhookCh as Webhook Channel
    participant DB as Database

    Source->>Dispatcher: NotificationRequest(breaking-change-report, payload)
    
    Dispatcher->>ChannelReg: getActiveChannels()
    ChannelReg-->>Dispatcher: [Slack, Email, Webhook]
    
    par Dispatch to Slack
        Dispatcher->>SlackCh: send(payload)
        SlackCh->>SlackCh: Check circuit breaker
        alt Circuit closed
            SlackCh->>Slack: POST /api/slack/chat.postMessage
            alt Success
                Slack-->>SlackCh: 200 OK
                SlackCh-->>Dispatcher: DELIVERED
            else Failure
                SlackCh->>SlackCh: Retry (max 3)
                alt All retries failed
                    SlackCh->>SlackCh: Open circuit breaker
                    SlackCh-->>Dispatcher: FAILED
                end
            end
        else Circuit open
            SlackCh-->>Dispatcher: FAILED (circuit open)
        end
    and Dispatch to Email
        Dispatcher->>EmailCh: send(payload)
        EmailCh->>Email: SMTP send
        Email-->>EmailCh: Accepted
        EmailCh-->>Dispatcher: DELIVERED
    and Dispatch to Webhook
        Dispatcher->>WebhookCh: send(payload)
        WebhookCh->>WebhookTarget: POST callback URL
        WebhookTarget-->>WebhookCh: 200 OK
        WebhookCh-->>Dispatcher: DELIVERED
    end
    
    Dispatcher->>DB: Store Notification records
```

---

## 6. Component Data Dependencies

Shows which backend endpoints each frontend view depends on.

```mermaid
flowchart TD
    subgraph FE["Frontend Views"]
        OV[Overview]
        INV[API Inventory]
        BR[Breaking Changes]
        POL[Policy Compliance]
        DG[Dependency Graph]
        NT[Notifications]
    end
    
    subgraph BE["Backend Endpoints"]
        S1["GET /dashboard/summary"]
        S2["GET /dashboard/health-score"]
        S3["GET /dashboard/health/{type}/{id}"]
        S4["GET /dashboard/compliance-history/{specId}"]
        S5["GET /dashboard/audit-log"]
        S6["GET /dashboard/violation-trends"]
        S7["GET /dashboard/policies"]
        S8["GET /dashboard/policies/breakdown"]
        S9["GET /dashboard/policies/{id}"]
        
        I1["GET /ingestion/apis"]
        I2["GET /ingestion/apis/stale"]
        
        B1["GET /breaking/reports/{id}"]
        B2["GET /breaking/repositories/{repo}/latest"]
        
        G1["GET /graph/services"]
        G2["POST /graph/impact"]
        
        N1["GET /notifications/channels"]
        N2["GET /notifications/{id}"]
    end
    
    OV --> S1
    OV --> S2
    OV --> S3
    
    INV --> I1
    INV --> I2
    
    BR --> B1
    BR --> B2
    
    POL --> S7
    POL --> S8
    POL --> S9
    POL --> S4
    
    DG --> G1
    DG --> G2
    
    NT --> N1
    NT --> N2
```

---

*Date: 2026-06-13*
