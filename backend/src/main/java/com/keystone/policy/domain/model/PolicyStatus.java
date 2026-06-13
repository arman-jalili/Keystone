// Canonical Reference: .pi/architecture/modules/policy-engine.md
// Module: policy-engine
package com.keystone.policy.domain.model;

/**
 * Lifecycle status for a policy rule.
 *
 * <ul>
 *   <li>{@link #ACTIVE} — policy is evaluated against all incoming specs</li>
 *   <li>{@link #INACTIVE} — policy is preserved but not evaluated</li>
 *   <li>{@link #DEPRECATED} — policy is scheduled for removal, emits warnings</li>
 * </ul>
 */
public enum PolicyStatus {
    ACTIVE,
    INACTIVE,
    DEPRECATED
}
