# Ubiquitous Language

> Canonical glossary for **Keystone**.
> All code MUST use these terms. Aliases/synonyms listed below are **prohibited** in source identifiers.
> Drift is detected by `.pi/scripts/validate-ubiquitous-language.sh`.

## Glossary

| Term | Definition | Bounded Context | Aliases/Synonyms | Examples |
|------|-----------|----------------|-----------------|---------|
| OpenApiSpec | A versioned OpenAPI specification document with its parsed model, metadata, and source provenance | Contract Ingestion | API contract, spec file, OAS document | `OpenApiSpec.ingest(rawYaml, source="github.com/org/repo/blob/{sha}/spec.yaml")` |
| SpecVersion | An immutable snapshot of an OpenApiSpec at a specific point in time, identified by commit SHA or version string | Contract Ingestion | spec snapshot, versioned spec | `SpecVersion(commitSha="a1b2c3d", specId=openApiSpec.id)` |
| ParsedEndpoint | A single parsed API endpoint (path, method, request/response schemas, parameters) extracted from an OpenApiSpec | Contract Ingestion | endpoint, route, API operation | `ParsedEndpoint(path="/users/{id}", method="GET", ...)` |
| BreakingChangeReport | The output of a diff analysis containing a list of Changes, overall verdict, and metadata referencing base and target SpecVersions | Breaking Change Analysis | diff report, change report, breaking change report | `BreakingChangeReport(baseVersion, targetVersion, changes=[...], verdict="breaking")` |
| Change | A single detected difference between two spec versions, classified by type and ChangeSeverity | Breaking Change Analysis | diff, delta, change entry | `Change(severity=BREAKING, description="Response property 'email' removed from GET /users")` |
| ChangeSeverity | A classification of a change: breaking (removes/alters existing contract), non-breaking (additive only), additive (adds new contract), or deprecation (marks existing as deprecated) | Breaking Change Analysis | severity level, change type | `ChangeSeverity.BREAKING`, `ChangeSeverity.NON_BREAKING`, `ChangeSeverity.ADDITIVE`, `ChangeSeverity.DEPRECATION` |
| Policy | A governance entity containing a set of PolicyRules, target selectors (which specs/services it applies to), and a lifecycle state (active/inactive/archived) | Policy Engine | governance policy, compliance rule set | `Policy(name="Strict-Contract-Backward-Compat", rules=[...], targets=["core-*"], state=ACTIVE)` |
| PolicyRule | A single evaluable rule within a Policy; defines a condition expression, an action (block/warn/info), and a human-readable message template | Policy Engine | rule, condition, policy clause | `PolicyRule(condition="change.severity == BREAKING && !policy.hasExemption(change)", action=BLOCK, message="Breaking change without exemption: {{change.description}}")` |
| ComplianceResult | The verdict produced by evaluating a BreakingChangeReport against one or more Policies; contains pass/fail/warn status and list of violations | Policy Engine | compliance verdict, evaluation result | `ComplianceResult(verdict=FAIL, policyId="policy-123", violations=[...], evaluatedAt=2026-06-12T10:00:00Z)` |
| Exemption | A time-bound or permanent permission to bypass a PolicyRule for a specific Change, approved by a Compliance Manager | Policy Engine | waiver, exception, skip | `Exemption(changeId="change-456", policyId="policy-123", expiresAt=2026-07-12T00:00:00Z, grantedBy="compliance@org")` |
| Notification | A pending or sent notification with a specific channel type, payload, delivery status, and retry metadata | Notification Engine | alert, message, notification event | `Notification(channel=CI_STATUS, payload=CiStatusUpdate(...), status=DELIVERED)` |
| CiStatusUpdate | A structured status update for the GitHub/GitLab commit status API with state (pending/success/failure/error), description, and target URL | Notification Engine | CI status, commit status, pipeline status | `CiStatusUpdate(commitSha="a1b2c3d", state=FAILURE, description="Breaking change detected", targetUrl="https://keystone.orbiting.ch/reports/789")` |
| ApiDependency | A directed edge between two Services indicating that one consumes an API produced by the other | Dependency Graph | API dependency, dependency edge, service link | `ApiDependency(consumer="payment-svc", producer="user-svc", specId="spec-123", discoveredAt=2026-06-12T10:00:00Z)` |
| Service | A registered service in the Dependency Graph with metadata including name, owning team, and associated API endpoints | Dependency Graph | microservice, application, consumer | `Service(name="payment-svc", team="payments", endpoints=[...])` |
| ImpactAnalysisResult | A value object listing all downstream Services affected by a breaking change, with estimated impact severity per service | Dependency Graph | blast radius, impact report, downstream effect | `ImpactAnalysisResult(changeId="change-456", affectedServices=[{service: "payment-svc", impact: HIGH}, ...])` |
| GovernanceHealthScore | An aggregate metric computed from spec compliance rates, policy pass rates, and exemption counts over a configurable time window | Dashboard | compliance score, governance metric, health metric | `GovernanceHealthScore(score=0.92, period=LAST_30_DAYS, specCount=150, passRate=0.95, exemptionRate=0.03)` |
| AuditEntry | An immutable, append-only record of a governance action (spec ingestion, policy change, exemption grant, CI status update) | Dashboard | audit log, audit record, governance event | `AuditEntry(action="EXEMPTION_GRANTED", actor="compliance@org", target="policy-123", timestamp=2026-06-12T10:00:00Z, payload={...})` |
| LocalAnalysisRequest | A request to analyze a spec change locally within the CI runner; includes current spec content and optional base ref | CLI Orchestrator | CLI request, local analysis job | `LocalAnalysisRequest(specContent, baseRef="origin/main", repo="org/repo", commitSha="a1b2c3d")` |
| CachedSpec | A locally cached copy of a previously analyzed spec version, keyed by checksum or commit SHA, to enable fast offline diffs | CLI Orchestrator | cached version, spec cache, local snapshot | `CachedSpec(key="sha256:abc123", specId="spec-456", parsedEndpoints=[...])` |
| LocalDiffResult | A preliminary diff result produced by local CLI analysis before server-side policy evaluation | CLI Orchestrator | local result, preliminary report, fast diff | `LocalDiffResult(verdict="breaking", changes=[...], analysisTimeMs=42)` |

## Adding New Terms

1. Identify the term used in conversation and code
2. Add a row to the Glossary table
3. Define the term's **bounded context** (which module it lives in)
4. List any **aliases/synonyms** that agents might mistakenly use
5. Provide **code examples** showing correct usage
6. Run `.pi/scripts/validate-ubiquitous-language.sh` to detect drift

> **Rule of thumb:** If two agents use different names for the same concept, add an entry.
> The canonical term is the one used in the architecture module documents.
