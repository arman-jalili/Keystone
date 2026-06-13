# Dependency Graph — Disaster Recovery Plan

> **Module:** dependency-graph (`com.keystone.graph`)
> **Last updated:** 2026-06-12
> **RTO:** 1 hour | **RPO:** 15 minutes

## Overview

This document describes the disaster recovery procedures for the Dependency Graph
module. The graph data consists of Service nodes and ApiDependency edges stored in
PostgreSQL. The module is stateless from a business logic perspective — all
persistent state is in the database.

## Recovery Objectives

| Metric | Target | Details |
|--------|--------|---------|
| **RTO** (Recovery Time Objective) | 1 hour | Time to restore full graph capability after failure |
| **RPO** (Recovery Point Objective) | 15 minutes | Maximum data loss window for graph data |
| **RTO (Critical)** | 15 minutes | For complete module failure during business hours |
| **RPO (Critical)** | 5 minutes | For graph data during active registration periods |

## Backup Strategy

### What Gets Backed Up

| Data | Backup Method | Frequency | Retention |
|------|--------------|-----------|-----------|
| `graph_services` table | PostgreSQL pg_dump | Every 15 minutes | 30 days |
| `graph_api_dependencies` table | PostgreSQL pg_dump | Every 15 minutes | 30 days |
| Full graph schema | PostgreSQL pg_dump | Daily | 90 days |
| Configuration (`application.yml`) | Git (part of repo) | On change | Per git history |

### Backup Procedure

```bash
# Incremental backup (every 15 min)
pg_dump -h localhost -U keystone \
  --table=graph_services \
  --table=graph_api_dependencies \
  --data-only \
  --file=/backup/graph/graph_data_$(date +%Y%m%d_%H%M).sql \
  keystonedb

# Full schema backup (daily)
pg_dump -h localhost -U keystone \
  --schema=graph \
  --file=/backup/graph/graph_schema_$(date +%Y%m%d).sql \
  keystonedb
```

### Backup Verification

```bash
# Verify backup file integrity
pg_restore --list /backup/graph/graph_data_latest.sql | head -10

# Test restore (on non-production)
pg_dump -h localhost -U keystone \
  --table=graph_services \
  --data-only \
  > /tmp/graph_test_restore.sql
```

## Restore Procedure

### Full Restore

```bash
# 1. Stop the application (if running)
systemctl stop keystone-server

# 2. Drop and recreate the graph tables
psql -h localhost -U keystone -d keystonedb <<EOF
DROP TABLE IF EXISTS graph_api_dependencies CASCADE;
DROP TABLE IF EXISTS graph_services CASCADE;
EOF

# 3. Restore schema
psql -h localhost -U keystone -d keystonedb \
  -f /backup/graph/graph_schema_latest.sql

# 4. Restore data
psql -h localhost -U keystone -d keystonedb \
  -f /backup/graph/graph_data_latest.sql

# 5. Start the application
systemctl start keystone-server

# 6. Verify restoration
curl -s http://localhost:8080/api/v1/graph/services | jq '. | length'
```

### Point-in-Time Recovery

```bash
# 1. Identify the target timestamp
TARGET_TIME="2026-06-12 14:30:00 UTC"

# 2. Restore the closest backup
psql -h localhost -U keystone -d keystonedb \
  -f /backup/graph/graph_data_20260612_1425.sql

# 3. Apply WAL logs up to target time (if PITR is enabled)
# Requires PostgreSQL WAL archiving to be configured
```

## Failover Plan

### Active-Passive Failover

```yaml
Primary:   pg-primary.internal (read/write)
Standby:   pg-standby.internal (read-only, promoted on failure)
```

**Failover Steps:**

```bash
# 1. Detect primary failure (health check timeout)
# 2. Promote standby to primary
pg_ctl promote -D /var/lib/postgresql/standby

# 3. Update application connection string
sed -i 's/pg-primary/pg-standby/' application.yml

# 4. Restart the application
systemctl restart keystone-server

# 5. Verify graph data is accessible
curl -s http://localhost:8080/api/v1/graph/services | jq .
```

### Failback

```bash
# 1. Repair the original primary
# 2. Replicate data from new primary
# 3. Switch application back (during maintenance window)
# 4. Verify data consistency
```

## Data Integrity Checks

### Automated Checks

Run the following queries after any restore:

```sql
-- Check for orphaned dependencies (consumer_id pointing to non-existent service)
SELECT * FROM graph_api_dependencies d
LEFT JOIN graph_services s ON d.consumer_id = s.id
WHERE d.consumer_id IS NOT NULL AND s.id IS NULL;

-- Check for orphaned dependencies (producer_id pointing to non-existent service)
SELECT * FROM graph_api_dependencies d
LEFT JOIN graph_services s ON d.producer_id = s.id
WHERE s.id IS NULL;

-- Check for duplicate edges
SELECT producer_id, consumer_id, spec_path, COUNT(*)
FROM graph_api_dependencies
GROUP BY producer_id, consumer_id, spec_path
HAVING COUNT(*) > 1;

-- Verify unique constraint on service names
SELECT name, COUNT(*)
FROM graph_services
GROUP BY name
HAVING COUNT(*) > 1;
```

### Manual Verification

```bash
# 1. List all services
curl -s http://localhost:8080/api/v1/graph/services | jq '.[].serviceName'

# 2. Run impact analysis on a known spec
curl -s -X POST http://localhost:8080/api/v1/graph/impact \
  -H "Content-Type: application/json" \
  -d '{"specPath":"openapi/checkout.yaml"}' | jq .

# 3. Verify BFS results match expected topology
```

## Prevention

### Data Loss Prevention

| Risk | Mitigation |
|------|------------|
| Accidental service deletion | Soft-delete not implemented — use `DELETE` with caution |
| Duplicate edge registration | Unique constraint on (producer_id, consumer_id, spec_path) |
| Service name conflict | Unique constraint on `name` column |
| Orphaned dependencies | Cascade delete when service is removed |

### Monitoring Alerts

| Alert | Threshold | Action |
|-------|-----------|--------|
| Graph health DOWN | Health check fails | Page on-call engineer |
| Service count drops suddenly | >50% reduction in 5 min | Investigate immediately |
| Impact analysis timeout | >30s per analysis | Review graph topology |
| Circular dependency | Detection logged | Notify team to resolve |

## Testing

### DR Test Schedule

| Test Type | Frequency | Success Criteria |
|-----------|-----------|-----------------|
| Backup integrity check | Daily | All backup files valid |
| Restore drill | Weekly | Full restore completes in <30 min |
| Failover drill | Monthly | Standby promoted in <5 min |
| Full DR exercise | Quarterly | RTO and RPO targets met |

### Test Procedure

```bash
# 1. Spin up a staging environment with duplicate configuration
# 2. Load the latest production backup
# 3. Verify all graph endpoints respond correctly
# 4. Run contract proofing scripts
bash .pi/scripts/ci/check_dependency-graph_contracts.sh

# 5. Measure recovery time
# 6. Document any issues
```

---

*Last updated: 2026-06-12*
