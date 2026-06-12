# ADR-005: Policy DSL Format and Authoring

**Status:** Accepted
**Date:** 2026-06-12
**Session:** 7bff170e-8b01-4621-9de1-4397f096b27a

## Context

Compliance managers need to define governance policies, but enterprises also demand version-controlled, GitOps-compatible policy management. The domain exploration identified this as open question #1: "Policy DSL or UI-first?"

## Decision

**Dual-mode policy authoring with DSL as source of truth:**

**1. DSL-first (source of truth):** Policies are defined in YAML files (`keystone-policy.yaml`) stored in a dedicated git repository. This enables PR-based policy review, version history, and audit trails.

**2. UI-as-committer:** The Dashboard UI can edit policies, but instead of updating the database directly, it commits changes to the git repository via GitHub API. The server then syncs from the repository.

**Architecture:**
- `PolicySource` interface (pluggable): file system, HTTP(s), git, OCI registry
- `PolicyRepository` syncs policies from the configured source into the local database
- Policy hot-reload (NFR-007): `PolicyRepository` polls for changes or receives webhook notifications
- Validation occurs at two points: (1) when committing to git (CI pipeline validates DSL), (2) when loading into Policy Engine

**Policy DSL format:**
```yaml
apiVersion: keystone/v1
kind: Policy
metadata:
  name: no-breaking-changes-prod
  environment: prod
spec:
  rules:
    - name: block-breaking-changes
      condition: "change.severity == BREAKING && !policy.hasExemption(change)"
      action: block
      message: "Breaking changes require an exemption in production"
    - name: warn-deprecations
      condition: "change.severity == DEPRECATION"
      action: warn
      message: "Endpoint {{change.path}} is deprecated"
  targets:
    - "core-*"
    - "payment-*"
  exemptions:
    - changeId: "change-456"
      expiresAt: "2026-07-12T00:00:00Z"
      grantedBy: "compliance@org"
```

**Environment model:**
- Each Policy has an `environment` label (`dev`, `staging`, `prod`, or `*` for all)
- The CI call includes the target environment (e.g., from pipeline context variable)
- Policy Engine evaluates only policies matching the current environment

## Alternatives Considered

| Alternative | Pros | Cons | Reason Rejected |
|-------------|------|------|-----------------|
| UI-only policy editor | Easier for non-technical compliance managers | No version control, no GitOps, no audit trail | Enterprises demand GitOps |
| DSL-only (UI reads only) | Clean GitOps model | Compliance managers must learn YAML; higher friction | Adopted but with UI-as-committer as middle ground |
| Database as source of truth | Fast updates | No version history; difficult to review policy changes | Git is better for audit |

## Consequences

### Positive
- GitOps-compatible — policies are version-controlled and reviewed
- Compliance managers get a UI without sacrificing GitOps
- Policy history is automatically preserved in git
- Hot-reload is natural (poll git or receive webhook)

### Negative
- UI commits to git require GitHub API token with write access
- Merge conflicts possible if two managers edit via UI simultaneously
- Git sync latency (seconds to minutes) before policies take effect

## Implementation

**Affected Modules:**
- `.pi/architecture/modules/policy-engine.md`
- `.pi/architecture/modules/dashboard.md`

**PolicySource Interface:**
```typescript
interface PolicySource {
  listPolicies(): Promise<PolicySpec[]>;
  getPolicy(name: string): Promise<PolicySpec | null>;
  watch(callback: (change: PolicyChange) => void): void;
}

class GitPolicySource implements PolicySource {
  constructor(private repoUrl: string, private branch: string) {}
  // Pulls from git, parses YAML files matching keystone-policy*.yaml
}

class HttpPolicySource implements PolicySource {
  constructor(private endpoint: string) {}
  // Fetches from HTTP(s) URL, e.g., enterprise policy registry
}
```

## Validation

**Validators Required:**
- security-validator: Verify policy DSL parser rejects malicious inputs; verify RBAC on policy mutations
- test-validator: Verify parser edge cases (empty rules, circular conditions, invalid YAML)

## References

- Related ADRs: ADR-003 (Event-Driven Communication — PolicyEvaluated event)
- `.pi/architecture/modules/policy-engine.md`

---

*Decision date: 2026-06-12*
