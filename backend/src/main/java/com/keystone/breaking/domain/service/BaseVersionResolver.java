package com.keystone.breaking.domain.service;

import com.keystone.breaking.domain.exception.VersionResolutionException;
import com.keystone.breaking.domain.model.BaseVersion;

/**
 * Domain service that resolves the base version to compare against
 * during breaking change analysis.
 *
 * <p>The resolution strategy determines which specification version is
 * used as the "old" baseline. Different strategies suit different
 * workflows:
 *
 * <ul>
 *   <li><strong>Last known good</strong> — compares against the most recently
 *       ingested version of the same spec (default for CLI audit)</li>
 *   <li><strong>Git base branch</strong> — resolves the spec at the base
 *       branch tip (e.g. {@code main} or {@code master}) for PR reviews</li>
 *   <li><strong>Pinned version</strong> — compares against a specific,
 *       user-provided version or tag</li>
 *   <li><strong>Custom</strong> — delegated to a user-provided resolution strategy</li>
 * </ul>
 *
 * <p>Implementations must handle the case where no base version exists
 * (first-time ingestion) gracefully.
 */
public interface BaseVersionResolver {

    /**
     * Resolves the base version for the given specification at the given commit.
     *
     * <p>The resolver uses its configured strategy to determine the
     * appropriate base version. For first-time ingestion where no
     * previous version exists, implementations should return an empty
     * result (or a distinguished "no-base" indicator).
     *
     * @param repository the repository identifier (e.g. "org/repo")
     * @param specPath   the relative spec path within the repository
     * @param newCommit  the git commit SHA of the new spec version
     * @return the resolved base version, or an empty BaseVersion if no
     *         previous version exists
     * @throws VersionResolutionException if resolution fails
     */
    BaseVersion resolve(String repository, String specPath, String newCommit)
            throws VersionResolutionException;

    /**
     * Returns the resolution strategy this resolver implements.
     *
     * @return the strategy identifier
     */
    BaseVersion.ResolutionStrategy getStrategy();
}
