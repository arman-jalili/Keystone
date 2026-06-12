# Breaking Change Analysis — Runbook

> **Module:** breaking-change-analysis (`com.keystone.analysis`)
> **Last updated:** 2026-06-12

## Overview

The Breaking Change Analysis module diffs two versions of an OpenAPI specification,
classifies each change by severity, and produces a `BreakingChangeReport`. It is
triggered automatically via `SpecIngestedEvent` from the Contract Ingestion module.

## Startup Sequence

### Dependencies (start order)

1. **PostgreSQL** — primary data store
2. **Contract Ingestion** — provides `SpecVersion` data and triggers analysis
3. **Breaking Change Analysis** — starts after Contract Ingestion is ready

### Health Check

```
GET /actuator/health
```

Expected response: `{"status": "UP"}`

The breaking-change-analysis module contributes a component health indicator that
checks:
- Database connectivity (via `ChangeReportRepository`)
- Event publisher availability

### Startup Verification

```bash
# Check the module is registered and healthy
curl -s http://localhost:8080/actuator/health | jq .

# Trigger a test analysis
curl -s -X POST http://localhost:8080/api/v1/breaking/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "repository": "org/repo",
    "specPath": "openapi.yaml",
    "commitSha": "0000000000000000000000000000000000000000"
  }' | jq .
```

## Graceful Shutdown

### Procedure

1. **Stop accepting new analysis requests** — the load balancer should drain
   connections from this instance.
2. **Wait for in-flight analyses to complete** — the analysis pipeline has a
   configurable timeout (default: 30 seconds).
3. **Shutdown Spring context** — `POST /actuator/shutdown` (if enabled) or
   send SIGTERM to the JVM process.

### Timeout Configuration

```yaml
keystone:
  analysis:
    timeout: 30s        # Max time for a single analysis
    shutdown-timeout: 60s  # Max time to wait for in-flight analyses
```

## Common Failure Modes

### 1. No Base Version Available

**Symptom:** `NoBaseVersionException` logged at WARN level.
**Impact:** Analysis produces `Verdict.INCONCLUSIVE`.
**Resolution:**
- This is expected for first-time spec ingestion — no action needed.
- If persistent for an existing spec, verify that the spec has been
  previously ingested and committed to the database.

### 2. Detector Failure

**Symptom:** `WARN` log: "Detector 'X' failed, skipping"
**Impact:** One detector is skipped; other detectors continue running.
**Resolution:**
- Check if the detector has a bug or if the spec format is unexpected.
- Review logs for the specific error message.
- File a bug if the detector should handle the input format.

### 3. Database Connection Lost

**Symptom:** `DiffAnalysisException` with SQL error in logs.
**Impact:** All analyses fail until database is restored.
**Resolution:**
- Verify PostgreSQL is running: `pg_isready`
- Check connection pool in logs for exhaustion.
- Restart the database if needed; the module auto-recovers via
  HikariCP connection pool retry.

### 4. High Memory Usage

**Symptom:** OutOfMemoryError or GC pressure during large spec diffs.
**Impact:** Analysis may timeout or fail with `DiffAnalysisException`.
**Resolution:**
- Large specs (>10MB) should be handled with streaming parsers.
- Current limit: 500 endpoints per spec. Monitor `analysis.diff.time`
  metric and increase heap if needed.
- Consider enabling the spec size limit in configuration.

## Configuration Reference

```yaml
keystone:
  analysis:
    # Analysis pipeline configuration
    timeout: 30s
    max-endpoints: 500
    max-spec-size: 10MB

    # Report retention
    retention-days: 90

    # Base version resolution
    base-version:
      default-branch: main
      fallback-to-main: true

    # Detectors (enable/disable individual detectors)
    detectors:
      path-removal:
        enabled: true
      required-field-added:
        enabled: true
      field-removal:
        enabled: true
      field-type-changed:
        enabled: true
      optional-field-added:
        enabled: true
      deprecated-field:
        enabled: true

  # Metrics export
  metrics:
    export:
      prometheus:
        enabled: true

  # Distributed tracing
  tracing:
    sampling-rate: 0.1
```

## Monitoring

### Key Metrics (Micrometer)

| Metric | Type | Description |
|--------|------|-------------|
| `analysis.diff.time` | Timer | Time to complete a diff analysis (p99 target: <100ms) |
| `analysis.report.count` | Counter | Total reports generated |
| `analysis.report.breaking` | Counter | Reports with BREAKING verdict |
| `analysis.report.error` | Counter | Reports that failed during analysis |
| `analysis.detector.failure` | Counter | Individual detector failures |

### Key Logs

| Event | Log Level | Message |
|-------|-----------|---------|
| Analysis started | INFO | "Starting diff analysis for {repo}/{spec} (target: {id})" |
| Analysis completed | INFO | "Diff analysis completed for {repo}/{spec}: verdict={v}, changes={n}" |
| Analysis failed | ERROR | "Diff analysis failed: {reason}" |
| Detector skipped | WARN | "Detector '{name}' failed, skipping" |
| No base version | WARN | "No base version for {repo}/{spec}: {reason}" |
| Event published | INFO | "Publishing BreakingChangeReportedEvent: reportId={id}" |

## Recovery Procedures

### After Database Outage

1. Verify PostgreSQL is healthy.
2. The module will automatically reconnect via HikariCP pool.
3. Any analyses started during the outage will have failed with
   `DiffAnalysisException`. The CLI/webhook should retry.
4. Verify health endpoint returns UP.

### After Module Crash

1. Check logs in `var/log/keystone/analysis.log` for the crash reason.
2. Restart the module: `systemctl restart keystone-analysis` (or k8s pod restart).
3. Verify all 4 database migrations have been applied.
4. Verify health check passes.
5. Any in-flight analyses are lost; callers must retry.

## Change Management

### Adding a New Detector

1. Implement `ChangeDetector` interface.
2. Annotate with `@Component` for auto-discovery.
3. Add configuration in `keystone.analysis.detectors` section.
4. Add the detector to the runbook's configuration reference.
5. Verify via integration test.

### Deploying a Change

1. Create feature branch with changes.
2. Ensure all validators pass (ci, tests, security, architecture, canonical).
3. Merge via PR into main.
4. Monitor `analysis.diff.time` p99 metric after deploy.

---

*Generated with Guardian compliance workflow*
