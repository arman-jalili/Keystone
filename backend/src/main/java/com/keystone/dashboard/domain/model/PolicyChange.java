// Canonical Reference: .pi/architecture/modules/dashboard.md#policy-ui-service
// Implements: Domain model for a policy change requested via the UI
package com.keystone.dashboard.domain.model;

import java.util.Objects;

/**
 * A policy change requested by a compliance manager via the Dashboard UI.
 *
 * @param policyName  The name of the policy to change
 * @param action      The action to perform (e.g. "UPDATE", "CREATE", "DELETE")
 * @param yamlContent The new YAML content for the policy
 * @param message     The commit message describing the change
 */
public record PolicyChange(String policyName, String action, String yamlContent, String message) {

    public PolicyChange {
        Objects.requireNonNull(policyName, "policyName must not be null");
        Objects.requireNonNull(action, "action must not be null");
        Objects.requireNonNull(yamlContent, "yamlContent must not be null");
        Objects.requireNonNull(message, "message must not be null");
    }
}
