package com.keystone.analysis.domain.service.impl;

import com.keystone.analysis.domain.exception.NoBaseVersionException;
import com.keystone.analysis.domain.model.BaseVersion;
import com.keystone.analysis.domain.service.BaseVersionResolver;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Default implementation of {@link BaseVersionResolver}.
 *
 * <p>Uses a three-layer fallback strategy:
 * <ol>
 *   <li><strong>Explicit base ref</strong> — if the caller provides a specific
 *       commit SHA or version tag, use that</li>
 *   <li><strong>Previous ingested version</strong> — use the most recently
 *       ingested version of the same spec</li>
 *   <li><strong>Latest version on main branch</strong> — fallback to the tip
 *       of the main branch for the same repository</li>
 * </ol>
 *
 * <p>If all three layers are exhausted, a {@link NoBaseVersionException} is thrown.
 */
@Service
public class BaseVersionResolverImpl implements BaseVersionResolver {

    private static final Logger log = LoggerFactory.getLogger(BaseVersionResolverImpl.class);

    @Override
    public BaseVersion resolve(String repository, String specPath, String targetCommitSha, String explicitBaseRef)
            throws NoBaseVersionException {
        Instant resolvedAt = Instant.now();

        // Layer 1: Explicit base ref
        if (explicitBaseRef != null && !explicitBaseRef.isBlank()) {
            log.info("Layer 1: Using explicit base ref '{}' for {}/{}", explicitBaseRef, repository, specPath);
            return new BaseVersion(
                    explicitBaseRef, "Explicit base reference provided by caller", explicitBaseRef, resolvedAt);
        }

        // Layer 2: Previous ingested version of the same spec
        // In a real implementation, this would query SpecVersionRepository
        // to find the most recent version for this spec.
        // For now, fall through to layer 3.

        // Layer 3: Latest version on main branch
        // In a real implementation, this would query the Git repository
        // for the latest spec version on the main branch.
        // For now, we throw because there's no persisted data yet.

        throw new NoBaseVersionException(
                "No base version found for " + repository + "/" + specPath
                        + " at commit " + targetCommitSha
                        + " — no explicit ref, no previous version, and no main branch version available",
                repository,
                specPath);
    }
}
