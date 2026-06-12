package com.keystone.policy.infrastructure.repository.jpa;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * JPA entity for the {@code policy_evaluation_results} table.
 *
 * <p>Stores the results of evaluating policies against OpenAPI specifications.
 * Each evaluation result captures the verdict, violation count, and timing.
 * Detailed violation data is stored as JSON in the violations column.
 */
@Entity
@Table(name = "policy_evaluation_results")
public class EvaluationResultEntity {

    @Id
    private UUID id;

    @Column(name = "spec_id", nullable = false)
    private UUID specId;

    @Column(name = "policy_set_id", nullable = false)
    private UUID policySetId;

    @Column(nullable = false, length = 256)
    private String repository;

    @Column(name = "spec_path", nullable = false, length = 512)
    private String specPath;

    @Column(name = "commit_sha", length = 40)
    private String commitSha;

    @Column(nullable = false, length = 16)
    private String verdict;

    @Column(name = "violations_json", columnDefinition = "TEXT")
    private String violationsJson;

    @Column(name = "total_policies_checked")
    private int totalPoliciesChecked;

    @Column(name = "passed_count")
    private int passedCount;

    @Column(name = "failed_count")
    private int failedCount;

    @Column(name = "evaluated_at", nullable = false)
    private Instant evaluatedAt;

    protected EvaluationResultEntity() {}

    public EvaluationResultEntity(UUID id, UUID specId, UUID policySetId,
                                  String repository, String specPath, String commitSha,
                                  String verdict, String violationsJson,
                                  int totalPoliciesChecked, int passedCount, int failedCount,
                                  Instant evaluatedAt) {
        this.id = Objects.requireNonNull(id);
        this.specId = Objects.requireNonNull(specId);
        this.policySetId = Objects.requireNonNull(policySetId);
        this.repository = Objects.requireNonNull(repository);
        this.specPath = Objects.requireNonNull(specPath);
        this.commitSha = commitSha;
        this.verdict = Objects.requireNonNull(verdict);
        this.violationsJson = violationsJson;
        this.totalPoliciesChecked = totalPoliciesChecked;
        this.passedCount = passedCount;
        this.failedCount = failedCount;
        this.evaluatedAt = Objects.requireNonNull(evaluatedAt);
    }

    public UUID getId() { return id; }
    public UUID getSpecId() { return specId; }
    public UUID getPolicySetId() { return policySetId; }
    public String getRepository() { return repository; }
    public String getSpecPath() { return specPath; }
    public String getCommitSha() { return commitSha; }
    public String getVerdict() { return verdict; }
    public String getViolationsJson() { return violationsJson; }
    public int getTotalPoliciesChecked() { return totalPoliciesChecked; }
    public int getPassedCount() { return passedCount; }
    public int getFailedCount() { return failedCount; }
    public Instant getEvaluatedAt() { return evaluatedAt; }
}
