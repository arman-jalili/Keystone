# Runbook: contract-ingestion

> **Module:** contract-ingestion
> **Version:** v0.1.0
> **Last Updated:** 2026-06-12

## Overview

The contract-ingestion module receives, validates, deduplicates, and stores OpenAPI 3.x specifications. It exposes HTTP endpoints for CLI audit uploads and GitHub/GitLab webhooks, and publishes domain events for downstream consumers (Breaking Change Analysis, Dependency Graph, Dashboard).

## Dependencies

| Dependency | Type | Required | Notes |
|-----------|------|----------|-------|
| PostgreSQL 15+ | Database | Yes | Stores specs, versions, idempotency keys |
| Spring ApplicationEventPublisher | Internal | Yes | In-process event bus (ADR-003) |
| swagger-parser 2.x | Library | Yes | OpenAPI 3.x validation |

## Startup Sequence

1. **Database migration** — Flyway/Liquibase runs on startup (`spring.jpa.hibernate.ddl-auto: validate`)
2. **Connection pool** — HikariCP initializes with 10 max connections
3. **Health check** — `/actuator/health` reports UP when DB and context are ready
4. **Event listeners** — Breaking Change Analysis subscribes to `SpecIngestedEvent`

### Startup Verification

```bash
# Check health endpoint
curl -s http://localhost:8080/actuator/health | jq .status
# Expected: "UP"

# Check ingestion endpoint
curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/v1/ingestion/audit
# Expected: 200 (or 401 if auth is configured)
```

## Graceful Shutdown

```bash
# Send SIGTERM (Spring Boot handles this automatically)
kill -TERM <pid>

# Or use actuator (if enabled)
curl -X POST http://localhost:8080/actuator/shutdown
```

Spring Boot drains the HikariCP pool and waits for in-flight requests to complete before shutting down (default 30s timeout).

## Configuration Reference

| Property | Default | Description |
|----------|---------|-------------|
| `spring.datasource.url` | `jdbc:postgresql://localhost:5432/keystone` | JDBC URL |
| `spring.datasource.username` | `keystone` | DB username |
| `spring.datasource.password` | `${KEYSTONE_DB_PASSWORD}` | DB password (env var) |
| `spring.jpa.hibernate.ddl-auto` | `validate` | Schema management |
| `server.port` | `8080` | HTTP server port |
| `spring.datasource.hikari.maximum-pool-size` | `10` | Max DB connections |
| `spring.datasource.hikari.minimum-idle` | `2` | Min idle connections |

## Common Failure Modes

### 1. Spec Validation Failure

**Symptoms:** HTTP 422 response, `SpecParseFailedEvent` published
**Impact:** Single spec rejected, other specs unaffected
**Recovery:** Client fixes OpenAPI spec and re-uploads
**Monitoring:** Alert on `ingestion.validation.errors > 0` per minute

### 2. Duplicate Spec

**Symptoms:** HTTP 200 with `duplicate: true`, existing event ID returned
**Impact:** None (idempotent by design — ADR-007)
**Recovery:** No action needed
**Monitoring:** Track `ingestion.dedup.hits` metric

### 3. Database Connection Failure

**Symptoms:** HTTP 503 on ingestion, health check shows DOWN
**Impact:** All ingestion fails
**Recovery:**
1. Check PostgreSQL is running: `systemctl status postgresql`
2. Check connection pool: `SELECT count(*) FROM pg_stat_activity`
3. Restart application if pool is exhausted
**Monitoring:** PagerDuty alert on health check DOWN > 30s

### 4. High Throughput / Connection Exhaustion

**Symptoms:** Slow responses, connection timeouts
**Impact:** Degraded ingestion throughput
**Recovery:**
1. Increase `maximum-pool-size` (monitor DB first)
2. Scale horizontally (add application instances)
3. Rate-limit at the ingress (Kong/NGINX)
**Monitoring:** Alert on `hikaricp.connections.timeout > 0`

## Key Metrics

| Metric | Type | Description |
|--------|------|-------------|
| `ingestion.requests.total` | Counter | Total ingestion requests |
| `ingestion.requests.duration` | Timer | Request latency |
| `ingestion.dedup.hits` | Counter | Duplicate requests |
| `ingestion.dedup.misses` | Counter | New requests |
| `ingestion.validation.errors` | Counter | Validation failures |
| `ingestion.event.publish` | Timer | Event publication latency |

## Health Checks

| Endpoint | Purpose |
|----------|---------|
| `/actuator/health` | Overall health (includes DB) |
| `/actuator/health/readiness` | Readiness probe |
| `/actuator/health/liveness` | Liveness probe |
| `/actuator/info` | Build and version info |
| `/actuator/metrics` | Micrometer metrics |

## Troubleshooting

### Ingestion returns 401
- Verify `Authorization: Bearer <token>` header is present
- Check token has `SCOPE_audit:write` scope

### Webhook returns 401
- Verify `X-Hub-Signature-256` header matches the webhook secret
- Re-configure webhook in GitHub/GitLab settings

### Spec content rejected
- Validate spec locally first: `npx swagger-cli validate spec.yaml`
- Check the error details in the 422 response body
- Verify OpenAPI version is 3.x
