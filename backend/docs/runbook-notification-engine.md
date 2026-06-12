# Notification Engine Runbook

> **Module:** notification-engine
> **Package:** `com.keystone.notification`
> **Last Updated:** 2026-06-12

## Overview

The notification engine dispatches compliance verdicts and exemption events to
registered notification channels (CI status, email, Slack). It provides retry
logic with exponential backoff, circuit breaker protection, and idempotent
delivery tracking.

## Startup Sequence

### Dependencies (in order)

| Dependency | Required | Health Check | Timeout |
|-----------|----------|--------------|---------|
| PostgreSQL | Yes | `/actuator/health` shows DB up | 30s |
| GitHub API (for CI status) | No | Channel reports unavailable | N/A |
| SMTP server (for email) | No | Channel reports unavailable | N/A |

### Startup Order

1. **Database migrations** — Run automatically via Spring Boot / Flyway
2. **Bean initialization** — Spring creates beans in dependency order:
   - `NotificationRepositoryImpl` (in-memory or JPA)
   - `NotificationEventPublisherImpl` (Spring event bus)
   - `CiStatusChannelImpl` (auto-detected by `ChannelRegistryImpl`)
   - `ChannelRegistryImpl` (collects all `NotificationChannel` beans)
   - `NotificationDispatcherImpl` (depends on registry, repository, publisher)
3. **Endpoint availability** — REST API available at `/api/v1/notifications`
4. **Event listener registration** — `@EventListener` methods active

### Startup Verification

```bash
# Health endpoint
curl http://localhost:8080/actuator/health | jq .

# Channel status
curl http://localhost:8080/api/v1/notifications/channels | jq .

# Sample dispatch
curl -X POST http://localhost:8080/api/v1/notifications/dispatch \
  -H "Content-Type: application/json" \
  -d '{"eventType":"TestEvent","eventPayload":"{\"key\":\"value\"}"}'
```

## Graceful Shutdown

### Pre-shutdown Steps

1. **Stop accepting new events**: Disable the notification listener or set a flag
2. **Drain in-flight dispatches**: Wait for active `CompletableFuture` tasks
3. **Persist pending notifications**: Save any in-memory notifications
4. **Shutdown executor**: Virtual thread executor handles itself via JVM

### Shutdown Procedure

```bash
# Send SIGTERM
kill -TERM <pid>

# Spring Boot handles graceful shutdown via:
# - @PreDestroy methods
# - ExecutorService.shutdown() with 30s timeout
```

### Expected Shutdown Time

- Normal: < 2 seconds
- With in-flight dispatches: up to 30 seconds (max retry time)

## Configuration Reference

| Property | Default | Description |
|----------|---------|-------------|
| `github.api.base-url` | `https://api.github.com` | GitHub API base URL |
| `github.token` | (empty) | GitHub personal access token |
| `notification.ci-status.circuit-breaker.threshold` | `5` | Failures to open circuit |
| `notification.ci-status.circuit-breaker.cooldown` | `30` | Cooldown seconds before half-open |

## Common Failure Modes

### Circuit Breaker Open

**Symptom:** CI status updates not appearing on GitHub PRs.
**Cause:** GitHub API returning 429 (rate limit) or 5xx errors.
**Check:**
```bash
curl http://localhost:8080/api/v1/notifications/channels | jq '.channels[] | select(.name=="CI_STATUS")'
```
**Recovery:** Automatic — circuit half-opens after 30s cooldown.
**Escalation:** If circuit remains open for > 5 minutes, check GitHub token validity.

### All Channels Unavailable

**Symptom:** `dispatch()` returns FAILED for all channels.
**Check:**
```bash
curl http://localhost:8080/api/v1/notifications/channels | jq '.available'
```
**Recovery:** Verify each channel's external dependencies are reachable.
**Escalation:** If > 10 minutes, page on-call SRE.

### Notification Delivery Failures

**Symptom:** Failed notifications in the repository.
**Check:**
```bash
curl http://localhost:8080/api/v1/notifications/{notificationId}
```
**Recovery:** The dispatcher retries up to 3 times with exponential backoff
(1s, 4s, 10s). Manual re-dispatch via:
```bash
curl -X POST http://localhost:8080/api/v1/notifications/dispatch \
  -H "Content-Type: application/json" \
  -d '{"eventType":"PolicyEvaluated","eventPayload":"{...}"}'
```

### Database Full

**Symptom:** `NotificationRepository.save()` throws.
**Recovery:** Old notifications are deleted by retention policy
(`NotificationRepository.deleteOlderThan()`).
**Escalation:** Configure a scheduled job to call retention cleanup.

## Monitoring

### Key Metrics

| Metric | Type | Description |
|--------|------|-------------|
| `notification.dispatched` | Counter | Total events dispatched |
| `notification.delivered` | Counter | Successful deliveries |
| `notification.failed` | Counter | Failed deliveries |
| `notification.ci-status.time` | Timer | CI status API call duration |
| `notification.circuit-breaker.state` | Gauge | 0=closed, 1=open, 2=half-open |

### Health Endpoints

- `/actuator/health` — Overall application health
- `/api/v1/notifications/channels` — Channel availability

### Logging

All notification operations log at appropriate levels:
- `INFO`: Successful deliveries, channel registration
- `WARN`: Retry attempts, unavailable channels, circuit breaker open
- `ERROR`: Delivery failures after max retries
