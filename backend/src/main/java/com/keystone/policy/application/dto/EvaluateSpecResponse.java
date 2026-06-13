// Canonical Reference: .pi/architecture/modules/policy-engine.md
// Module: policy-engine
package com.keystone.policy.application.dto;

import com.keystone.policy.domain.model.PolicyEvaluationResult;
import com.keystone.policy.domain.model.Violation;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Output DTO for policy evaluation results.
 *
 * @param id                   Unique identifier for this evaluation result
 * @param specId               The UUID of the evaluated OpenAPI specification
 * @param policySetId          The UUID of the evaluated policy set
 * @param repository           The repository identifier
 * @param specPath             The relative spec path
 * @param commitSha            The git commit SHA evaluated
 * @param verdict              The overall evaluation verdict
 * @param violations           The list of detected violations
 * @param totalPoliciesChecked Number of policies that were checked
 * @param passedCount          Number of policies that passed
 * @param failedCount          Number of policies that were violated
 * @param evaluatedAt          When the evaluation was completed
 */
public record EvaluateSpecResponse(
        UUID id,
        UUID specId,
        UUID policySetId,
        String repository,
        String specPath,
        String commitSha,
        PolicyEvaluationResult.Verdict verdict,
        List<ViolationDto> violations,
        int totalPoliciesChecked,
        int passedCount,
        int failedCount,
        Instant evaluatedAt) {
    public EvaluateSpecResponse {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(specId, "specId must not be null");
        Objects.requireNonNull(policySetId, "policySetId must not be null");
        Objects.requireNonNull(repository, "repository must not be null");
        Objects.requireNonNull(specPath, "specPath must not be null");
        Objects.requireNonNull(verdict, "verdict must not be null");
        violations = violations == null ? List.of() : List.copyOf(violations);
        Objects.requireNonNull(evaluatedAt, "evaluatedAt must not be null");
    }

    /**
     * Creates an {@link EvaluateSpecResponse} from a domain {@link PolicyEvaluationResult}.
     */
    public static EvaluateSpecResponse from(PolicyEvaluationResult result) {
        return new EvaluateSpecResponse(
                result.getId(),
                result.getSpecId(),
                result.getPolicySetId(),
                result.getRepository(),
                result.getSpecPath(),
                result.getCommitSha(),
                result.getVerdict(),
                result.getViolations().stream().map(ViolationDto::from).toList(),
                result.getTotalPoliciesChecked(),
                result.getPassedCount(),
                result.getFailedCount(),
                result.getEvaluatedAt());
    }

    /**
     * Output DTO for a single violation.
     *
     * @param policyId     The UUID of the violated policy
     * @param policyName   The name of the violated policy
     * @param severity     The violation severity level
     * @param message      A human-readable description of the violation
     * @param specPath     JSON Pointer path within the spec where the violation occurred
     * @param suggestedFix Optional suggestion for fixing the violation
     */
    public record ViolationDto(
            UUID policyId, String policyName, String severity, String message, String specPath, String suggestedFix) {
        public static ViolationDto from(Violation violation) {
            return new ViolationDto(
                    violation.policyId(),
                    violation.policyName(),
                    violation.severity().name(),
                    violation.message(),
                    violation.specPath(),
                    violation.suggestedFix());
        }
    }
}
