# Breaking Change Analysis — Disaster Recovery Plan

> **Module:** breaking-change-analysis (`com.keystone.analysis`)
> **Last updated:** 2026-06-12
> **RTO:** 1 hour | **RPO:** 15 minutes

## Overview

This document describes the disaster recovery procedures for the Breaking Change
Analysis module. This module is stateless from a business logic perspective — all
persistent state is stored in PostgreSQL via `ChangeReportRepository`.

## Recovery Objectives

| Metric | Target | Details |
|--------|--------|---------|
| **RTO** (Recovery Time Objective) | 1 hour | Time to restore full analysis capability after failure |
| **RPO** (Recovery Point Objective) | 15 minutes | Maximum data loss window for report data |
| **RTO (Critical)** | 15 minutes | For complete module failure during business hours |
| **RPO (Critical)** | 5 minutes | For report data during active analysis |

## Backup Strategy

### What Gets Backed Up

| Data | Backup Method | Frequency | Retention |
|------|--------------|-----------|-----------|
| `breaking_change_reports` table | PostgreSQL pg_dump | Every 15 minutes | 30 days |
| `breaking_change_reports` table (full) | PostgreSQL pg_dump | Daily | 90 days |
| Configuration (`application.yml`) | Git (part of repo) | On change | Per git history |

### Backup Procedure

```bash
# Incremental backup (every 15 min)
pg_dump -h localhost -U keystone \
  --table=breaking_change_reports \
  --data-only \
  --file=/backup/analysis/breaking_change_reports_$(date +%Y%m%d_%H%M).sql \
  keystonedb

# Full backup (daily)
pg_dump -h localhost -U keystone \
  --table=breaking_change_reports \
  --format=custom \
  --file=/backup/analysis/breaking_change_reports_daily_$(date +%Y%m%d).dump \
  keystonedb
```

### Backup Verification

```bash
# Verify backup file integrity
pg_restore --list /backup/analysis/breaking_change_reports_daily_*.dump | head -5

# Verify backup contains data
ls -la /backup/analysis/ | grep breaking_change
```

## Restore Procedure

### Restore from Backup

```bash
# 1. Stop the application
systemctl stop keystone-analysis

# 2. Drop and recreate the report table
psql -h localhost -U keystone -d keystonedb -c "
  TRUNCATE TABLE breaking_change_reports;
"

# 3. Restore from the most recent backup
pg_restore -h localhost -U keystone \
  --data-only \
  --dbname=keystonedb \
  /backup/analysis/breaking_change_reports_daily_$(date +%Y%m%d).dump

# 4. Verify data integrity
psql -h localhost -U keystone -d keystonedb -c "
  SELECT COUNT(*) as report_count,
         MIN(completed_at) as oldest,
         MAX(completed_at) as newest
  FROM breaking_change_reports;
"

# 5. Restart the application
systemctl start keystone-analysis

# 6. Verify health
curl -s http://localhost:8080/actuator/health | jq .
```

### Point-in-Time Recovery

If the database supports PITR (PostgreSQL WAL archiving):

```bash
# Restore to a specific timestamp (< 15 min RPO)
pg_restore --target-time "2026-06-12 14:30:00 UTC" \
  --dbname=keystonedb \
  /backup/wal/archive/
```

## Failover Plan

### Single Instance Failure

1. **Detection:** Health check returns DOWN or timeout on `/actuator/health`.
2. **Action:** Load balancer removes the failed instance from rotation.
3. **Recovery:** Remaining instances handle the load. If all instances fail,
   proceed to full failover.
4. **Verification:** Check remaining instances handle analysis requests.

### Full Module Failure

1. **Detection:** All instances are DOWN; `analysis.diff.time` metric stops
   reporting.
2. **Action:** Activate standby deployment (if available) or rebuild from
   latest backup.
