# Disaster Recovery Plan: contract-ingestion

> **Module:** contract-ingestion
> **Version:** v0.1.0
> **Last Updated:** 2026-06-12

## RTO / RPO Targets

| Metric | Target | Measurement |
|--------|--------|-------------|
| Recovery Time Objective (RTO) | 1 hour | Time from disaster declaration to full recovery |
| Recovery Point Objective (RPO) | 15 minutes | Maximum acceptable data loss |
| Failover Time | 5 minutes | Time to switch to standby instance |
| Max Acceptable Downtime | 4 hours per quarter | Cumulative |

## Backup Strategy

### Database (PostgreSQL)

| Item | Policy |
|------|--------|
| **Full backup** | Daily at 02:00 UTC |
| **WAL archiving** | Continuous (every 5 minutes) |
| **Retention** | 30 days for daily backups, 7 days for WAL |
| **Encryption** | AES-256 at rest |
| **Storage** | S3 bucket (cross-region replicated) |
| **Verification** | Weekly restore test to staging environment |

**Tables to back up:**
- `openapi_specs` — Core spec data
- `spec_versions` — Versioned spec content
- `idempotency_keys` — Dedup keys (7-day TTL)

### Application

| Item | Policy |
|------|--------|
| **Configuration** | Stored in `application.yml` + environment variables |
| **Build artifacts** | Published to Maven repository / Docker registry |
| **Infrastructure** | Infrastructure-as-Code (Terraform / Helm) |

## Restore Procedure

### Database Restore

```bash
# 1. Stop the application
systemctl stop keystone-server

# 2. Restore from latest full backup
pg_restore -h <db-host> -U keystone -d keystone \
  --clean --if-exists \
  /backups/keystone_$(date +%Y%m%d).dump

# 3. Replay WAL to catch up to desired recovery point
# (Point-in-Time Recovery)
# Configure recovery.conf with restore_command for WAL archive

# 4. Verify data integrity
psql -h <db-host> -U keystone -d keystone -c \
  "SELECT count(*) FROM openapi_specs;"
psql -h <db-host> -U keystone -d keystone -c \
  "SELECT count(*) FROM spec_versions;"

# 5. Restart the application
systemctl start keystone-server
```

### Partial Restore (Single Spec)

```bash
# Restore from WAL archive for a specific timeframe
pg_waldump <wal-segment> | grep <spec-id>

# Re-insert the affected spec version manually if needed
```

## Failover Plan

### Scenario: Primary Database Failure

1. **Detect** — Health check fails, monitoring alerts
2. **Verify** — Check PostgreSQL process, disk space, network
3. **Failover** — Promote standby replica:
   ```bash
   pg_ctl promote -D /var/lib/postgresql/standby
   ```
4. **Reconfigure** — Update application datasource URL:
   ```bash
   kubectl set env deployment/keystone-server \
     SPRING_DATASOURCE_URL=jdbc:postgresql://standby-host:5432/keystone
   ```
5. **Verify** — Check `/actuator/health` returns UP
6. **Repair** — Restore primary and re-establish replication

### Scenario: Application Failure

1. **Detect** — Load balancer health check fails
2. **Restart** — Kubernetes will auto-restart the pod
3. **Verify** — Check pod logs and health endpoint
4. **Scale** — If crash-looping, rollback to previous version

## Data Integrity Checks

Run after any restore:

```sql
-- Check for orphaned spec versions (versions without a parent spec)
SELECT sv.id FROM spec_versions sv
LEFT JOIN openapi_specs os ON sv.spec_id = os.id
WHERE os.id IS NULL;

-- Check idempotency keys reference valid events
SELECT ik.event_id FROM idempotency_keys ik
LEFT JOIN audit_events ae ON ik.event_id = ae.id
WHERE ae.id IS NULL;

-- Verify no duplicate idempotency keys
SELECT repository, commit_sha, spec_path, count(*)
FROM idempotency_keys
GROUP BY repository, commit_sha, spec_path
HAVING count(*) > 1;
```

## Scheduled Maintenance

| Activity | Frequency | Expected Downtime |
|----------|-----------|-------------------|
| Backup verification restore | Weekly | 0 (staging environment) |
| Idempotency key TTL cleanup | Daily | 0 (background job) |
| Database vacuum/analyze | Weekly | 0 (concurrent) |
| SSL certificate renewal | Quarterly | 0 (rolling update) |
| Dependency updates | Monthly | < 1 minute |
| Major version upgrades | Yearly | < 30 minutes |

## Communication Plan

| Severity | Definition | Notification | Response Time |
|----------|------------|-------------|---------------|
| **SEV1** | Complete ingestion outage | PagerDuty + Slack | 15 minutes |
| **SEV2** | Partial degradation (>10% errors) | Slack | 1 hour |
| **SEV3** | Non-critical issue | Jira ticket | Next business day |

## Post-Mortem Process

1. Document timeline of events
2. Identify root cause
3. Create action items with owners
4. Schedule follow-up review (within 5 business days)
5. Update runbook and DR plan

## Testing the Plan

The DR plan must be tested quarterly:
1. **Q1:** Tabletop exercise (walk through scenarios)
2. **Q2:** Database restore drill (staging environment)
3. **Q3:** Full failover test (staging to DR)
4. **Q4:** Chaos engineering (random component failure)
