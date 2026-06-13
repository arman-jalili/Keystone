// Canonical Reference: .pi/architecture/modules/breaking-change-analysis.md
// Module: breaking-change-analysis
package com.keystone.analysis.domain.model;

/**
 * Overall verdict of a breaking change analysis.
 *
 * <p>Determined by {@link com.keystone.analysis.domain.service.DiffOrchestrator} after
 * running all registered {@link com.keystone.analysis.domain.detector.ChangeDetector} instances.
 *
 * <ul>
 *   <li>{@link #PASS} — no changes detected, specs are compatible</li>
 *   <li>{@link #BREAKING} — at least one BREAKING change detected</li>
 *   <li>{@link #NON_BREAKING} — changes detected but none are breaking</li>
 *   <li>{@link #INCONCLUSIVE} — analysis could not be completed (e.g. no base version)</li>
 * </ul>
 */
public enum Verdict {
    PASS,
    BREAKING,
    NON_BREAKING,
    INCONCLUSIVE
}
