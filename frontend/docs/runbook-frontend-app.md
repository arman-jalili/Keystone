# Keystone Dashboard — Runbook

> Operational runbook for the frontend-app module.
> Canonical Reference: .pi/architecture/modules/frontend-app.md

---

## Overview

The Keystone Dashboard is a **Next.js (App Router) single-page application** that surfaces API governance data from the Keystone backend REST API. It is purely a view layer — no backend domain logic.

**Tech Stack:** TypeScript (Bun/Node), Next.js 15, React 19, Tailwind CSS v4
**Environment Variables:** `NEXT_PUBLIC_KEYSTONE_API_URL`

---

## Startup Sequence

### Prerequisites
- Node.js ≥ 18 or Bun ≥ 1.0
- Keystone backend running (default: `http://localhost:8080`)

### Steps

```bash
# 1. Install dependencies
cd frontend
bun install

# 2. Set backend URL (optional, defaults to localhost:8080)
export NEXT_PUBLIC_KEYSTONE_API_URL=http://localhost:8080/api/v1

# 3. Development mode
bun run dev

# 4. Production build
bun run build
bun run start
```

### Verification
1. Open `http://localhost:3000` — should redirect to `/?view=overview`
2. NavRail should show 6 items with wordmark "Keystone · API Governance"
3. Theme toggle should work and persist across refresh
4. View switching should update URL without full page reload

---

## Graceful Shutdown

- `SIGTERM`: Next.js handles graceful shutdown — in-flight requests complete within 30s
- `SIGINT` (Ctrl+C): Immediate stop in development
- No database connections to close; no queues to drain

---

## Common Failure Modes

### 1. Backend Unreachable

**Symptoms:** All views show error state ("Unable to load view") with retry button.

**Cause:** Keystone backend is down, or `NEXT_PUBLIC_KEYSTONE_API_URL` is misconfigured.

**Recovery:**
1. Verify backend is running: `curl http://localhost:8080/api/v1/dashboard/health`
2. Check `NEXT_PUBLIC_KEYSTONE_API_URL` in `.env.local` or deployment config
3. Click "Retry" on the view — it re-fetches data
4. If backend is starting up, views will automatically recover on next poll

### 2. Build Failures

**Symptoms:** `bun run build` exits with errors.

**Common Causes:**
- TypeScript type errors after contract changes
- Missing import or component
- Tailwind config syntax error

**Recovery:**
1. Run `bun run typecheck` to see all TypeScript errors
2. Verify all frozen contracts match implementations (`bash scripts/validate-contracts.sh`)
3. Check `tailwind.config.ts` for syntax errors
4. Clear `.next/` cache and rebuild: `rm -rf .next && bun run build`

### 3. Theme FOUC (Flash of Unstyled Content)

**Symptoms:** Brief flash of light theme before dark theme applies on reload.

**Cause:** The inline FOUC-prevention script in `app/layout.tsx` is not executing before React hydration.

**Recovery:**
1. Verify the `<script>` tag is in `<head>`, not `<body>`
2. Check that `suppressHydrationWarning` is on `<html>`
3. Verify `localStorage` key matches `THEME_STORAGE_KEY` in `lib/contracts/theme.ts`

### 4. Missing Environment Variable

**Symptoms:** All API calls fail; `process.env.NEXT_PUBLIC_KEYSTONE_API_URL` is undefined.

**Cause:** `.env.local` or deployment environment doesn't have the variable.

**Recovery:**
1. Create `.env.local`: `NEXT_PUBLIC_KEYSTONE_API_URL=http://localhost:8080/api/v1`
2. For production, set environment variable in deployment platform
3. Restart the dev server / redeploy

---

## Configuration Reference

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `NEXT_PUBLIC_KEYSTONE_API_URL` | Yes | `http://localhost:8080/api/v1` | Base URL for Keystone backend REST API |
| `NODE_ENV` | No | `development` | Set to `production` for production builds |
| `PORT` | No | `3000` | Port for the dev server |

---

## Health Checks

| Check | Endpoint / Command | Expected |
|-------|-------------------|----------|
| App is serving | `curl http://localhost:3000` | 200 OK, HTML response |
| TypeScript compiles | `bun run typecheck` | Exit code 0 |
| Tests pass | `bun run test:run` | All tests green |
| Contract freeze | `bash scripts/validate-contracts.sh` | All checks pass |

---

## Monitoring

- **Build status:** GitHub Actions CI workflow (`.github/workflows/ci.yml`)
- **Test coverage:** `bun run test:coverage` (threshold: 90% lines/functions, 80% branches)
- **TypeScript strictness:** `tsconfig.json` has `strict: true`
