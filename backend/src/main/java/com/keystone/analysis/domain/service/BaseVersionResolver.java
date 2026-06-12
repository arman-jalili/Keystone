package com.keystone.analysis.domain.service;

import com.keystone.analysis.domain.exception.NoBaseVersionException;
import com.keystone.analysis.domain.model.BaseVersion;

/**
 * Domain service that resolves the base version to compare against
 * during breaking change analysis.
 *
 * <p>Per the architecture, uses a three-layer fallback:
 * <ol>
 *   <li><strong>Explicit base ref</strong> — caller provides a specific commit SHA or version</li>
 *   <li><strong>Previous ingested version</strong> — most recently ingested version of the same spec</li>
 *   <li><strong>Latest version on main branch</strong> — fallback to the tip of {@code main}</li>
 * </ol>
 *
 * <p>If all three layers are exhausted, a {@link NoBaseVersionException} is thrown.
 * The caller ({@link DiffOrchestrator}) should handle this by producing an
 * {@link com.keystone.analysis.domain.model.Verdict#INCONCLUSIVE} report.
 */
public interface BaseVersionResolver {

    /**
     * Resolves the base version for a given specification.
     *
     * <p>Uses the three-layer fallback strategy. The caller may provide
     * an explicit base ref to bypass layer 1.
     *
     * @param repository      the repository identifier (e.g. "org/repo")
     * @param specPath        the relative spec path within the repository
     * @param targetCommitSha the git commit SHA of the target spec version
     * @param explicitBaseRef optional explicit base reference (may be null)
     * @return the resolved base version
     * @throws NoBaseVersionException if all resolution layers are exhausted
     */
    BaseVersion resolve(String repository, String specPath,
                         String targetCommitSha, String explicitBaseRef)
            throws NoBaseVersionException;

    /**
     * Resolves the base version without an explicit reference.
     *
     * <p>Equivalent to calling {@link #resolve(String, String, String, String)}
     * with a null explicitBaseRef.
     */
    default BaseVersion resolve(String repository, String specPath, String targetCommitSha)
            throws NoBaseVersionException {
        return resolve(repository, specPath, targetCommitSha, null);
    }
}
