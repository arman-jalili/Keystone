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
- **Database:** PostgreSQL 16 (H2 for tests)
- **Build:** Maven
- **Messaging:** RabbitMQ / AMQP
- **Tracing:** Micrometer + Brave
- **Validation:** Jakarta Validation
- **Formatting:** Palantir Java Format (via Spotless)

## Quick Start

```bash
# Prerequisites
java -version    # JDK 21+
mvn -version     # Maven 3.9+

# Build
cd backend
mvn clean compile

# Run tests
mvn test

# Run (requires PostgreSQL on localhost:5432)
mvn spring-boot:run
```

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
