# Disaster Recovery Plan: Notification Engine

> **Module:** notification-engine
> **Package:** `com.keystone.notification`
> **Version:** v0.1.0
> **Last Updated:** 2026-06-12

## Overview

This document covers disaster recovery procedures for the notification engine module.
The notification engine is stateless with respect to event processing — it reads,
dispatches, and persists delivery results. The primary recovery concern is
notification history (delivery records).

## Recovery Objectives

| Metric | Target | Measurement |
|--------|--------|-------------|
| **RTO** (Recovery Time Objective) | 30 minutes | Time from disaster declaration to operational service |
| **RPO** (Recovery Point Objective) | 5 minutes | Max acceptable notification data loss |
| **MTO** (Maximum Tolerable Outage) | 2 hours | Max time before manual escalation |

## Backup Strategy

### What to Back Up

| Data | Backup Method | Frequency | Retention |
|------|---------------|-----------|-----------|
| Notification records | PostgreSQL dump | Hourly | 7 days |
| Channel configuration | Git-tracked config | Per deployment | Full git history |

### Backup Commands

```bash
# PostgreSQL backup of notification data
pg_dump -h localhost -U keystone -d keystone \
  -t notifications \
  -f /backups/keystone/notification-$(date +%Y%m%d-%H%M%S).sql

# Rotate old backups (keep 7 days)
find /backups/keystone/ -name "notification-*.sql" -mtime +7 -delete
```

### Backup Verification

```bash
# Verify backup integrity
pg_restore --list /backups/keystone/notification-$(date +%Y%m%d-%H%M%S).sql

# Spot-check: restore to temp database
createdb keystone_restore_test
pg_restore -d keystone_restore_test /backups/keystone/notification-*.sql
psql -d keystone_restore_test -c "SELECT COUNT(*) FROM notifications;"
dropdb keystone_restore_test
```

## Restore Procedure

### Scenario 1: Database Corruption

**Symptoms:**
- `NotificationRepository` queries return errors
- Health check shows database DOWN

**Recovery Steps:**

1. **Stop the application**
   ```bash
   systemctl stop keystone-server
   ```

2. **Restore database from backup**
   ```bash
   # Drop and recreate the database
   dropdb keystone
   createdb keystone

   # Restore from most recent backup
   pg_restore -d keystone /backups/keystone/notification-latest.sql

   # Run any pending migrations
   mvn flyway:migrate
   ```

3. **Verify data integrity**
   ```bash
   psql -d keystone -c "SELECT COUNT(*) FROM notifications;"
   ```

4. **Start the application**
   ```bash
   systemctl start keystone-server
   ```

5. **Verify health**
   ```bash
   curl http://localhost:8080/actuator/health | jq .
   curl http://localhost:8080/api/v1/notifications/channels | jq .
   ```

**RPO Impact:** Up to 1 hour of notification records may be lost (hourly backup cadence).

### Scenario 2: Application Crash

**Symptoms:**
- Application process exited unexpectedly
- 502 errors on API endpoints

**Recovery Steps:**

1. **Check crash logs**
   ```bash
   journalctl -u keystone-server --since "5 minutes ago"
   ```

2. **Restart the application**
   ```bash
   systemctl restart keystone-server
   ```

3. **Verify recovery**
   ```bash
   journalctl -u keystone-server --since "30 seconds ago" | tail -20
   curl http://localhost:8080/actuator/health
   ```

**RPO Impact:** Minimal — in-memory notifications may be lost (within RPO).

### Scenario 3: Full Region Failure

**Recovery Steps:**

1. **Provoke DNS failover to secondary region**
2. **Provision new database from latest backup**
   ```bash
   pg_restore -d keystone /backups/keystone/notification-latest.sql
   ```
3. **Deploy application in secondary region**
   ```bash
   mvn package -DskipTests
   java -jar target/keystone-server-*.jar --spring.profiles.active=dr
   ```
4. **Verify operation**
   ```bash
   curl http://keystone-dr/api/v1/notifications/channels | jq .
   ```

## Failover Plan

| Trigger | Action | Owner | SLA |
|---------|--------|-------|-----|
| Database unreachable > 1 min | Failover to read replica | DBA | 5 min |
| Application unresponsive > 30s | Restart process | DevOps | 2 min |
| Region unreachable | DNS failover | SRE | 10 min |

## Testing Schedule

| Test | Frequency | Procedure |
|------|-----------|-----------|
| Backup restore | Monthly | Restore to test environment, verify data |
| Application restart | Quarterly | Kill process, verify auto-restart |
| Failover drill | Bi-annual | Full region failover exercise |

## Post-Mortem Checklist

- [ ] Determine root cause
- [ ] Assess data loss (if any)
- [ ] Verify RTO/RPO were met
- [ ] Update runbook with lessons learned
- [ ] Schedule preventive improvements
