# Keystone

**OpenAPI Specification Governance Platform**

<p align="center">
  <img src="https://img.shields.io/badge/status-public-2ea44f?style=for-the-badge" alt="Public Release"/>
  <img src="https://img.shields.io/badge/version-0.1.0--alpha-blue?style=for-the-badge" alt="Version 0.1.0-alpha"/>
</p>

<p align="center">
  <a href="#features">Features</a> •
  <a href="#architecture">Architecture</a> •
  <a href="#quick-start">Quick Start</a> •
  <a href="#api-endpoints">API</a> •
  <a href="CONTRIBUTING.md">Contributing</a>
</p>

[![CI](https://github.com/arman-jalili/Keystone/actions/workflows/ci.yml/badge.svg)](https://github.com/arman-jalili/Keystone/actions/workflows/ci.yml)
[![Java 21](https://img.shields.io/badge/Java-21%2B-blue)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-green)](https://spring.io/projects/spring-boot)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Tests](https://img.shields.io/badge/tests-250%20passing-brightgreen)](https://github.com/arman-jalili/Keystone/actions)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen)](CONTRIBUTING.md)

**Keystone is now public.** 🎉

Keystone is an open-source governance platform for OpenAPI specifications. It ingests, analyzes, and enforces policy on API specs across your organization, providing a dashboard for visualizing governance health, compliance trends, and policy violations.

Built with **Java 21 + Spring Boot 3.2**, Keystone follows Clean Architecture with 6 bounded contexts, 250+ tests, and zero critical gaps.

Keystone is a governance platform for OpenAPI specifications. It ingests, analyzes, and enforces policy on API specs across your organization, providing a dashboard for visualizing governance health, compliance trends, and policy violations.

---

## Features

- **📥 Contract Ingestion** — Upload OpenAPI 3.0/3.1 specs via API, CLI, or GitHub webhook integration
- **🔍 Breaking Change Analysis** — Diff-based detection of field removals, type changes, path removals, deprecations, and more using 6 pluggable detectors
- **📋 Policy Engine** — Define DSL-based policies and evaluate specs against them in real time
- **📊 Governance Dashboard** — Health scores, compliance trends, policy breakdowns, and violation tracking
- **🔗 Dependency Graph** — Map service-to-service API dependencies and compute impact cascades
- **🔔 Notifications** — Multi-channel dispatch (CI status, email, webhook) with retries and circuit breakers
- **📝 Audit Trail** — Append-only event store recording all governance actions
- **🧩 Clean Architecture** — Modular bounded contexts with domain-driven design

---

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Keystone Dashboard                       │
│              Next.js 22 (TypeScript, Tailwind)              │
│                http://localhost:3000                         │
└──────────────────────┬──────────────────────────────────────┘
                       │ REST / JSON
┌──────────────────────▼──────────────────────────────────────┐
│                  Keystone Server (Java 21)                  │
│              Spring Boot 3.2 / Micrometer / AMQP            │
│                http://localhost:8080                         │
│                                                              │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌────────────┐  │
│  │Ingestion │  │ Analysis │  │  Policy  │  │ Dashboard  │  │
│  │          │  │          │  │          │  │            │  │
│  │ Spec     │  │ Diff     │  │ DSL      │  │ Health     │  │
│  │ Upload   │  │ Detect   │  │ Evaluate │  │ Scores     │  │
│  │ Webhook  │  │ Reports  │  │ Sync     │  │ Audit Log  │  │
│  └──────────┘  └──────────┘  └──────────┘  └────────────┘  │
│  ┌──────────┐  ┌──────────────┐                              │
│  │  Graph   │  │ Notification │                              │
│  │          │  │              │                              │
│  │ Depend.  │  │ Channels    │                              │
│  │ Impact   │  │ Dispatch    │                              │
│  └──────────┘  └──────────────┘                              │
└──────┬──────────────────────┬────────────────────────────────┘
       │                      │
┌──────▼──────┐      ┌───────▼────────┐
│ PostgreSQL  │      │    RabbitMQ    │
│  16         │      │  3.13         │
│ (prod) / H2 │      │ (message bus) │
│ (dev)       │      └────────────────┘
└─────────────┘
```

### Bounded Contexts

| Module | Package | Purpose | Key Components |
|--------|---------|---------|----------------|
| **Contract Ingestion** | `com.keystone.ingestion` | OpenAPI spec ingestion, deduplication, storage | `IngestionController`, `SpecValidator`, `SpecRepository` |
| **Breaking Change Analysis** | `com.keystone.analysis` | Diff-based breaking change detection | `DiffOrchestrator`, 6 `ChangeDetector` implementations, `BaseVersionResolver` |
| **Policy Engine** | `com.keystone.policy` | Policy definition, DSL evaluation, Git sync | `DslParser`, `DslExecutor`, `EvaluationEngine`, `PolicySyncScheduler` |
| **Dashboard** | `com.keystone.dashboard` | Health scores, policy UI, compliance views, audit log | `DashboardController`, `HealthScoreService`, `AuditLogService` |
| **Dependency Graph** | `com.keystone.graph` | Service dependency mapping and impact analysis | `ImpactAnalyzer`, `DependencyParser`, `GraphRepository` |
| **Notification Engine** | `com.keystone.notification` | Multi-channel notification dispatch | `NotificationDispatcher`, `CiStatusChannel`, `ChannelRegistry` |

Each module follows **Clean Architecture** with domain, application, infrastructure, and interface layers.

### Frontend Views

| View | Route | Data Source |
|------|-------|-------------|
| Overview | `/?view=overview` | `GET /dashboard/summary`, `GET /dashboard/health-score` |
| API Inventory | `/?view=inventory` | `GET /ingestion/apis`, `GET /ingestion/apis/stale` |
| Breaking Changes | `/?view=breaking` | `GET /breaking/reports/latest` |
| Policy Compliance | `/?view=policy` | `GET /policies`, `GET /policies/summary` |
| Dependency Graph | `/?view=graph` | `GET /graph/services`, `POST /graph/impact` |
| Notifications | `/?view=notifications` | `GET /notifications`, `GET /notifications/channels` |

---

## Quick Start

### Prerequisites

- **Java 21+** (Eclipse Temurin recommended)
- **Maven 3.9+**
- **Node.js 22+** (for frontend)
- **Docker & Docker Compose** (for full stack)
- **pnpm** (for frontend — `corepack enable && corepack prepare pnpm@latest --activate`)

### Option 1: Docker Compose (Full Stack — Recommended)

```bash
# Start everything (dashboard + server + RabbitMQ)
docker compose up -d

# Open the dashboard
open http://localhost:3000

# Check server health
curl http://localhost:8080/actuator/health

# With PostgreSQL (prod profile)
docker compose --profile prod up -d
```

### Option 2: Local Development (Backend Only)

```bash
# Build and test
cd backend
mvn clean compile
mvn test                          # 228+ tests

# Run with H2 in-memory database
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# In another terminal: ingest a spec
curl -X POST http://localhost:8080/api/v1/ingestion/audit \
  -H "Content-Type: application/json" \
  -d '{
    "repository": "org/example",
    "commitSha": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
    "specPath": "openapi/api.yaml",
    "content": "openapi: \"3.0.0\"\ninfo:\n  title: My API\n  version: \"1.0.0\"\npaths: {}"
  }'
```

### Option 3: Local Development (Full Stack)

```bash
# Terminal 1: Start infrastructure
docker compose up -d rabbitmq

# Terminal 2: Start backend
cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Terminal 3: Start frontend
cd frontend && pnpm install && pnpm dev
```

### Option 4: Standalone Backend Docker

```bash
docker build -t keystone-server ./backend
docker run -p 8080:8080 -e SPRING_PROFILES_ACTIVE=dev keystone-server
```

---

## API Endpoints

### Ingestion

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/v1/ingestion/audit` | Ingest an OpenAPI spec |
| `POST` | `/api/v1/ingestion/webhook/github` | GitHub push webhook receiver |
| `GET` | `/api/v1/ingestion/apis` | List all ingested specs |
| `GET` | `/api/v1/ingestion/apis/stale` | List stale specs |

### Breaking Change Analysis

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/v1/breaking/analyze` | Trigger breaking change analysis |
| `GET` | `/api/v1/breaking/reports/latest` | Latest analysis reports |
| `GET` | `/api/v1/breaking/reports/{reportId}` | Get specific report |
| `POST` | `/api/v1/breaking/reports/{reportId}/reanalyze` | Re-run analysis |

### Policy

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/v1/policies` | List all policies |
| `GET` | `/api/v1/policies/summary` | Policy compliance summary |
| `POST` | `/api/v1/policies` | Create a policy |
| `POST` | `/api/v1/policies/evaluate` | Evaluate a spec against policies |
| `GET` | `/api/v1/policies/sources` | List configured policy sources |

### Dashboard

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/v1/dashboard/summary` | Aggregate dashboard overview |
| `GET` | `/api/v1/dashboard/health-score` | Governance health score |
| `GET` | `/api/v1/dashboard/health/{type}/{id}` | Entity health detail |
| `GET` | `/api/v1/dashboard/health/{type}/{id}/trend` | Health score trend |
| `GET` | `/api/v1/dashboard/compliance-history/{specId}` | Compliance history |
| `GET` | `/api/v1/dashboard/audit-log` | Audit trail |
| `GET` | `/api/v1/dashboard/violation-trends` | Violation trends |

### Graph

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/v1/graph/services` | List all registered services |
| `POST` | `/api/v1/graph/impact` | Compute impact cascade |
| `POST` | `/api/v1/graph/services` | Register a service |

### Notifications

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/v1/notifications` | List notifications |
| `GET` | `/api/v1/notifications/channels` | List notification channels |
| `POST` | `/api/v1/notifications` | Dispatch a notification |

---

## Configuration

### Profiles

| Profile | Database | DDL | Use Case |
|---------|----------|-----|----------|
| `dev` (default) | H2 in-memory | `create-drop` | Local development |
| `prod` | PostgreSQL | `validate` | Production |
| `test` | H2 in-memory | `create-drop` | Automated tests |

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `SPRING_PROFILES_ACTIVE` | `dev` | Active Spring profile |
| `KEYSTONE_DB_URL` | `jdbc:h2:mem:keystone` | Database JDBC URL |
| `KEYSTONE_DB_USERNAME` | `sa` | Database username |
| `KEYSTONE_DB_PASSWORD` | — | Database password |
| `RABBITMQ_PASSWORD` | `keystone` | RabbitMQ password |
| `GITHUB_WEBHOOK_SECRET` | — | GitHub webhook HMAC secret |
| `GITHUB_TOKEN` | — | GitHub API token for spec fetching |

---

## Development

### Build

```bash
# Backend
cd backend && mvn clean compile

# Frontend
cd frontend && pnpm install && pnpm build
```

### Test

```bash
# Backend (228+ tests)
cd backend && mvn test

# Frontend
cd frontend && pnpm test:run

# Code formatting
cd backend && mvn spotless:check
cd backend && mvn spotless:apply  # Auto-fix formatting
```

### Code Style

- **Java:** Palantir Java Format (enforced by Spotless)
- **TypeScript/React:** ESLint + Prettier
- All Java source files must have canonical reference headers:

```java
// Canonical Reference: .pi/architecture/modules/<module>.md#[section]
```

### CI Pipeline

```bash
# Full hardening pipeline
bash .pi/scripts/ci/stage_build.sh
bash .pi/scripts/ci/stage_test.sh
```

The CI pipeline runs automatically on push and pull requests via GitHub Actions (see [`.github/workflows/ci.yml`](.github/workflows/ci.yml)).

---

## Project Structure

```
keystone/
├── backend/                    # Java Spring Boot server
│   ├── src/main/java/com/keystone/
│   │   ├── ingestion/          # Contract Ingestion context
│   │   ├── analysis/           # Breaking Change Analysis context
│   │   ├── policy/             # Policy Engine context
│   │   ├── dashboard/          # Dashboard context
│   │   ├── graph/              # Dependency Graph context
│   │   ├── notification/       # Notification Engine context
│   │   └── infrastructure/     # Cross-cutting config
│   ├── src/main/resources/     # Application config
│   ├── src/test/java/          # Tests
│   └── Dockerfile
├── frontend/                   # Next.js dashboard
│   ├── app/                    # App Router pages
│   ├── components/             # React components by view
│   ├── lib/                    # API client, types, contracts
│   ├── design/                 # Design tokens and specs
│   └── Dockerfile
├── cli/                        # Go CLI client
│   └── cmd/keystone/
├── docs/                       # Documentation
├── .github/workflows/          # CI/CD
├── docker-compose.yml          # Full stack deployment
└── pom.xml                     # Root Maven POM
```

---

## Detectors

Keystone ships with 6 built-in change detectors:

| Detector | Severity | What It Detects |
|----------|----------|-----------------|
| `PathRemovalDetector` | 🔴 BREAKING | API operation removed from spec |
| `FieldRemovalDetector` | 🔴 BREAKING | Response field or parameter removed |
| `FieldTypeChangedDetector` | 🔴 BREAKING | Schema type changed |
| `RequiredFieldAddedDetector` | 🟠 BREAKING | New required field added |
| `OptionalFieldAddedDetector` | 🟢 ADDITIVE | New optional field added |
| `DeprecatedFieldDetector` | 🟡 DEPRECATION | Endpoint marked as deprecated |

---

## Policy DSL

Policies are defined using Keystone's DSL. Example:

```dsl
# No deprecated endpoints
none endpoint in spec.endpoints
  where endpoint.is_deprecated
  yield violation("Deprecated endpoints are not allowed")

# All paths must follow naming convention
each path in spec.paths
  where not path.matches("^/api/v[0-9]+/.*")
  yield violation("Path must start with /api/v{version}/")
```

---

## Frontend Technology

- **Framework:** Next.js 15 (App Router)
- **Language:** TypeScript
- **Styling:** Tailwind CSS 4 + OKLch design tokens
- **Testing:** Vitest + Playwright
- **State:** URL-driven (`?view=` search params)
- **Theme:** Light/dark mode with localStorage persistence

---

## License

[MIT](LICENSE) © 2026 Keystone Contributors

Keystone is released under the MIT License. See [LICENSE](LICENSE) for the full text.

---

## Contributing

We welcome contributions! See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines, [SECURITY.md](SECURITY.md) for responsible disclosure, and [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md) for community standards.

Check out the [gap ledger](backend/docs/backend-gaps.md) for known issues and planned improvements.

**Quick links:**
- [Open an issue](https://github.com/arman-jalili/Keystone/issues/new?template=bug_report.yml)
- [Request a feature](https://github.com/arman-jalili/Keystone/issues/new?template=feature_request.yml)
- [View CI status](https://github.com/arman-jalili/Keystone/actions)

---

<p align="center">
  <sub>Made with ❤️ for the API governance community.</sub>
</p>
