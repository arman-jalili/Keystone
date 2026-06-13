# System Context Diagram (C4 Level 1)

## Context

Keystone is an **OpenAPI Specification Governance Server**. The system ingests OpenAPI specs from repositories, detects breaking changes, evaluates policies, tracks service dependencies, and provides a governance dashboard.

This diagram shows the high-level system boundary and external actors.

---

## C4 System Context

```mermaid
C4Context
    title System Context diagram for Keystone API Governance

    Person(dev, "API Developer", "Submits specs via CLI/CI, views analysis results and violations")
    Person(compliance, "Compliance Manager", "Manages policies, grants exemptions, reviews audit trail")
    Person(repo_owner, "Repository Owner", "Monitors governance health, reviews breaking changes")
    
    System_Boundary(keystone, "Keystone API Governance") {
        System(backend, "Keystone Server", "Spring Boot backend - spec ingestion, analysis, policy engine, dependency graph, notifications")
        System(frontend, "Keystone Dashboard", "Next.js frontend - governance health overview, API inventory, breaking changes, policy compliance, dependency graph, notifications")
    }
    
    System_Ext(github, "GitHub", "Source code repositories with OpenAPI specs and policy definitions")
    System_Ext(slack, "Slack", "Notification channel for governance alerts")
    System_Ext(email, "Email", "Notification channel for governance alerts")
    System_Ext(webhook, "Webhook", "Custom HTTP notification targets")
    System_Ext(cli, "Keystone CLI", "CLI tool for spec upload and audit")
    System_Ext(ci, "CI Pipeline", "Automated spec submission in CI/CD")
    
    Rel(dev, cli, "Uploads spec", "HTTPS")
    Rel(dev, frontend, "Views results", "HTTPS")
    Rel(compliance, frontend, "Manages policies", "HTTPS")
    Rel(repo_owner, frontend, "Monitors health", "HTTPS")
    Rel(ci, backend, "Submits spec", "HTTPS")
    Rel(github, backend, "Push webhooks, policy sync", "HTTPS")
    Rel(backend, slack, "Sends notifications", "HTTPS")
    Rel(backend, email, "Sends notifications", "SMTP/HTTPS")
    Rel(backend, webhook, "Sends notifications", "HTTPS")
    Rel(frontend, backend, "Fetches data", "HTTPS")
    
    UpdateLayoutConfig($c4ShapeInRow="3", $c4BoundaryInRow="1")
```

---

## Event Flow Between Bounded Contexts

```mermaid
graph TB
    subgraph Ingestion[Contract Ingestion]
        SI[SpecIngestedEvent]
        SP[SpecParseFailedEvent]
    end
    
    subgraph Analysis[Breaking Change Analysis]
        BR[BreakingChangeReportedEvent]
    end
    
    subgraph Policy[Policy Engine]
        PE[PolicyEvaluatedEvent]
        PS[PolicySyncedEvent]
        PSC[PolicySourceChangedEvent]
    end
    
    subgraph Graph[Dependency Graph]
        DI[DownstreamImpactComputedEvent]
        DA[DependencyAddedEvent]
    end
    
    subgraph Notifications[Notification Engine]
        NS[NotificationSentEvent]
        NF[NotificationDeliveryFailedEvent]
    end
    
    subgraph Dashboard[Dashboard]
        HS[HealthScoreRecalculatedEvent]
        PV[PolicyStatusChangedEvent]
        DV[DashboardViewAccessedEvent]
    end
    
    %% Event flow
    SI --> Analysis
    SI --> Dashboard
    BR --> Policy
    BR --> Notifications
    BR --> Dashboard
    PE --> Notifications
    PE --> Dashboard
    DI --> Notifications
    DI --> Dashboard
    DA --> Dashboard
    PS --> Dashboard
    PSC --> Policy
    
    style Ingestion fill:#e1f5fe,stroke:#0288d1
    style Analysis fill:#fff3e0,stroke:#f57c00
    style Policy fill:#e8f5e9,stroke:#388e3c
    style Graph fill:#f3e5f5,stroke:#7b1fa2
    style Notifications fill:#fce4ec,stroke:#c62828
    style Dashboard fill:#fff8e1,stroke:#f9a825
```

---

## System Context Data Flow

```mermaid
flowchart LR
    subgraph External["External Systems"]
        direction TB
        GH[("GitHub\nRepositories")]
        CLI[("Keystone CLI")]
        CI[("CI Pipeline")]
    end
    
    subgraph Backend["Keystone Backend (Spring Boot)"]
        direction TB
        A1[Contract\nIngestion]
        A2[Breaking Change\nAnalysis]
        A3[Policy\nEngine]
        A4[Dependency\nGraph]
    end
    
    subgraph Frontend["Keystone Dashboard (Next.js)"]
        direction TB
        F1[Overview\nView]
        F2[API Inventory\nView]
        F3[Breaking Changes\nView]
        F4[Policy Compliance\nView]
        F5[Dependency Graph\nView]
        F6[Notifications\nView]
    end
    
    subgraph Notification["Notification Targets"]
        direction TB
        N1[Slack]
        N2[Email]
        N3[Webhook]
    end
    
    GH -- "Push webhook" --> A1
    CLI -- "POST /audit" --> A1
    CI -- "POST /audit" --> A1
    A1 -- "SpecIngestedEvent" --> A2
    A1 -- "SpecIngestedEvent" --> A3
    A2 -- "BreakingChangeReportedEvent" --> A3
    A2 --> A4
    A3 -- "PolicyEvaluatedEvent" --> A4
    A3 --> N2
    A4 -- "DownstreamImpactComputedEvent" --> A4
    A4 --> A3
    
    A1 <--> F2
    A2 <--> F3
    A3 <--> F4
    A4 <--> F5
    A1 -.->|"aggregate"| F1
    A2 -.->|"aggregate"| F1
    A3 -.->|"aggregate"| F1
    A4 -.->|"aggregate"| F1
    
    A3 -.->|"events→"| Notification
    A2 -.->|"events→"| Notification
    
    F6 -.->|"polls"| Notification
```

---

## Layout Structure

```mermaid
flowchart LR
    subgraph Browser["Browser Viewport"]
        direction TB
        NAV["NavRail\n232px fixed"]
        CONTENT["Content Area\nscrollable"]
    end
    
    subgraph Content["Content"]
        TOP["TopBar\n56px"]
        VIEW["View Container\nflex-grow"]
    end
    
    CONTENT --> TOP
    CONTENT --> VIEW
    
    VIEW --> OV["Overview\nScoreRing + Stats + Tables"]
    VIEW --> INV["API Inventory\nDataTable + StaleTable"]
    VIEW --> BRK["Breaking Changes\nStatGrid + BreakageCards"]
    VIEW --> POL["Policy Compliance\nStatGrid + RuleCards"]
    VIEW --> GRAPH["Dependency Graph\nSVG + ImpactCascade"]
    VIEW --> NOTIF["Notifications\nStatGrid + Feed + Channels"]
```

---

*Generated from session: 25c57121-7a91-47ce-ae20-5c561181984d*
*Date: 2026-06-13*
