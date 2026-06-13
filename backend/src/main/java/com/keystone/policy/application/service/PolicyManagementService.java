// Canonical Reference: .pi/architecture/modules/policy-engine.md
// Module: policy-engine
package com.keystone.policy.application.service;

import com.keystone.policy.application.dto.CreatePolicyRequest;
import com.keystone.policy.application.dto.PolicySummaryResponse;
import com.keystone.policy.application.dto.UpdatePolicyRequest;
import com.keystone.policy.domain.exception.PolicyNotFoundException;
import com.keystone.policy.domain.exception.PolicyParseException;
import com.keystone.policy.domain.model.Policy;
import java.util.List;
import java.util.UUID;

/**
 * Application service for CRUD operations on policies and policy sets.
 *
 * <p>This is the primary inbound port for managing policy definitions
 * directly (outside of sync operations). Supports creating, reading,
 * updating, and deactivating policies.
 *
 * <p>Policies created through this service are stored in the default
 * "local" policy set.
 */
public interface PolicyManagementService {

    /**
     * Creates a new policy from the given request.
     *
     * <p>The DSL expression is validated before the policy is persisted.
     *
     * @param request the policy creation details
     * @return the created domain Policy
     * @throws PolicyParseException if the DSL expression is invalid
     */
    Policy createPolicy(CreatePolicyRequest request) throws PolicyParseException;

    /**
     * Updates an existing policy.
     *
     * @param policyId the UUID of the policy to update
     * @param request  the fields to update (null fields are ignored)
     * @return the updated domain Policy
     * @throws PolicyNotFoundException if the policy does not exist
     * @throws PolicyParseException    if the updated DSL expression is invalid
     */
    Policy updatePolicy(UUID policyId, UpdatePolicyRequest request)
            throws PolicyNotFoundException, PolicyParseException;

    /**
     * Retrieves a policy by its UUID.
     *
     * @param policyId the UUID of the policy
     * @return the domain Policy
     * @throws PolicyNotFoundException if the policy does not exist
     */
    Policy getPolicy(UUID policyId) throws PolicyNotFoundException;

    /**
     * Lists all policies, optionally filtered by status.
     *
     * @param status   optional status filter (null = return all)
     * @param sourceId optional source filter (null = return all sources)
     * @return list of policy summaries
     */
    List<PolicySummaryResponse> listPolicies(com.keystone.policy.domain.model.PolicyStatus status, String sourceId);

    /**
     * Deactivates a policy (sets status to INACTIVE).
     *
     * <p>Policies are soft-deactivated rather than deleted to preserve
     * evaluation history.
     *
     * @param policyId the UUID of the policy to deactivate
     * @throws PolicyNotFoundException if the policy does not exist
     */
    void deactivatePolicy(UUID policyId) throws PolicyNotFoundException;

    /**
     * Activates a previously deactivated policy.
     *
     * @param policyId the UUID of the policy to activate
     * @return the activated domain Policy
     * @throws PolicyNotFoundException if the policy does not exist
     */
    Policy activatePolicy(UUID policyId) throws PolicyNotFoundException;
}
