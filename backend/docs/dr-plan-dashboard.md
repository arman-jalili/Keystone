# Dashboard Module Disaster Recovery Plan

> **Module:** dashboard (com.keystone.dashboard)
> **Last updated:** 2026-06-13
> **Canonical Reference:** .pi/architecture/modules/dashboard.md

## RTO/RPO Targets

| Metric | Target | Notes |
|--------|--------|-------|
| RTO (Recovery Time Objective) | 15 minutes | Time to restore dashboard service |
| RPO (Recovery Point Objective) | 1 hour | Maximum data loss on failure |
| MTD (Maximum Tolerable Downtime) | 4 hours | Before escalation |

## Backup Strategy

### Database

The Dashboard module does not have its own database — it reads from the shared PostgreSQL instance used by all modules.

| Backup Type | Schedule | Retention | Method |
|-------------|----------|-----------|--------|
| Full database | Daily at 02:00 UTC | 30 days | `pg_dump -Fc` |
| WAL archiving | Continuous | 7 days | PostgreSQL WAL archiving |
| Point-in-time recovery | N/A | WAL retention period | `pg_restore --recovery-conf-target-timeline` |

Backup command:
```bash
pg_dump -h localhost -U keystone -Fc -f /backups/keystone_$(date +%Y%m%d).dump keystone
```

### Source Code

| Backup Type | Schedule | Location |
|-------------|----------|----------|
| Git repository | Every push | GitHub (remote) |
| Architecture docs | Every push | `.pi/architecture/` (in repo) |

## Restore Procedure

### Level 1: Service Restart (RTO: 2 minutes)

If the dashboard service crashes:

```bash
# Restart the service
cd /opt/keystone
mvn spring-boot:run -Dspring-boot.run.profiles=production

# Or via systemd
sudo systemctl restart keystone-dashboard
```

Verify recovery:
```bash
curl -s http://localhost:8080/actuator/health | grep -q '"status":"UP"'
curl -s http://localhost:8080/api/v1/dashboard/summary | head -1
```

### Level 2: Database Recovery (RTO: 15 minutes)

If the database is corrupted:

```bash
# 1. Stop the dashboard service
sudo systemctl stop keystone-dashboard

# 2. Restore the database from latest backup
pg_restore -h localhost -U keystone -d keystone --clean /backups/keystone_latest.dump

# 3. Verify database integrity
psql -h localhost -U keystone -d keystone -c "SELECT count(*) FROM policies;"

# 4. Restart the dashboard service
sudo systemctl start keystone-dashboard
```

### Level 3: Full Instance Recovery (RTO: 1 hour)

If the entire server is lost:

```bash
# 1. Provision new server with JDK 21 and PostgreSQL 16
# 2. Clone the repository
git clone https://github.com/arman-jalili/Keystone.git /opt/keystone
cd /opt/keystone/backend

# 3. Restore database from backup
pg_restore -h localhost -U keystone -d keystone /backups/keystone_latest.dump

# 4. Build and start
mvn clean package -DskipTests
java -jar target/keystone-server-0.1.0.jar --spring.profiles.active=production

# 5. Verify
curl -s http://localhost:8080/actuator/health
```

## Failover Plan

### Single Instance Failure

The dashboard runs as a single Java process. On failure:
1. Health check detects unavailability (liveness probe fails)
2. Orchestrator (Kubernetes/systemd) restarts the process
3. Dashboard resumes serving within ~30 seconds

### Database Failure

If PostgreSQL becomes unavailable:
1. Dashboard health check reports DOWN
2. Endpoints return 503 Service Unavailable
3. On database recovery, dashboard automatically reconnects
4. Stale cache is cleared on reconnection

### Upstream Module Failure

If an upstream module (ingestion, policy, analysis) is unavailable:
1. Dashboard serves degraded data — affected widgets show "No Data"
2. Non-affected features continue to work
3. Log warning: `Upstream module X unavailable, dashboard serving degraded data`

## Disaster Scenarios

### Scenario 1: Data Corruption

**Symptom:** Dashboard shows incorrect or nonsensical data.

**Action:**
1. Isolate the corruption — identify which data source is affected
2. Restore the affected data from backup
3. Clear dashboard cache
4. Verify data integrity

### Scenario 2: Configuration Loss

**Symptom:** Dashboard fails to start with configuration errors.

**Action:**
1. Restore `application.yml` from version control
2. Verify database connection string
3. Restart the service

### Scenario 3: Security Incident

**Symptom:** Unauthorized access to dashboard data.

**Action:**
1. Revoke compromised credentials
2. Rotate database passwords
3. Audit access logs
4. Apply security patches

## Testing

The DR plan should be tested quarterly:

| Test | Frequency | Success Criteria |
|------|-----------|-----------------|
| Service restart | Monthly | Dashboard recovers within 2 minutes |
| Database restore | Quarterly | Data restored within 15 minutes |
| Full instance recovery | Annually | Server provisioned and running within 1 hour |
