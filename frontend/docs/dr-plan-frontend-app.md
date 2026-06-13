# Keystone Dashboard — Disaster Recovery Plan

> Disaster recovery plan for the frontend-app module.
> Canonical Reference: .pi/architecture/modules/frontend-app.md

---

## Overview

The frontend-app is a **stateless Next.js application**. It has no database, no queues, and no persistent state beyond non-sensitive `localStorage` (theme preference, last-viewed). This makes recovery inherently simple — the primary failure mode is the deployment itself.

**RTO (Recovery Time Objective):** 15 minutes
**RPO (Recovery Point Objective):** N/A (no mutable data)

---

## Backup Strategy

### What needs backing up

| Artifact | Why | Backup Method | Frequency |
|----------|-----|---------------|-----------|
| Source code | Application logic | Git repository | On every commit |
| Design tokens | Visual identity | Git repository | On every change |
| CI config | Build/deploy pipeline | Git repository | On every change |
| TypeScript types | API contracts | Git repository | On every change |

### What does NOT need backing up
- `node_modules/` — regenerated via `bun install`
- `.next/` — build cache, regenerated via `bun run build`
- `localStorage` — user preference only, not business critical

---

## Recovery Procedures

### Scenario 1: Complete Deployment Failure (RTO: 15 min)

**Trigger:** Production app returns 5xx or fails to serve.

**Recovery Steps:**
1. **Rollback deployment:** Revert to the last known-good commit
   ```bash
   git checkout <last-known-good-commit>
   bun install
   bun run build
   # Redeploy via platform CLI
   ```
2. **Verify health:**
   ```bash
   curl http://<production-url>/?view=overview
   ```
3. **If rollback fails:** Restore from Git
   ```bash
   git reset --hard <last-known-good-commit>
   git push --force-with-lease
   ```
4. **Notify stakeholders:** Post in #api-gov channel

### Scenario 2: Build Pipeline Failure

**Trigger:** CI pipeline fails, blocking deployments.

**Recovery Steps:**
1. Identify the failure: Check GitHub Actions logs
2. Common fixes:
   - **TypeScript errors:** Run `bun run typecheck` locally, fix all errors
   - **Test failures:** Run `bun run test:run` locally, fix failing tests
   - **Contract drift:** Run `bash scripts/validate-contracts.sh`, reconcile mismatches
3. Push fix commit and re-trigger CI

### Scenario 3: Configuration Corruption

**Trigger:** Environment variables misconfigured (wrong `NEXT_PUBLIC_KEYSTONE_API_URL`).

**Recovery Steps:**
1. Verify current config:
   ```bash
   # Check if the env var is set
   echo $NEXT_PUBLIC_KEYSTONE_API_URL
   ```
2. Correct the value:
   ```bash
   export NEXT_PUBLIC_KEYSTONE_API_URL=https://keystone-api.prod.company.com/api/v1
   ```
3. Redeploy with correct config
4. Verify backend reachable:
   ```bash
   curl $NEXT_PUBLIC_KEYSTONE_API_URL/dashboard/health
   ```

### Scenario 4: Data Corruption (Backend Sync Lost)

**Trigger:** Backend data is inconsistent or stale, but the app serves fine.

**Recovery Steps:**
1. This is primarily a backend incident — the frontend displays what the backend returns
2. Frontend action: Clear browser cache and reload
3. Verify with an incognito/private window
4. If issue persists, the backend needs recovery (see backend runbook)

---

## Restore Procedure

Since the frontend-app has no database, "restore" means redeploying from Git:

```bash
# 1. Clone fresh copy
git clone https://github.com/arman-jalili/Keystone.git
cd frontend

# 2. Checkout target version
git checkout <tag-or-commit>

# 3. Install and build
bun install
bun run build

# 4. Deploy (platform-specific)
# e.g., for Vercel: vercel --prod
# e.g., for Docker: docker build -t keystone-frontend . && docker run -p 3000:3000 keystone-frontend

# 5. Verify
curl http://localhost:3000/?view=overview
```

---

## Testing Recovery

| Drill | Frequency | Steps |
|-------|-----------|-------|
| Rollback test | Monthly | Deploy a breaking change, verify rollback restores service |
| Fresh deploy | Monthly | Deploy from scratch following restore procedure |
| CI failure drill | Quarterly | Introduce a TypeScript error and verify CI catches it |
| Config failure drill | Quarterly | Set wrong `NEXT_PUBLIC_KEYSTONE_API_URL` and verify ErrorState renders |

---

## Dependencies

| Dependency | Failure Impact | Mitigation |
|------------|---------------|------------|
| Keystone Backend API | Dashboard shows error states on all views | ErrorState component with Retry button; views recover automatically when backend is back |
| GitHub (source of truth) | Cannot deploy new versions | Local Git clones serve as backup; can deploy from any clone |
| npm / Bun registry | Cannot install dependencies | If registry is down, use lockfile with offline cache or previously installed node_modules |

---

## Communication

| Severity | Channel | Message |
|----------|---------|---------|
| Critical (app down) | #api-gov + email | "Keystone Dashboard is down. ETA: [X] min. Incident: [link]" |
| High (partial outage) | #api-gov | "Dashboard experiencing [issue]. Affected views: [list]. Investigating." |
| Medium (non-functional) | #api-gov | "Dashboard degraded: [description]. Fix in progress: [PR link]" |
| Low (cosmetic) | GitHub issue | Filed as issue #[number] |
