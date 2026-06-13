# Keystone

**OpenAPI Specification Governance Platform**

Keystone is a governance platform for OpenAPI specifications — it ingests, analyzes, and enforces policy on API specs across your organization. Provides a dashboard for visualizing governance health, compliance trends, and policy violations.

## Architecture

Keystone follows Clean Architecture with the following bounded contexts:

| Module | Package | Purpose |
|--------|---------|---------|
| **Contract Ingestion** | `com.keystone.ingestion` | OpenAPI spec ingestion, deduplication, storage |
| **Breaking Change Analysis** | `com.keystone.analysis` | Diff-based breaking change detection |
| **Policy Engine** | `com.keystone.policy` | Policy definition, evaluation, sync, DSL |
| **Dashboard** | `com.keystone.dashboard` | Health scores, policy UI, compliance views |
| **Dependency Graph** | `com.keystone.graph` | Service dependency mapping and impact analysis |
| **Notification Engine** | `com.keystone.notification` | Multi-channel notification dispatch |

### Clean Architecture Layers

Every module follows the same layer structure:

```
src/main/java/com/keystone/<module>/
├── domain/          # Enterprise business rules
│   ├── model/       # Domain models and value objects
│   ├── event/       # Domain events
│   ├── exception/   # Domain exceptions
│   └── service/     # Domain service interfaces
├── application/     # Application use cases
│   ├── dto/         # Input/output DTOs with validation
│   └── service/     # Application service interfaces
├── infrastructure/  # External adapters
│   ├── repository/  # Repository interfaces & implementations
│   ├── event/       # Event publisher interfaces & implementations
│   └── config/      # Configuration and health indicators
└── interfaces/      # Inbound adapters
    └── http/        # REST controllers and exception handlers
```

## Tech Stack

- **Language:** Java 21
- **Framework:** Spring Boot 3.2.0
- **Database:** PostgreSQL 16 (H2 for dev/test)
- **Build:** Maven (multi-module)
- **Messaging:** RabbitMQ / AMQP
- **Tracing:** Micrometer + Brave
- **Validation:** Jakarta Validation
- **Formatting:** Palantir Java Format (via Spotless)
- **Deployment:** Docker / Docker Compose

## Quick Start

### Option 1: Local Development (H2 — no dependencies)

```bash
# Prerequisites
java -version    # JDK 21+
mvn -version     # Maven 3.9+

# Build & test
cd backend
mvn clean compile
mvn test

# Run with H2 in-memory database (zero config)
mvn spring-boot:run -Dspring-boot.run.profiles=dev
# → http://localhost:8080
# → H2 console: http://localhost:8080/h2-console (JDBC URL: jdbc:h2:mem:keystone)
```

### Option 2: Docker Compose (Dev — H2 + RabbitMQ)

```bash
docker compose up
# → Keystone: http://localhost:8080
# → RabbitMQ: http://localhost:15672 (keystone/keystone)
```

### Option 3: Docker Compose (Prod — PostgreSQL + RabbitMQ)

```bash
docker compose --profile prod up
# → Keystone: http://localhost:8080
# → PostgreSQL: localhost:5432
# → RabbitMQ: http://localhost:15672
```

### Option 4: Docker (standalone backend)

```bash
docker build -t keystone-server ./backend
docker run -p 8080:8080 -e SPRING_PROFILES_ACTIVE=dev keystone-server
```

## Profiles

| Profile | Database | DDL | Use Case |
|---------|----------|-----|----------|
| `dev` (default) | H2 in-memory | `create-drop` | Local development |
| `prod` | PostgreSQL | `validate` | Production |
| `test` | H2 in-memory | `create-drop` | Automated tests |

Activate via `SPRING_PROFILES_ACTIVE=prod` env var or `--spring-boot.run.profiles=dev`.

## Development

### Branch Naming

- `feat/<issue-id>` — Feature branches
- `fix/<issue-id>` — Bug fixes
- `chore/<issue-id>` — Maintenance

### Code Style

Formatting is enforced by Spotless (Palantir style):

```bash
mvn spotless:apply   # Format all code
mvn spotless:check   # Verify formatting
```

### Architecture Validation

Canonical reference headers must be present in all Java source files:

```java
// Canonical Reference: .pi/architecture/modules/<module>.md#[section]
// Implements: [description]
```

### CI Validators

```bash
# Build + Test + Format + Security
bash .pi/scripts/languages/java/validate-ci.sh

# Canonical references
bash .pi/scripts/languages/java/validate-canonical.sh
```

## Project Structure

```
keystone/
├── backend/              # Java Spring Boot server
│   ├── src/main/java/    # Production code
│   ├── src/test/java/    # Test code
│   └── pom.xml           # Maven build
├── cli/                  # Go CLI client
├── .pi/                  # Pipeline and agent configuration
├── .gitnexus/            # Code intelligence index
└── AGENTS.md             # Agent operating guidelines
```

## License

Internal — Keystone Governance Platform
