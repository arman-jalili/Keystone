package com.keystone.policy.domain.exception;

import java.util.Objects;
import java.util.UUID;

/**
 * Exception thrown when a requested policy or policy set cannot be found.
 */
public class PolicyNotFoundException extends RuntimeException {

    private final UUID policyId;

    public PolicyNotFoundException(UUID policyId) {
        super("Policy not found: " + Objects.requireNonNull(policyId, "policyId must not be null"));
        this.policyId = policyId;
    }

    public PolicyNotFoundException(String name, String sourceId) {
        super("Policy not found: name='" + name + "', sourceId='" + sourceId + "'");
        this.policyId = null;
    }

    public UUID getPolicyId() {
        return policyId;
    }
}
