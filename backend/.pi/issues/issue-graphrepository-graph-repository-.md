---
guardian_issue:
  id: "ISSUE-DEPENDENCY-GRAPH-2"
  epic: "TBD"
  component: "GraphRepository {#graph-repository}"
  module: "dependency-graph"
  status: planned
  priority: high
  dependencies:
    - "none"

  in_scope:
    - Implement GraphRepository {#graph-repository} for the dependency-graph module
    - Write unit tests for all public interfaces
    - Add integration tests with upstream/downstream components
    - Create API documentation

  out_of_scope:
    - Changes to upstream components (none)
    - UI/frontend changes
    - Deployment pipeline configuration

  affected_layers:
    domain:
      - New domain models for graphrepository-{#graph-repository}
    application:
      - New service/handler for graphrepository-{#graph-repository}
    infrastructure:
      - New database tables or external service connections
    api:
      - New endpoints or event handlers

  canonical_references:
    - module: ".pi/architecture/modules/dependency-graph.md#graphrepository-{#graph-repository}"

  acceptance_criteria:
    - "CI pipeline passes (validate-ci.sh)"
    - "All unit tests pass with ≥ 90% coverage"
    - "Integration tests pass with upstream/downstream components"
    - "validate-security.sh passes"
    - "validate-architecture.sh passes"
    - "validate-canonical.sh passes"

  validators:
    - ci
    - tests
    - security
    - architecture
    - canonical

  implementation_notes: |
    JPA data access for Service nodes and ApiDependency edges.

  file_changes:
    - "create: src/dependency-graph/graphrepository-{#graph-repository}/"
    - "create: tests/unit/dependency-graph/graphrepository-{#graph-repository}/"
    - "create: tests/integration/dependency-graph/graphrepository-{#graph-repository}/"
---

# ISSUE-DEPENDENCY-GRAPH-2: GraphRepository {#graph-repository}

## Intent

JPA data access for Service nodes and ApiDependency edges.

## Architecture Context

- **Module:** dependency-graph
- **Component:** GraphRepository {#graph-repository}
- **Status:** planned
- **Dependencies:** none

## Dependencies

```
  └── none
```

## In Scope

- Implement GraphRepository {#graph-repository} for the dependency-graph module
- Write unit tests for all public interfaces
- Add integration tests with upstream/downstream components
- Create API documentation

## Out of Scope

- Changes to upstream components
- UI/frontend changes
- Deployment pipeline configuration

## Affected Layers

### Domain
- New domain models for graphrepository-{#graph-repository}

### Application
- New service/handler for graphrepository-{#graph-repository}

### Infrastructure
- New database tables or external service connections

### API
- New endpoints or event handlers

## Canonical References

- **Module:** `.pi/architecture/modules/dependency-graph.md#graphrepository-{#graph-repository}`

## Acceptance Criteria

| # | Criterion | Validator |
|---|-----------|-----------|
| 1 | CI pipeline passes | `validate-ci.sh` |
| 2 | All unit tests pass with ≥ 90% coverage | `validate-tests.sh` |
| 3 | Integration tests pass | `validate-integration.sh` |
| 4 | Security checks pass | `validate-security.sh` |
| 5 | Architecture compliance | `validate-architecture.sh` |
| 6 | Canonical references valid | `validate-canonical.sh` |

## Implementation

> **Agent:** This is your complete session context. All information you need is above.
> Start by reading the canonical reference files, then implement following the layer structure.

### Steps

1. Read canonical architecture references
2. Create domain entities and interfaces
3. Implement application service/handler
4. Add infrastructure connections
5. Write unit tests (≥ 90% coverage)
6. Write integration tests
7. Run all validators
8. Create MR