3. **Procedure:**

   ```bash
   # 1. Deploy the latest known-good version
   git checkout tags/v1.0.0-analysis-stable
   mvn clean package -DskipTests

   # 2. Ensure database schema is up to date
   java -jar target/keystone.jar --spring.flyway.enabled=true

   # 3. Restore report data from latest backup
   # (see Restore Procedure above)

   # 4. Start the application
   systemctl start keystone-analysis

   # 5. Verify full recovery
   curl -s http://localhost:8080/actuator/health | jq .
   curl -s -X POST http://localhost:8080/api/v1/breaking/analyze \
     -H "Content-Type: application/json" \
     -d '{
       "repository": "org/repo",
       "specPath": "openapi.yaml",
       "commitSha": "0000000000000000000000000000000000000000"
     }' | jq .
   ```

4. **Verification:** All 4 API endpoints return expected responses.
5. **Failback:** Once original deployment is repaired, redirect traffic back.

## Data Loss Scenarios

### Scenario A: Report Data Lost (<15 min)

**Impact:** Up to 15 minutes of analysis reports are lost.
**Action:** No immediate action needed — analysis will be re-triggered on next
spec ingestion. The CLI can re-submit analysis requests if needed.

### Scenario B: Report Data Lost (>15 min)

**Impact:** Analysis history is lost beyond the RPO window.
**Action:** 
1. Restore from the most recent backup.
2. Accept that some reports between the last backup and the failure are lost.
3. Notify affected users that their analysis history may be incomplete.

### Scenario C: Complete Database Loss

**Impact:** All report data is lost.
**Action:**
1. Restore from the most recent full daily backup + incremental backups.
2. If backups are also lost, the module will start fresh with no history.
3. All existing reports will need to be regenerated via re-analysis.
4. Notify all users with a communication plan.

## Failover Testing

### Schedule

| Test Type | Frequency | Success Criteria |
|-----------|-----------|------------------|
| Backup verification | Daily | Backup files are non-empty and restorable |
| Restore drill | Monthly | Full restore completes within 30 minutes |
| Failover drill | Quarterly | Full failover completes within RTO (1 hour) |

### Test Script

```bash
#!/bin/bash
# Failover drill script
set -euo pipefail

echo "=== Failover Drill: Breaking Change Analysis ==="

# 1. Verify backups
echo "[1/5] Verifying backups..."
ls -la /backup/analysis/breaking_change_reports_daily_*.dump

# 2. Simulate failure
echo "[2/5] Simulating module failure..."
systemctl stop keystone-analysis

# 3. Verify module is down
echo "[3/5] Verifying module is down..."
curl -s http://localhost:8080/actuator/health && exit 1 || echo "Module is down (expected)"

# 4. Restore and restart
echo "[4/5] Restoring and restarting..."
# (restore procedure here)
systemctl start keystone-analysis
sleep 10

# 5. Verify module is up
echo "[5/5] Verifying module is up..."
curl -s http://localhost:8080/actuator/health | jq -e '.status == "UP"'
echo "=== Failover Drill Complete ==="
```

## Rollback Plan

### Rolling Back a Deployment

```bash
# 1. Identify the previous working version
helm history keystone-analysis
# REVISION  UPDATED          STATUS      CHART
# 3         ...               deployed    keystone-analysis-1.0.0
# 2         ...               superseded  keystone-analysis-0.9.0

# 2. Rollback to the previous revision
helm rollback keystone-analysis 2

# 3. Verify rollback
kubectl rollout status deployment/keystone-analysis
curl -s http://localhost:8080/actuator/health | jq .
```

### Rolling Back a Data Migration

```bash
# Flyway undo (if supported) or manual rollback
psql -h localhost -U keystone -d keystonedb -f /rollback/V2__undo_analysis_schema_change.sql
```

## Contact Information

| Role | Contact | Escalation |
|------|---------|------------|
| On-call Engineer | [#oncall](https://keystone.slack.com/archives/oncall) | PagerDuty |
| Database Admin | [#database](https://keystone.slack.com/archives/database) | 1-hour SLA |
| Module Owner | @breaking-change-analysis-team | Business hours |

---

*Generated with Guardian compliance workflow*
