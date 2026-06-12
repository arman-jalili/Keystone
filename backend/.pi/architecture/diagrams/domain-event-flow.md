# Domain Event Flow

> Shows how domain events flow between bounded contexts, including event payloads and consumer actions.

```mermaid
graph LR
    subgraph "CLI Orchestrator"
        LAS[LocalAnalysisStarted]
        LAC[LocalAnalysisCompleted]
        ALU[AuditLogUploaded]
    end

    subgraph "Contract Ingestion"
        SI[SpecIngested]
        SPF[SpecParseFailed]
    end

    subgraph "Breaking Change Analysis"
        BCR[BreakingChangeReported]
    end

    subgraph "Policy Engine"
        PE[PolicyEvaluated]
        CVR[ComplianceVerdictReached]
        EG[ExemptionGranted]
    end

    subgraph "Notification Engine"
        CSU[CiStatusUpdated]
        SN[StakeholderNotified]
    end

    subgraph "Dependency Graph"
        DA[DependencyAdded]
        DIC[DownstreamImpactComputed]
    end

    LAS --> LAC
    LAC --> ALU

| ALU -. "HTTP upload (keystone-cli)" .-> SI
    ALU -. "HTTP upload (keystone-cli)" .-> SPF

    SI --> BCR
    SI --> DA

    BCR --> PE
    BCR --> DIC

    PE --> CVR

    CVR --> CSU
    CVR --> SN
    EG --> CSU
    EG --> SN

    classDef cli fill:#e1f5fe,stroke:#0288d1
    classDef ing fill:#f3e5f5,stroke:#7b1fa2
    classDef bca fill:#e8f5e9,stroke:#388e3c
    classDef pol fill:#fff8e1,stroke:#f57f17
    classDef not fill:#fce4ec,stroke:#c62828
    classDef dep fill:#f5f5f5,stroke:#616161
    class LAS,LAC,ALU cli
    class SI,SPF ing
    class BCR bca
    class PE,CVR,EG pol
    class CSU,SN not
    class DA,DIC dep
```

## Event Payload Schemas

### SpecIngested
```json
{
  "eventType": "SpecIngested",
  "version": 1,
  "id": "evt_abc123",
  "timestamp": "2026-06-12T10:00:00Z",
  "source": "ContractIngestion",
  "data": {
    "specId": "spec_456",
    "version": "a1b2c3d",
    "repository": "org/repo",
    "commitSha": "a1b2c3d4e5f6",
    "specPath": "openapi/checkout.yaml",
    "checksum": "sha256:abc123def456",
    "parsedEndpointCount": 42
  }
}
```

### BreakingChangeReported
```json
{
  "eventType": "BreakingChangeReported",
  "version": 1,
  "id": "evt_def456",
  "timestamp": "2026-06-12T10:00:01Z",
  "source": "BreakingChangeAnalysis",
  "data": {
    "reportId": "rpt_789",
    "baseSpecVersion": "abc1111",
    "targetSpecVersion": "a1b2c3d",
    "verdict": "breaking",
    "changeCount": 3,
    "changes": [
      {
        "id": "chg_001",
        "severity": "BREAKING",
        "description": "Response property 'email' removed from GET /users/{id}",
        "path": "/users/{id}",
        "method": "GET"
      }
    ]
  }
}
```

### ComplianceVerdictReached
```json
{
  "eventType": "ComplianceVerdictReached",
  "version": 1,
  "id": "evt_ghi789",
  "timestamp": "2026-06-12T10:00:02Z",
  "source": "PolicyEngine",
  "data": {
    "reportId": "rpt_789",
    "verdict": "FAIL",
    "policiesEvaluated": 3,
    "violations": [
      {
        "policyId": "pol_123",
        "policyName": "no-breaking-changes-prod",
        "ruleName": "block-breaking-changes",
        "severity": "block",
        "description": "Breaking change detected without exemption"
      }
    ]
  }
}
```

### CiStatusUpdated
```json
{
  "eventType": "CiStatusUpdated",
  "version": 1,
  "id": "evt_jkl012",
  "timestamp": "2026-06-12T10:00:03Z",
  "source": "NotificationEngine",
  "data": {
    "repository": "org/repo",
    "commitSha": "a1b2c3d4e5f6",
    "state": "failure",
    "description": "Breaking change detected: response property removed",
    "targetUrl": "https://keystone.acme.corp/reports/rpt_789",
    "context": "keystone/governance"
  }
}
```

---

## Event Ordering Guarantees

| Guarantee | Implementation |
|-----------|---------------|
| **At-least-once delivery** | Consumer acknowledges after processing; retry on failure |
| **Ordered per aggregate** | Events for the same spec/commit arrive in order (partition key = specId) |
| **No total order** | Events across different specs are unordered (acceptable) |
| **Idempotent consumers** | Consumers deduplicate by event ID |

---

*Last updated: 2026-06-12*
