# Runbook: Policy Engine

> **Status:** Production Ready
> **Last Updated:** 2026-06-12
> **Module:** policy-engine
> **Package:** `com.keystone.policy`

## Overview

The Policy Engine evaluates OpenAPI specifications against governance policies.
It synchronizes policies from a Git repository, evaluates specs against active
policies, and produces compliance verdicts.

## Startup Sequence

1. **ApplicationContext Initialization** — Spring Boot loads all `@Service`,
   `@Component`, and `@Repository` beans in `com.keystone.policy`

2. **Database Schema** — Hibernate validates or creates tables:
   - `policies` — policy cache
   - `policy_sets` — policy group definitions
   - `policy_evaluation_results` — evaluation history

3. **Scheduler Activation** — `SyncScheduler` starts with configurable polling
   (default 60s)

4. **Listener Registration** — `BreakingChangeReportListener` subscribes to
   `BreakingChangeReportedEvent` from the analysis module

### Dependencies

| Dependency | Type | Required | Failure Impact |
|-----------|------|----------|----------------|
| PostgreSQL | Database | Yes | Service unavailable |
| Git Repository | External | Yes | Policy sync fails (stale cache used) |
| Breaking Change Analysis | Module | No | Policy evaluation not auto-triggered |

## Configuration Reference

| Property | Default | Description |
|----------|---------|-------------|
| `policy.sync.interval.ms` | `60000` | Poll interval for Git policy sync (ms) |
| `policy.git.repository` | — | Git repository URL for policy source |
| `policy.git.branch` | `main` | Git branch to sync from |
| `policy.git.policy-path` | `.keystone/policies` | Directory containing `.policy` files |
| `policy.git.source-id` | `default` | Identifier for the policy source |
| `policy.git.local-path` | `java.io.tmpdir/keystone-policies` | Local clone path |

## Health Checks

The policy engine contributes to the Spring Boot health endpoint at `/actuator/health`:

- **Database connectivity** — automatic via HikariCP
- **Git repository** — checked during sync operations
- **Policy count** — can be monitored via `/actuator/metrics`

## Common Failure Modes

### Sync Failure (Git unavailable)
- **Symptom:** `PolicySyncException` logged, sync returns partial/failure
- **Impact:** Policy cache becomes stale, but evaluations continue with cached policies
- **Recovery:** Automatic retry on next sync cycle (60s). Fix Git repo access.
- **Monitoring:** `policy.sync.time` timer metric, error log

### Parse Failure (invalid policy DSL)
- **Symptom:** `PolicyParseException` logged with line/column details
- **Impact:** Invalid policy is skipped, valid policies still sync
- **Recovery:** Fix the `.policy` file and push to Git. Next sync picks it up.
- **Monitoring:** Error log with policy file name

### Evaluation Failure
- **Symptom:** `PolicyEvaluationException` logged
- **Impact:** Individual policy evaluation is skipped, remaining policies still evaluated
- **Recovery:** Check policy DSL expression syntax, review stack trace
- **Monitoring:** `policy.evaluation.time` timer metric

### Database Connection Loss
- **Symptom:** `DataSource` health check fails, `/actuator/health` shows DOWN
- **Impact:** All policy operations fail
- **Recovery:** Restore database connection, verify connectivity
- **Monitoring:** Spring Boot health endpoint, HikariCP metrics

## Graceful Shutdown

The service handles SIGTERM gracefully:
1. `@PreDestroy` hooks cancel scheduled syncs
2. In-flight evaluations complete (configurable timeout)
3. Active database transactions roll back if not complete

To trigger: `kill -15 <pid>` or `curl -X POST /actuator/shutdown`

## Metrics

| Metric | Type | Tags | Description |
|--------|------|------|-------------|
| `policy.sync.time` | Timer | sourceId, success | Duration of policy sync operations |
| `policy.sync.count` | Counter | sourceId, result | Count of sync operations (added/updated/removed) |
| `policy.evaluation.time` | Timer | verdict | Duration of policy evaluation |
| `policy.evaluation.count` | Counter | verdict | Count of policy evaluations |
| `policy.active.count` | Gauge | — | Number of active policies in cache |
| `policy.violation.count` | Counter | severity | Count of violations by severity |

## Logging

All policy operations use structured logging with correlation IDs:

```
2026-06-12 20:00:00.000 [thread] INFO  c.k.p.sync.PolicySyncServiceImpl [traceId,spanId]
  - Starting policy sync from source: default
2026-06-12 20:00:01.000 [thread] INFO  c.k.p.sync.PolicySyncServiceImpl [traceId,spanId]
  - Policy sync completed for 'default': 3 added, 0 updated, 1 removed
```

Log levels:
- **INFO:** Sync start/complete, evaluation start/complete
- **WARN:** Parse failures (per-policy), configuration issues
- **ERROR:** Sync failures, evaluation failures, connectivity issues

## Troubleshooting

### Policies not being evaluated
1. Check `POST /api/v1/policies/evaluate` returns results
2. Verify active policies exist: `GET /api/v1/policies?status=ACTIVE`
3. Check database: `SELECT COUNT(*) FROM policies WHERE status = 'ACTIVE'`
4. Verify SyncScheduler is running: check logs for "scheduled policy sync"

### Policy sync not working
1. Verify Git repository URL is configured
2. Check Git credentials are valid
3. Verify `.policy` files exist in the configured directory
4. Check logs for `PolicySyncException`
5. Trigger manual sync: `POST /api/v1/policies/sync`
