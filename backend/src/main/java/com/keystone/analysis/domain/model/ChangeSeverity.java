// Canonical Reference: .pi/architecture/modules/breaking-change-analysis.md
// Module: breaking-change-analysis
package com.keystone.analysis.domain.model;

/**
 * Severity classification for a detected change between two OpenAPI spec versions.
 *
 * <p>Per the architecture, changes are classified as:
 * <ul>
 *   <li>{@link #BREAKING} — definitely breaks existing clients</li>
 *   <li>{@link #NON_BREAKING} — changes that are backwards-compatible</li>
 *   <li>{@link #ADDITIVE} — new functionality that doesn't break existing clients</li>
 *   <li>{@link #DEPRECATION} — elements marked as deprecated</li>
 * </ul>
 */
public enum ChangeSeverity {
    BREAKING,
    NON_BREAKING,
    ADDITIVE,
    DEPRECATION
}
