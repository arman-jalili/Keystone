# Ubiquitous Language

> Canonical glossary for **Keystone**.
> All code MUST use these terms. Aliases/synonyms listed below are **prohibited** in source identifiers.
> Drift is detected by `.pi/scripts/validate-ubiquitous-language.sh`.

## Glossary

| Term | Definition | Bounded Context | Aliases/Synonyms | Examples |
|------|-----------|----------------|-----------------|---------|
| OpenApiSpec | An ingested OpenAPI 3.x specification file identified by repository and path | Contract Ingestion | Spec, API spec, OpenAPI document | `OpenApiSpec` class in domain model |
| SpecVersion | A snapshot of a spec at a specific commit, with raw content and checksum | Contract Ingestion | Spec version, version snapshot | `SpecVersion` class; version tracking |
| IdempotencyKey | Deduplication key (repo + commitSha + specPath) to prevent duplicate ingestion | Contract Ingestion | Dedup key, idempotency token | `IdempotencyKey` class; repository + commitSha + specPath tuple |
| BreakingChangeReport | Diff analysis result comparing two spec versions, containing all changes and a verdict | Breaking Change Analysis | Diff report, analysis report, breaking report | `BreakingChangeReport` aggregate root; `POST /api/v1/breaking/reports/{id}` |
| Change | A single detected difference between spec versions with severity classification | Breaking Change Analysis | Diff, finding, delta | `Change` record; field removal, type change, path removal |
| ChangeSeverity | Severity: BREAKING, NON_BREAKING, ADDITIVE, DEPRECATION | Breaking Change Analysis | Severity level, change kind | `ChangeSeverity.BREAKING` |
| Verdict | Overall analysis outcome: PASS, BREAKING, NON_BREAKING, INCONCLUSIVE | Breaking Change Analysis / Policy Engine | Result, outcome, conclusion | `Verdict.PASS`, `Verdict.BREAKING` |
| Policy | An immutable policy rule with name, severity, scope, and DSL expression | Policy Engine | Rule, policy rule, governance rule | `Policy` record; name + severity + dslExpression |
| PolicySet | A named, versioned collection of policies evaluated together | Policy Engine | Policy collection, rule set, policy group | `PolicySet` aggregate root; "breaking-change-rules" |
| PolicyEvaluationResult | Complete evaluation outcome for a spec against a policy set | Policy Engine | Evaluation result, compliance result | `PolicyEvaluationResult` aggregate root; violations + verdict |
| Violation | A single policy violation with severity, message, spec path, and optional fix suggestion | Policy Engine | Finding, issue, non-compliance | `Violation` record; policyId + severity + message + specPath |
| Exemption | A time-bound grant bypassing a specific policy violation | Policy Engine | Waiver, exception, policy override | `Exemption` record; policyId + expiresAt + grantedBy |
| PolicyScope | Target scope for a policy: path patterns, HTTP operations, tags, exclusions | Policy Engine | Rule scope, applicability, target | `PolicyScope` record; pathPatterns + operations + tags |
| Service | A registered microservice or application in the dependency graph | Dependency Graph | Application, service node, producer/consumer | `Service` aggregate root; name + team + metadata |
| ApiDependency | A directed edge between a consumer and a producer service for a spec path | Dependency Graph | Dependency edge, relationship, link | `ApiDependency` entity; producerId + consumerId + specPath |
| ImpactAnalysisResult | BFS result listing all downstream services affected by a breaking change | Dependency Graph | Blast radius, impact assessment, downstream impact | `ImpactAnalysisResult` value object; affectedServices list |
| DashboardSummary | Aggregate governance overview with overall health and per-repo summaries | Dashboard | Dashboard overview, governance summary | `DashboardSummary` record; overallScore + repositories |
| GovernanceHealthScore | Composite health score (0-1) from compliance, stability, freshness, coverage | Dashboard | Health score, governance score, overall health | `GovernanceHealthScore` record; score + period + sub-rates |
| ComplianceSummary | Compliance snapshot for a single spec: last evaluated, rate, violation count | Dashboard | Spec compliance, compliance status | `ComplianceSummary` record; specId + complianceRate |
| ViolationTrend | Time-series data showing violation counts by severity over a date range | Dashboard | Trend data, violation history | `ViolationTrend` record; date + violationCount + severity |
| AuditEntry | An append-only log entry recording a governance action (who, what, when) | Dashboard | Audit log entry, governance event, action record | `AuditEntry` record; id + action + actor + target + timestamp |
| PolicyBreakdown | Policy counts grouped by lifecycle status and severity level | Dashboard | Policy distribution, policy stats | `PolicyBreakdown` record; byStatus + bySeverity |
| Notification | A single delivery attempt through a specific channel with delivery status | Notification Engine | Alert, message, delivery record | `Notification` record; channelName + status + payloadType |
| NotificationStatus | Delivery status: PENDING, DELIVERED, FAILED, RETRYING | Notification Engine | Delivery status, status | `NotificationStatus.DELIVERED` |
| Policy DSL | Domain-specific language for defining policy rules evaluated against spec elements | Policy Engine | Policy language, rule DSL, policy expression | DSL expression in `Policy.dslExpression` |
| HealthScore | A time-series health score record for a specific entity with sub-scores | Dashboard | Entity health, sub-score breakdown | `HealthScore` entity; entityType + entityId + score + compliance |
| ChangeDetector | Pluggable detector that identifies a specific type of API change | Breaking Change Analysis | Detector, change finder | `FieldRemovalDetector`, `PathRemovalDetector` implementations |
| SpecValidator | Validates that an incoming spec is a well-formed OpenAPI document | Contract Ingestion | Spec parser, schema validator | `SpecValidator` interface; parse + validate |
| ChannelRegistry | Registry of available notification delivery channels | Notification Engine | Channel manager, delivery registry | `ChannelRegistry` interface; register + getChannel |
| EvaluationEngine | Engine that evaluates a spec against a set of policies using the DSL interpreter | Policy Engine | Policy evaluator, compliance engine | `EvaluationEngine` interface; evaluate(spec, policySet) |
| ImpactAnalyzer | Service that traverses the dependency graph via BFS to find affected downstream services | Dependency Graph | Graph analyzer, blast radius calculator | `ImpactAnalyzer` interface; analyze(reportId, specPath) |
| PolicySource | Configuration for an external Git repository that contains policy definitions | Policy Engine | Policy repo, external source, upstream policy store | `GitPolicySource` implementation; repository URL + branch |

## Adding New Terms

1. Identify the term used in conversation and code
2. Add a row to the Glossary table
3. Define the term's **bounded context** (which module it lives in)
4. List any **aliases/synonyms** that agents might mistakenly use
5. Provide **code examples** showing correct usage
6. Run `.pi/scripts/validate-ubiquitous-language.sh` to detect drift

> **Rule of thumb:** If two agents use different names for the same concept, add an entry.
> The canonical term is the one used in the architecture module documents.
