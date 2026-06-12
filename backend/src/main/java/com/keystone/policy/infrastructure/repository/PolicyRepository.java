package com.keystone.policy.infrastructure.repository;

import com.keystone.policy.domain.model.Policy;
import com.keystone.policy.domain.model.PolicyEvaluationResult;
import com.keystone.policy.domain.model.PolicySet;
import com.keystone.policy.domain.model.PolicyStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for accessing {@link Policy}, {@link PolicySet},
 * and {@link PolicyEvaluationResult} entities.
 *
 * <p>This is the data access contract for the policy bounded context.
 * The implementation may use Spring Data JPA, raw JDBC, or any other
 * persistence mechanism. Callers must not depend on implementation
 * details such as table names or column mappings.
 *
 * <p>This interface intentionally follows repository pattern conventions
 * that are compatible with Spring Data JPA for the initial implementation,
 * but is defined as a plain interface to keep the contract framework-agnostic.
 */
public interface PolicyRepository {

    // ---- Policy operations ----

    /**
     * Finds a policy by its unique identifier.
     *
     * @param policyId the policy UUID
     * @return the policy if found, or empty if not
     */
    Optional<Policy> findPolicyById(UUID policyId);

    /**
     * Finds a policy by its name and source.
     *
     * @param name     the policy name
     * @param sourceId the source identifier
     * @return the policy if found, or empty if not
     */
    Optional<Policy> findPolicyByNameAndSource(String name, String sourceId);

    /**
     * Returns all policies, optionally filtered by status.
     *
     * @param status optional status filter (null = return all)
     * @return list of policies matching the filter
     */
    List<Policy> findAllPolicies(PolicyStatus status);

    /**
     * Returns all policies from a specific source.
     *
     * @param sourceId the source identifier
     * @return list of policies from that source
     */
    List<Policy> findPoliciesBySource(String sourceId);

    /**
     * Persists a new policy.
     *
     * @param policy the policy to save
     * @return the saved policy with any generated fields populated
     */
    Policy savePolicy(Policy policy);

    /**
     * Updates an existing policy.
     *
     * @param policy the policy with updated fields
     * @return the updated policy
     */
    Policy updatePolicy(Policy policy);

    /**
     * Deletes a policy by its UUID.
     *
     * @param policyId the UUID of the policy to delete
     */
    void deletePolicy(UUID policyId);

    /**
     * Deletes all policies associated with a given source.
     *
     * @param sourceId the source identifier
     * @return the number of deleted policies
     */
    int deletePoliciesBySource(String sourceId);

    // ---- PolicySet operations ----

    /**
     * Finds a policy set by its unique identifier.
     *
     * @param policySetId the policy set UUID
     * @return the policy set if found, or empty if not
     */
    Optional<PolicySet> findPolicySetById(UUID policySetId);

    /**
     * Finds a policy set by its name.
     *
     * @param name the policy set name
     * @return the policy set if found, or empty if not
     */
    Optional<PolicySet> findPolicySetByName(String name);

    /**
     * Returns all policy sets.
     *
     * @return list of all policy sets
     */
    List<PolicySet> findAllPolicySets();

    /**
     * Persists a new policy set (with its policies).
     *
     * @param policySet the policy set to save
     * @return the saved policy set with any generated fields populated
     */
    PolicySet savePolicySet(PolicySet policySet);

    /**
     * Updates an existing policy set.
     *
     * @param policySet the policy set with updated fields
     * @return the updated policy set
     */
    PolicySet updatePolicySet(PolicySet policySet);

    /**
     * Deletes a policy set by its UUID.
     *
     * @param policySetId the UUID of the policy set to delete
     */
    void deletePolicySet(UUID policySetId);

    /**
     * Soft-deactivates (sets status to INACTIVE) policies whose names are
     * not in the active list. Used during sync to remove policies that
     * no longer exist in the remote source.
     *
     * @param activePolicyNames the list of policy names that should remain active
     * @return the number of policies deactivated
     */
    int deactivateStalePolicies(List<String> activePolicyNames);

    // ---- Evaluation result operations ----

    /**
     * Finds an evaluation result by its unique identifier.
     *
     * @param evaluationId the evaluation result UUID
     * @return the result if found, or empty if not
     */
    Optional<PolicyEvaluationResult> findEvaluationById(UUID evaluationId);

    /**
     * Returns the most recent evaluation results for a given spec, ordered by
     * evaluation timestamp descending.
     *
     * @param specId the spec UUID
     * @param limit  the maximum number of results to return
     * @return the most recent evaluation results
     */
    List<PolicyEvaluationResult> findEvaluationsBySpecId(UUID specId, int limit);

    /**
     * Persists an evaluation result.
     *
     * @param result the evaluation result to save
     * @return the saved result with any generated fields populated
     */
    PolicyEvaluationResult saveEvaluation(PolicyEvaluationResult result);
}
