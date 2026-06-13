// Canonical Reference: .pi/architecture/modules/policy-engine.md
// Module: policy-engine
package com.keystone.policy.domain.model;

import java.util.Optional;
import java.util.UUID;

/**
 * Manages exemption lifecycle for policy violations.
 *
 * <p>Exemptions allow specific changes to bypass policy rules for a
 * limited time. This interface handles the exemption lifecycle:
 * grant, check, renew, expire, and revoke.
 */
public interface ExemptionManager {

    /**
     * Checks whether a specific change has an active exemption
     * for the given policy.
     *
     * @param changeId the identifier of the change
     * @param policyId the UUID of the policy
     * @return true if an active, non-expired exemption exists
     */
    boolean hasExemption(String changeId, UUID policyId);

    /**
     * Retrieves the active exemption for a change/policy combination.
     *
     * @param changeId the identifier of the change
     * @param policyId the UUID of the policy
     * @return the exemption if found and active, empty otherwise
     */
    Optional<Exemption> getExemption(String changeId, UUID policyId);

    /**
     * Grants a new exemption.
     *
     * @param exemption the exemption to grant
     * @return the granted exemption
     */
    Exemption grantExemption(Exemption exemption);

    /**
     * Revokes an active exemption before its expiry.
     *
     * @param exemptionId the UUID of the exemption to revoke
     */
    void revokeExemption(UUID exemptionId);

    /**
     * Renews an exemption with a new expiry date.
     *
     * @param exemptionId the UUID of the exemption to renew
     * @param newExpiry   the new expiry timestamp
     * @return the renewed exemption
     */
    Exemption renewExemption(UUID exemptionId, java.time.Instant newExpiry);
}
