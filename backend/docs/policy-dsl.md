# Policy DSL Format (v1.0)

> **Status:** Frozen Contract
> **Last Updated:** 2026-06-12
> **Component:** Policy DSL Format {#policy-dsl}

## Overview

The Policy DSL (Domain-Specific Language) is a YAML-based format for defining
OpenAPI governance policies. Policies are stored as `.policy` files in a
designated directory (e.g., `.keystone/policies/`) within a Git repository.

Each `.policy` file can contain one or more policy rules. The DSL is designed
to be human-readable, version-controllable, and easy to review in pull requests.

---

## File Format

### Single Policy File

```yaml
# .keystone/policies/require-https.policy
name: require-https
description: All API endpoints must use HTTPS
severity: CRITICAL
scope:
  pathPatterns:
    - "/**"
  operations:
    - GET
    - POST
    - PUT
    - PATCH
    - DELETE
  excludePaths:
    - "/health"
    - "/metrics"
rule: |
  each endpoint in spec.paths
  where endpoint.protocol != "https"
  yield violation("Endpoint must use HTTPS: {endpoint.path}")
```

### Multi-Policy File

```yaml
# .keystone/policies/naming-conventions.policy
version: "1.0"
policies:
  - name: snake_case_paths
    description: All API paths must use snake_case format
    severity: MINOR
    scope:
      pathPatterns:
        - "/api/**"
    rule: |
      each path in spec.paths
      where not path.matches("^/[a-z0-9_/{}]+$")
      yield violation("Path must use snake_case: {path}")

  - name: versioned_api_paths
    description: All API paths must include a version prefix
    severity: MAJOR
    scope:
      pathPatterns:
        - "/**"
      excludePaths:
        - "/health"
        - "/metrics"
        - "/favicon.ico"
    rule: |
      each path in spec.paths
      where not path.matches("^/api/v[0-9]+/")
      yield violation("Path must include version prefix (e.g. /api/v1/): {path}")
```

---

## Policy Fields

| Field | Required | Type | Description |
|-------|----------|------|-------------|
| `name` | Yes | String | Unique policy identifier. Lowercase alphanumeric with hyphens. |
| `description` | No | String | Human-readable description of the policy's purpose. |
| `severity` | No | Enum | `CRITICAL`, `MAJOR`, `MINOR`, or `INFO`. Default: `MAJOR`. |
| `scope` | No | Object | Scope constraints (see below). Default: applies to all spec elements. |
| `rule` | Yes | String | Policy DSL expression defining the rule logic. |
| `version` | No | String | File format version (`"1.0"`). Default: `"1.0"`. |
| `policies` | No | Array | Used only in multi-policy files to define multiple rules. |

### Scope Fields

| Field | Required | Type | Description |
|-------|----------|------|-------------|
| `pathPatterns` | No | String[] | Ant-style path patterns the policy applies to. Default: `["/**"]`. |
| `operations` | No | String[] | HTTP methods to target (`GET`, `POST`, `PUT`, `PATCH`, `DELETE`, `HEAD`, `OPTIONS`). Empty = all methods. |
| `tags` | No | String[] | OpenAPI operation tags to target. Empty = all tags. |
| `excludePaths` | No | String[] | Path patterns to exclude from evaluation. |

---

## Rule Expression Language

The rule expression is a simple declarative language for expressing policy
conditions over an OpenAPI specification's parsed structure.

### Grammar

```
rule     = quantifier "in" source "where" condition "yield" action
         | quantifier "in" source "yield" action

quantifier = "each" | "any" | "none"

source   = "spec.paths" | "spec.schemas" | "spec.operations"
         | "spec.endpoints"

condition = comparison
          | unary_expr
          | condition "and" condition
          | condition "or" condition
          | "not" condition
          | "(" condition ")"

comparison = identifier operator value
identifier = "path" | "endpoint.protocol" | "endpoint.method"
           | "endpoint.operationId" | "field.name" | "field.type"
           | "schema.name" | "response.status"
           | spec_query_path

operator  = "!=" | "==" | "matches" | "contains" | "has"
value     = string_literal | number | "true" | "false" | "null"

unary_expr = "is_defined"
           | "is_deprecated"
           | "is_read_only"

action    = "violation(" message ")" | "pass()"
message   = string_literal (with optional "{expression}" interpolation)
```

### Quantifiers

| Quantifier | Behavior |
|------------|----------|
| `each`     | Iterates all matching elements and yields violations for each match |
| `any`      | Yields a single violation if any element matches |
| `none`     | Yields a violation if NO elements match (inverted check) |

### Built-in Functions

| Function | Description |
|----------|-------------|
| `matches(pattern)` | Tests if a value matches the given regex pattern |
| `contains(value)`  | Tests if a collection or string contains the given value |
| `has(field)`       | Tests if an object has the specified field/property |

### Examples

```yaml
# All paths must have an operationId
rule: |
  each endpoint in spec.endpoints
  where not endpoint.has("operationId")
  yield violation("Endpoint must have operationId: {endpoint.path} {endpoint.method}")

# No deprecated fields allowed
rule: |
  none field in spec.schemas
  where field.is_deprecated
  yield violation("Deprecated fields are not allowed: {field.name}")

# Response must include 200 status
rule: |
  each endpoint in spec.endpoints
  where not endpoint.responses.has("200")
  yield violation("Endpoint must define 200 response: {endpoint.path}")
```

---

## Evaluation Semantics

1. **Scope Filtering:** Before evaluation, the policy's `scope` is matched
   against the spec's paths, operations, and tags. Only matching elements
   are passed to the rule expression.

2. **Rule Evaluation:** The rule expression is evaluated against each
   matching spec element. The quantifier (`each`/`any`/`none`) determines
   how violations are collected.

3. **Severity Assignment:** The policy's `severity` field is applied to
   all violations produced by the rule.

4. **Verdict Computation:** The overall evaluation verdict is:
   - `FAIL` — if any CRITICAL or MAJOR violations are found
   - `WARNING` — if only MINOR or INFO violations are found
   - `PASS` — if no violations are found

---

## Policy Source Directory Structure

Policies are organized within a repository's designated policy directory:

```
.keystone/
  policies/
    breaking/              # Breaking change policies
      no-field-removal.policy
      no-path-removal.policy
    naming/                # Naming convention policies
      snake-case-paths.policy
      camelCase-properties.policy
    security/              # Security policies
      require-https.policy
      no-api-keys-in-path.policy
      require-auth.policy
    standards/             # Organizational standards
      versioned-paths.policy
      error-response-format.policy
```

Each subdirectory may represent a policy category, which maps to a
PolicySet in the system.

---

## Error Handling

If a `.policy` file contains syntax errors, the parser should:

1. Report the file path, line number, and column of the error
2. Continue parsing remaining files (partial success)
3. Collect all errors and return them in the sync response

Parse errors are surfaced via the
[PolicyParseException](../src/main/java/com/keystone/policy/domain/exception/PolicyParseException.java)
domain exception.
