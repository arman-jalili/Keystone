# Contributing to Keystone

Thank you for your interest in contributing to Keystone! We welcome contributions from everyone.

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Workflow](#development-workflow)
- [Architecture](#architecture)
- [Pull Request Guidelines](#pull-request-guidelines)
- [Testing](#testing)
- [Code Style](#code-style)
- [Issue Reporting](#issue-reporting)

## Code of Conduct

This project adheres to the [Contributor Covenant Code of Conduct](CODE_OF_CONDUCT.md). By participating, you are expected to uphold this code.

## Getting Started

1. **Fork the repository** on GitHub
2. **Clone your fork:**
   ```bash
   git clone https://github.com/your-username/Keystone.git
   cd Keystone
   ```
3. **Set up the development environment:**
   - Java 21+ (Eclipse Temurin)
   - Maven 3.9+
   - Node.js 22+ and pnpm (for frontend)
   - Docker & Docker Compose (for full stack)
4. **Build and test:**
   ```bash
   cd backend && mvn clean compile && mvn test
   ```
5. **Create a branch:**
   ```bash
   git checkout -b feat/your-feature-name
   ```

## Development Workflow

### Branch Naming

| Prefix | Purpose |
|--------|---------|
| `feat/*` | New features |
| `fix/*` | Bug fixes |
| `chore/*` | Maintenance, refactoring, dependencies |
| `docs/*` | Documentation changes |

### Commit Messages

We follow conventional commits:

```
<type>: <description>

[optional body]
```

Types: `feat`, `fix`, `chore`, `docs`, `test`, `refactor`, `style`

### Before Submitting

1. Run the full test suite: `cd backend && mvn clean test`
2. Check code formatting: `cd backend && mvn spotless:check`
3. Verify the Docker build: `docker compose build`
4. Update documentation if needed

## Architecture

Keystone follows **Clean Architecture** with bounded contexts:

```
src/main/java/com/keystone/<module>/
├── domain/          # Business rules (models, events, services)
├── application/     # Use cases (DTOs, service interfaces)
├── infrastructure/  # Adapters (repositories, event publishers)
└── interfaces/      # Inbound adapters (HTTP controllers)
```

All Java source files must include a canonical reference header:

```java
// Canonical Reference: .pi/architecture/modules/<module>.md#[section]
```

See [README.md](README.md#architecture) for the full architecture overview.

## Pull Request Guidelines

1. **Keep PRs focused** — one feature/fix per PR
2. **Include tests** — new features should have test coverage
3. **Update the gap ledger** — if fixing a known gap, mark it as `✅ FIXED` in `backend-gaps.md`
4. **Reference issues** — use `Closes #123` in the PR description
5. **Ensure CI passes** — all checks must be green before merging

### PR Checklist

- [ ] Code compiles: `mvn clean compile`
- [ ] Tests pass: `mvn clean test`
- [ ] Formatting is correct: `mvn spotless:check`
- [ ] Docker build succeeds: `docker compose build`
- [ ] Documentation updated (if applicable)
- [ ] Gap ledger updated (if fixing a gap)
- [ ] No new warnings

## Testing

### Backend (Java)

```bash
cd backend

# Run all tests
mvn clean test

# Run a specific test class
mvn test -Dtest=IngestionControllerTest

# Run a specific test method
mvn test -Dtest=IngestionControllerTest#ingestSpec_shouldSucceed

# Check test coverage (JaCoCo)
mvn verify
# → backend/target/site/jacoco/index.html
```

### Frontend (TypeScript)

```bash
cd frontend

# Run unit tests
pnpm test:run

# Run Playwright E2E tests
pnpm test:e2e
```

## Code Style

### Java

- **Formatter:** Palantir Java Format (enforced by Spotless Maven plugin)
- **Auto-fix:** `mvn spotless:apply`
- **Check:** `mvn spotless:check`
- **Null safety:** Use `Objects.requireNonNull()` in constructors
- **Validation:** Jakarta Validation annotations on DTOs
- **Records:** Use Java records for value objects and DTOs

### TypeScript/React

- ESLint configuration in `frontend/.eslintrc`
- Use TypeScript strict mode
- Components as `'use client'` only when interactivity requires it
- Server Components by default

## Issue Reporting

### Bug Reports

When filing a bug report, please include:

1. **Environment:** Java version, OS, Docker version
2. **Steps to reproduce:** Minimal, reproducible steps
3. **Expected behavior:** What should happen
4. **Actual behavior:** What actually happens
5. **Logs:** Relevant server or console output

### Feature Requests

For feature requests, describe:

1. **Use case:** What problem are you trying to solve?
2. **Proposed solution:** How should it work?
3. **Alternatives:** What other approaches have you considered?

### Security Issues

Please **do not** file a public issue for security vulnerabilities. See [SECURITY.md](SECURITY.md) for responsible disclosure.
