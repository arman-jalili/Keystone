# =============================================================================
# Keystone Dashboard — Dockerfile
# =============================================================================
# Multi-stage build: pnpm → Next.js standalone output
#
# Build:
#   docker compose build keystone-dashboard
#
# Run:
#   docker compose up -d
# =============================================================================

# ── Stage 1: Install & Build ──
FROM node:22-alpine AS builder

RUN corepack enable && corepack prepare pnpm@latest --activate

WORKDIR /app

COPY package.json pnpm-lock.yaml ./
COPY pnpm-workspace.yaml ./
COPY .pnpmrc.json ./

RUN pnpm install --frozen-lockfile

COPY . .

RUN pnpm build

# ── Stage 2: Production Runner ──
FROM node:22-alpine AS runner

WORKDIR /app

ENV NODE_ENV=production

COPY --from=builder /app/.next/standalone ./
COPY --from=builder /app/.next/static ./.next/static
COPY --from=builder /app/public ./public

EXPOSE 3000

ENV PORT=3000
ENV HOSTNAME=0.0.0.0

CMD ["node", "server.js"]
