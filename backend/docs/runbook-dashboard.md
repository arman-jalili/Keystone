# Dashboard Module Runbook

> **Module:** dashboard (com.keystone.dashboard)
> **Last updated:** 2026-06-13
> **Canonical Reference:** .pi/architecture/modules/dashboard.md

## Overview

The Dashboard module provides visualization of governance health, policy compliance, audit trails, and system metrics. It reads from all other bounded contexts via Spring `@Service` beans.

## Startup Sequence

1. **Database check** — The module depends on PostgreSQL being available. Health indicator checks connectivity.
2. **Dependency check** — Verifies that upstream modules (ingestion, analysis, policy) are available. If any are down, the dashboard serves degraded data.
3. **Cache warm-up** — On first request, the dashboard computes health scores and caches them. Subsequent requests use cached data.
4. **Ready** — The `/actuator/health` endpoint reports UP when all dependencies are available.

### Prerequisites

| Dependency | Required | Notes |
|------------|----------|-------|
| PostgreSQL 16 | Yes | Dashboard queries read replicas |
| Contract Ingestion | Yes | Spec metadata via SpecRepository |
| Policy Engine | Yes | Compliance data via PolicyRepository |
| Breaking Change Analysis | No | Degraded mode if unavailable |
| RabbitMQ | No | Only needed for event publishing |

### Startup Order

```
1. PostgreSQL
2. Contract Ingestion module
3. Policy Engine module
4. Breaking Change Analysis module
5. Dashboard module
```

## Graceful Shutdown

The Dashboard module supports graceful shutdown via Spring Boot's actuator:

```bash
# Initiate graceful shutdown
curl -X POST http://localhost:8080/actuator/shutdown

# Or send SIGTERM to the Java process
kill -15 <PID>
```

During shutdown:
1. In-flight requests complete within the configured timeout (default: 30s)
2. New requests are rejected with 503 Service Unavailable
3. Event publishers flush pending events
4. Connection pool drains

## Configuration Reference

| Property | Default | Description |
|----------|---------|-------------|
| `spring.datasource.url` | `jdbc:postgresql://localhost:5432/keystone` | PostgreSQL connection URL |
| `spring.datasource.username` | `keystone` | Database username |
| `spring.datasource.password` | `${KEYSTONE_DB_PASSWORD}` | Database password |
| `server.port` | `8080` | HTTP server port |
| `policy.git.repository` | — | Policy Git repository URL |
| `policy.git.deploy-key` | — | Path to deploy key for Git access |

## Common Failure Modes

### Dashboard shows "No Data"

**Symptoms:** Dashboard loads but shows empty charts/metrics.

**Causes:**
- No specs have been ingested yet
- Policy engine has not completed an evaluation cycle
- Cached data expired but refresh failed

**Resolution:**
1. Check ingestion status: `GET /api/v1/ingestion/health`
2. Check policy evaluation: `GET /api/v1/policies/health`
3. Trigger manual health score computation

### Dashboard returns 503

**Symptoms:** Dashboard endpoints return HTTP 503.

**Causes:**
- PostgreSQL connection pool exhausted
- Upstream module unavailable

**Resolution:**
1. Check database connectivity: `psql -h localhost -U keystone -c "SELECT 1"`
2. Check connection pool metrics: `/actuator/metrics/hikaricp.connections.active`
3. Restart the dashboard service if pool is stuck

### Health scores are stale

**Symptoms:** Health score hasn't changed in hours/days.

**Causes:**
- Health score scheduler not running
- Scheduled task failed

**Resolution:**
1. Check scheduler logs for errors
2. Trigger manual recalculation via the HealthScoreService API
3. Verify the scheduled task configuration

## Health Checks

| Endpoint | Purpose |
|----------|---------|
| `/actuator/health` | Overall application health |
| `/actuator/health/readiness` | Readiness probe |
| `/actuator/health/liveness` | Liveness probe |
| `/actuator/metrics` | Application metrics |
| `/actuator/info` | Application info |

## Monitoring

### Key Metrics

| Metric | Type | Description |
|--------|------|-------------|
| `dashboard.health-score.time` | Timer | Time to compute health score |
| `dashboard.page.time` | Timer | Dashboard page load time |
| `dashboard.audit-log.time` | Timer | Audit log query time |
| `http.server.requests` | Timer | HTTP request latency |

### Alerts

| Alert | Threshold | Severity |
|-------|-----------|----------|
| Dashboard page load > 500ms | p95 > 500ms | Warning |
| Health score stale > 1h | Last computed > 1h ago | Critical |
| Audit log query > 2s | p95 > 2s | Warning |
