// Canonical Reference: .pi/architecture/modules/policy-engine.md
// Module: policy-engine
package com.keystone.policy.domain.model;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Aggregate root representing the complete result of evaluating policies
 * against an OpenAPI specification.
 *
 * <p>Produced by {@link com.keystone.policy.domain.service.EvaluationEngine} and
 * published as a domain event for downstream consumers (e.g. breaking change analysis).
 *
 * <p>Results are immutable once created.
 */
public class PolicyEvaluationResult {

    private final UUID id;
    private final UUID specId;
    private final UUID policySetId;
    private final String repository;
    private final String specPath;
    private final String commitSha;
    private final Verdict verdict;
    private final List<Violation> violations;
    private final int totalPoliciesChecked;
    private final int passedCount;
    private final int failedCount;
    private final Instant evaluatedAt;

    public PolicyEvaluationResult(
            UUID id,
            UUID specId,
            UUID policySetId,
            String repository,
            String specPath,
            String commitSha,
            Verdict verdict,
            List<Violation> violations,
            int totalPoliciesChecked,
            int passedCount,
            int failedCount,
            Instant evaluatedAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.specId = Objects.requireNonNull(specId, "specId must not be null");
        this.policySetId = Objects.requireNonNull(policySetId, "policySetId must not be null");
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.specPath = Objects.requireNonNull(specPath, "specPath must not be null");
        this.commitSha = commitSha;
        this.verdict = Objects.requireNonNull(verdict, "verdict must not be null");
        this.violations = List.copyOf(Objects.requireNonNull(violations, "violations must not be null"));
        this.totalPoliciesChecked = totalPoliciesChecked;
        this.passedCount = passedCount;
        this.failedCount = failedCount;
        this.evaluatedAt = Objects.requireNonNull(evaluatedAt, "evaluatedAt must not be null");
    }

    public UUID getId() {
        return id;
    }

    public UUID getSpecId() {
        return specId;
    }

    public UUID getPolicySetId() {
        return policySetId;
    }

    public String getRepository() {
        return repository;
    }

    public String getSpecPath() {
        return specPath;
    }

    public String getCommitSha() {
        return commitSha;
    }

    public Verdict getVerdict() {
        return verdict;
    }

    public List<Violation> getViolations() {
        return Collections.unmodifiableList(violations);
    }

    public int getTotalPoliciesChecked() {
        return totalPoliciesChecked;
    }

    public int getPassedCount() {
        return passedCount;
    }

    public int getFailedCount() {
        return failedCount;
    }

    public Instant getEvaluatedAt() {
        return evaluatedAt;
    }

    public boolean hasViolations() {
        return !violations.isEmpty();
    }

    public boolean hasCriticalViolations() {
        return violations.stream().anyMatch(v -> v.severity() == PolicySeverity.CRITICAL);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PolicyEvaluationResult that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "PolicyEvaluationResult{id=" + id + ", verdict=" + verdict + ", violations=" + violations.size() + "}";
    }

    /**
     * Overall evaluation outcome.
     */
    public enum Verdict {
        PASS,
        FAIL,
        WARNING
    }
}
