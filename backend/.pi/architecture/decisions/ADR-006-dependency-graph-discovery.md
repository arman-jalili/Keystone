# ADR-006: Dependency Graph Discovery Strategy

**Status:** Accepted
**Date:** 2026-06-12
**Session:** 7bff170e-8b01-4621-9de1-4397f096b27a

## Context

The Dependency Graph context tracks which services consume which APIs. However, OpenAPI specs only declare producer contracts (what APIs a service exposes), not consumer relationships (which services call those APIs). Automated discovery would require integration with service mesh, Kubernetes, or traffic analysis — a significant engineering investment.

The question is: how does Keystone discover which services consume which APIs?

## Decision

**v1: Explicit declaration via `keystone.yml`**

Each service repository contains a `keystone.yml` file declaring:
- Which API specs it produces
- Which external service APIs it consumes

```yaml
# keystone.yml (placed in service repository root)
apiVersion: keystone/v1
kind: Service
metadata:
  name: payment-svc
  team: payments
spec:
  produces:
    - spec: ./openapi/checkout.yaml
      version: v2
  consumes:
    - service: user-svc
      spec: openapi/users.yaml
    - service: auth-svc
      spec: openapi/auth.yaml
```

**Discovery flow:**
1. CLI Orchestrator discovers `keystone.yml` alongside the OpenAPI spec at analysis time
2. CLI includes the dependency declaration in the audit upload payload
3. Contract Ingestion forwards it to Dependency Graph context
4. Dependency Graph registers `Service` nodes and `ApiDependency` edges

**v2+: Automated discovery (deferred — not committed)**

| Method | Timeline | Complexity |
|--------|----------|------------|
| Kubernetes label/annotation scraping | Post-v1 | Medium |
| Service mesh telemetry (Istio access logs) | Post-v1 | High |
| OpenTelemetry trace analysis | Post-v1 | High |
| Network traffic analysis | Future | Very high |

## Alternatives Considered

| Alternative | Pros | Cons | Reason Rejected |
|-------------|------|------|-----------------|
| Full auto-discovery from day one | Always up-to-date | 3-6 months additional engineering; high operational complexity | Delays v1 delivery |
| Manual registry (UI form) | No YAML to maintain | Manual; drifts from reality quickly | Worse than explicit declarations in repo |
| Derive from API gateway config | Reflects actual routing | Only works if org uses a single API gateway | Too narrow |

## Consequences

### Positive
- v1 is implementable immediately with no external dependencies
- Explicit declarations are always accurate (intent-based, not inferred)
- Automated discovery can be added later without breaking v1
- `keystone.yml` is co-located with the service (easy to keep in sync)

### Negative
- Requires teams to maintain `keystone.yml` — adoption friction and onboarding cost
- Declarations can become stale if not updated when dependencies change
- No automatic detection of unknown/rogue consumers

### Neutral
- `keystone.yml` can be validated in CI (schema check)
- Missing `keystone.yml` = service is not tracked in dependency graph (graceful degradation)

## Implementation

**Affected Modules:**
- `.pi/architecture/modules/dependency-graph.md`
- `.pi/architecture/modules/cli-orchestrator.md` (discovers and forwards keystone.yml)
- `.pi/architecture/modules/contract-ingestion.md` (routes declaration to Dependency Graph)

```typescript
interface DependencyDeclaration {
  service: {
    name: string;
    team?: string;
  };
  produces: SpecProduced[];
  consumes: SpecConsumed[];
}

interface SpecProduced {
  spec: string;       // path within repo
  version: string;
}

interface SpecConsumed {
  service: string;     // name of producer service
  spec?: string;       // optional — if not specified, all specs from that service
}
```

## Validation

**Validators Required:**
- architecture-validator: Verify Dependency Graph module does not depend on external discovery services
- test-validator: Verify graph query performance with 500+ services and 2000+ edges

## References

- Related ADRs: ADR-002 (CLI Orchestrator discovers keystone.yml)
- `.pi/architecture/modules/dependency-graph.md`
- `.pi/architecture/diagrams/data-model.md` (ApiDependency entity)

---

*Decision date: 2026-06-12*
