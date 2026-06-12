# DR Plan: Policy Engine

> **Status:** Production Ready
> **Last Updated:** 2026-06-12
> **Module:** policy-engine
> **RTO:** 1 hour
> **RPO:** 15 minutes

## Backup Strategy

### What to Back Up
| Asset | Backup Method | Frequency | Retention |
|-------|---------------|-----------|-----------|
| Policy database tables | PostgreSQL pg_dump | Every 6 hours | 30 days |
| Git policy repository | Git repository (source of truth) | Every push | Indefinite |
| Application configuration | Configuration management | Per deploy | Per release |

### Database Tables
```sql
-- Policies cache (can be rebuilt from Git)
SELECT * FROM policies;
SELECT * FROM policy_sets;

-- Evaluation history (business-critical)
SELECT * FROM policy_evaluation_results;
```

### Backup Commands
```bash
# Database backup
pg_dump -h localhost -U keystone -d keystone \
  --table=policies --table=policy_sets --table=policy_evaluation_results \
  > /backups/policy-engine-$(date +%Y%m%d-%H%M).sql

# Git repository (source of truth)
git clone --mirror https://github.com/org/policies.git \
  /backups/policies-mirror-$(date +%Y%m%d)
```

## Restore Procedure

### Full Restore
1. Stop the application: `systemctl stop keystone-server`
2. Restore database: `psql -h localhost -U keystone -d keystone < backup.sql`
3. Start the application: `systemctl start keystone-server`
4. Trigger policy sync: `curl -X POST /api/v1/policies/sync`
5. Verify: `curl GET /api/v1/policies | jq '. | length'`

### Git Repository Restore
1. If Git repo is lost, restore from mirror:
   ```bash
   git clone /backups/policies-mirror-20260612.git /tmp/policies-restore
   git -C /tmp/policies-restore remote add origin https://github.com/org/policies.git
   git -C /tmp/policies-restore push -f origin --all
   ```
2. Trigger re-sync: `curl -X POST /api/v1/policies/sync -d '{"sourceId":"default"}'`

### Partial Restore (Evaluation History)
1. Restore only evaluation results:
   ```bash
   pg_restore -h localhost -U keystone -d keystone \
     --table=policy_evaluation_results backup.sql
   ```

## Failover Plan

### Single Instance Failure
1. Health check detects failure (`/actuator/health` returns DOWN)
2. Load balancer routes traffic to healthy instance
3. New instance starts and syncs policies from Git
4. **RTO:** < 1 minute (with load balancer + auto-scaling)

### Database Failure
1. Connection pool exhausts, sync/evaluation operations fail
2. Read replica promotion (if configured)
   ```sql
   SELECT pg_promote();
   ```
3. Update connection string: `spring.datasource.url`
4. **RTO:** < 15 minutes
5. **Fallback:** Policy evaluations return ERROR verdict (not block)

### Git Repository Failure
1. Policy sync operations fail continuously
2. Existing policy cache remains usable (stale)
3. No new policies can be loaded
4. **RTO:** Until Git repo is restored
5. **Fallback:** Continue using last synced policies

## RTO/RPO Targets

| Scenario | RTO | RPO | Priority |
|----------|-----|-----|----------|
| Application crash | < 5 min | 0 | Critical |
| Database corruption | < 1 hour | 15 min | High |
| Git repository loss | < 2 hours | 24 hours | Medium |
| Full region failure | < 4 hours | 1 hour | Low |

## Disaster Recovery Team

| Role | Responsibility | Contact |
|------|---------------|---------|
| SRE | Infrastructure recovery | On-call rotation |
| Backend Developer | Policy data validation | Dev team |
| DBA | Database restore | DB team |

## Recovery Test Schedule

| Test | Frequency | Procedure |
|------|-----------|-----------|
| Application restart | Weekly | Restart service, verify sync works |
| Database restore | Monthly | Restore backup to staging, verify data |
| Git mirror restore | Quarterly | Restore from mirror, trigger sync |
| Full DR drill | Yearly | Complete region failover test |

## Post-Mortem Checklist

After any DR event:
- [ ] Restore completed successfully
- [ ] Data integrity verified
- [ ] All policies synced to cache
- [ ] Evaluation results consistent
- [ ] Runbook updated with lessons learned
- [ ] Alert thresholds adjusted if needed
