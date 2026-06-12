# CI Pipeline — End-to-End Sequence

> Shows the full flow from push to PR check-run across the local CLI and server-side processing.

```mermaid
sequenceDiagram
    participant Dev as Developer
    participant GH as GitHub
    participant CI as CI Runner
    participant CLI as Keystone CLI
    participant KS as Keystone Server
    participant CHECKS as GitHub Checks API

    Dev->>GH: Push commit with OpenAPI spec
    GH->>CI: Trigger pipeline (webhook)

    rect rgb(200, 230, 255)
        Note over CI,CLI: Local Analysis Phase (<50ms)
        CI->>CLI: keystone analyze --spec=openapi.yaml
        CLI->>CLI: Parse spec (SpecParser)
        CLI->>CLI: Retrieve cached version (LocalCache)
        CLI->>CLI: Diff & classify changes (LocalDiffEngine)
        CLI-->>CI: Exit code 0/1/2 + JSON summary
        Note right of CLI: ~42ms elapsed
    end

    CI->>GH: Continue pipeline (non-blocking)

    rect rgb(230, 255, 230)
        Note over CLI,KS: Async Audit Upload Phase (<1s)
        CLI-->>+KS: POST /api/v1/audit/upload
        Note right of CLI: Fire-and-forget background thread
        KS->>KS: Deduplication check
        KS->>KS: Validate & persist spec
        KS->>KS: Emit SpecIngested
        KS-->>-CLI: 202 Accepted
    end

    rect rgb(255, 235, 200)
        Note over KS: Server-Side Processing Phase
        KS->>KS: Breaking Change Analysis
        KS->>KS: Policy Evaluation
        KS->>KS: Compliance Verdict
        KS->>+CHECKS: POST /repos/{owner}/{repo}/check-runs
        Note right of KS: conclusion: success/failure/neutral
        CHECKS-->>-KS: 201 Created
    end

    rect rgb(255, 230, 230)
        Note over GH,Dev: Developer Feedback Phase
        GH->>GH: Check-run appears on PR
        Dev->>GH: View Keystone check-run details
        Dev->>KS: Click link to Dashboard
        KS-->>Dev: Full report with change details
    end
```

## Phase Timings

| Phase | Target Latency | Blocking? | Failure Impact |
|-------|---------------|-----------|---------------|
| **Local Analysis** | <50ms | Yes — CI blocks on CLI exit | CI step fails; pipeline may block |
| **Async Upload** | <1s | No — background thread | Upload fails → missing audit record; CI unaffected |
| **Server Processing** | <2s | No — async | Delayed check-run on PR; no CI impact |
| **Check-Run Visible** | <3s (total from push) | No — async | Developer must wait for check-run |

## Degraded Modes

| Scenario | Behavior |
|----------|----------|
| **Air-gapped (no network)** | CLI runs in local-only mode; no upload attempted; verdict based on cached spec only |
| **Server unavailable** | CLI exits with verdict; upload queued locally (retry on next invocation) |
| **No cached version** | CLI diffs against empty baseline → all changes classified as additive (safe default) |
| **Spec exceeds 1MB** | CLI falls back to streaming diff; latency may exceed 50ms; warning emitted |
