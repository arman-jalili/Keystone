# Dependency Graph — Runbook

> **Module:** dependency-graph (`com.keystone.graph`)
> **Last updated:** 2026-06-12

## Overview

The Dependency Graph module tracks which services consume/produce which API specs.
It maintains a directed graph of producer-consumer relationships in PostgreSQL and
computes impact analysis via BFS traversal when breaking changes are detected.

### Key Components

| Component | File | Purpose |
|-----------|------|---------|
| GraphService | `GraphServiceImpl.java` | Application service — inbound port |
| ImpactAnalyzer | `ImpactAnalyzerImpl.java` | BFS traversal for blast radius |
| DependencyParser | `DependencyParserImpl.java` | Parses `keystone.yml` declarations |
| GraphRepository | `GraphRepositoryImpl.java` | JPA persistence for graph data |
| GraphEventPublisher | `GraphEventPublisherImpl.java` | Publishes domain events |
| GraphController | `GraphController.java` | REST API |
| GraphHealthIndicator | `GraphHealthIndicator.java` | Health check endpoint |
| GraphMetrics | `GraphMetrics.java` | Micrometer metrics |

## Startup Sequence

### Dependencies (start order)

1. **PostgreSQL** — primary data store (graph schema)
2. **Contract Ingestion** — provides spec data and triggers event listeners
3. **Dependency Graph** — starts after PostgreSQL is ready

### Health Check

```
GET /actuator/health
```

Expected response:
```json
{"status": "UP"}
```

The dependency-graph module contributes a component health indicator that checks:
- Database connectivity (via `GraphRepository`)
- Service count (non-zero after initial registration)

### Startup Verification

```bash
# Check the module is registered and healthy
curl -s http://localhost:8080/actuator/health | jq .

# List registered services (should be empty initially)
curl -s http://localhost:8080/api/v1/graph/services | jq .

# Register a test service
curl -s -X POST http://localhost:8080/api/v1/graph/services \
  -H "Content-Type: application/json" \
  -d '{"name":"test-svc","team":"test","produces":[],"consumes":[]}' | jq .
```

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/graph/services` | Register a service and its API dependencies |
| GET | `/api/v1/graph/services` | List all registered services |
| GET | `/api/v1/graph/services/{id}` | Get a specific service |
| DELETE | `/api/v1/graph/services/{name}` | Remove a service |
| POST | `/api/v1/graph/impact` | Compute impact analysis (BFS) |

## Graceful Shutdown

The module handles graceful shutdown via Spring's `@PreDestroy` hooks on
the `GraphRepositoryImpl`. During shutdown:

1. In-flight impact analyses are allowed to complete (up to 30s timeout)
2. New service registration requests are rejected with 503
3. Pending events are flushed to the event bus

```bash
# Graceful shutdown
curl -X POST http://localhost:8080/actuator/shutdown

# Expected: 200 OK with shutdown initiated message
```

## Common Failure Modes

### Database Connection Loss

**Symptoms:**
- Health check returns `{"status": "DOWN"}`
- Graph repository queries throw `DataAccessResourceFailureException`
- Impact analysis returns empty results

**Recovery:**
```bash
# 1. Check database status
systemctl status postgresql

# 2. Restart PostgreSQL if needed
systemctl restart postgresql

# 3. Application will auto-reconnect via HikariCP pool
# No manual restart needed
```

### Graph Data Corruption

**Symptoms:**
- Missing services or dependencies
- Impact analysis returns incorrect results
- Log warnings about unknown services

**Recovery:**
```bash
# 1. Restore from backup
pg_restore -h localhost -U keystone \
  --data-only --schema=graph \
  /backup/graph/graph_latest.dump

# 2. Re-sync by re-running keystone.yml declarations
```

### Circular Dependency Detected

**Symptoms:**
- Log warning: "Cycle detected in dependency graph"
- Impact analysis reports unexpected affected services

**Recovery:**
```bash
# 1. Identify the cycle via health check
curl -s http://localhost:8080/api/v1/graph/services | jq .

# 2. Remove the circular dependency declaration
# from keystone.yml and re-register

# 3. The ImpactAnalyzer's visited set prevents infinite loops
# but the cycle should still be resolved
```

### Event Publishing Failure

**Symptoms:**
- DownstreamImpactComputedEvent not received by Notification Engine
- Dashboard not updating impact view

**Recovery:**
```bash
# 1. Check event publisher health
curl -s http://localhost:8080/actuator/health | jq .components.graph

# 2. Restart the application if event publisher is stuck
# Events are not persisted — they are fire-and-forget via Spring events
```

## Configuration Reference

| Property | Default | Description |
|----------|---------|-------------|
| `spring.datasource.url` | `jdbc:postgresql://localhost:5432/keystonedb` | PostgreSQL connection |
| `spring.datasource.schema` | `graph` | Database schema for graph tables |
| `graph.impact.timeout` | `30s` | Timeout for BFS impact analysis |
| `graph.registration.idempotent` | `true` | Allow duplicate registrations |
| `management.health.graph.enabled` | `true` | Enable graph health indicator |

## Metrics

| Metric | Type | Description |
|--------|------|-------------|
| `graph.registrations.total` | Counter | Total service registrations |
| `graph.impact.analyses` | Counter | Impact analyses performed |
| `graph.impact.duration` | Timer | BFS traversal time |
| `graph.registration.time` | Timer | Registration processing time |
| `graph.query.duration` | Timer | Repository query latency |
| `graph.cycles.detected` | Counter | Circular dependencies found |

## Tracing

The module propagates trace context via Micrometer Tracing (Brave). All
key operations are traced:

- `GraphService.registerService` — service registration span
- `GraphService.analyzeImpact` — impact analysis span
- `ImpactAnalyzerImpl.computeImpact` — BFS traversal sub-span
- `ImpactAnalyzerImpl.onBreakingChangeReported` — event listener span

## Logging

Structured logging is used with the following correlation keys:

| Key | Description |
|-----|-------------|
| `service` | Service name being registered/queried |
| `specPath` | Spec path being analyzed |
| `reportId` | Breaking change report UUID |
| `affectedCount` | Number of affected downstream services |

Example log entry:
```
INFO [traceId=abc123] c.k.g.d.s.ImpactAnalyzerImpl - Impact analysis complete: 3 affected services found
  service=payment-svc specPath=openapi/checkout.yaml reportId=uuid-123
```

## Troubleshooting

### Impact analysis returns no results

1. Verify the spec path matches exactly (case-sensitive)
2. Check that services are registered: `GET /api/v1/graph/services`
3. Verify the dependency edges exist in the database
4. Check logs for: "No producers found for spec"

### Service registration fails

1. Verify the `keystone.yml` format matches ADR-006
2. Check that consumed services are registered before consumers
3. Look for `UnknownServiceException` in logs
4. Verify database is reachable via health check

### High latency on impact analysis

1. Check the number of services/edges in the graph
2. Verify BFS timeout is configured appropriately
3. Look for circular dependencies that may cause deep traversal
4. Check database query performance via `graph.query.duration` metric

---

*Last updated: 2026-06-12*
