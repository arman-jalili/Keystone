package com.keystone.policy.domain.model;

/**
 * Severity classification for a policy violation detected during evaluation.
 *
 * <p>Per the policy-engine architecture, violations are classified as:
 * <ul>
 *   <li>{@link #CRITICAL} — violation that breaks the contract or has security implications</li>
 *   <li>{@link #MAJOR} — violation that may break downstream consumers</li>
 *   <li>{@link #MINOR} — violation that is advisory (style, naming, etc.)</li>
 *   <li>{@link #INFO} — informational observation, no action required</li>
 * </ul>
 */
public enum PolicySeverity {
    CRITICAL,
    MAJOR,
    MINOR,
    INFO
}
