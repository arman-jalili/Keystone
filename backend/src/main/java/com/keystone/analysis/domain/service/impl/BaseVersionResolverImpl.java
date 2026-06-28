// Canonical Reference: .pi/architecture/modules/breaking-change-analysis.md
// Module: breaking-change-analysis
package com.keystone.analysis.domain.service.impl;

import com.keystone.analysis.domain.exception.NoBaseVersionException;
import com.keystone.analysis.domain.model.BaseVersion;
import com.keystone.analysis.domain.service.BaseVersionResolver;
import com.keystone.ingestion.infrastructure.repository.SpecRepository;
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

    private final SpecRepository specRepository;

    public BaseVersionResolverImpl(SpecRepository specRepository) {
        this.specRepository = specRepository;
    }

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
        // Fetch 2 versions: the latest is the target (just saved), the second is the base
        var existingSpec = specRepository.findByRepositoryAndSpecPath(repository, specPath);
        if (existingSpec.isPresent()) {
            var spec = existingSpec.get();
            var versions = specRepository.findVersionsBySpecId(spec.getId(), 2);
            // versions[0] is the latest (just saved target), versions[1] is the previous
            if (versions.size() >= 2) {
                var previousVersion = versions.get(1);
                log.info("Layer 2: Using previous ingested version '{}' for {}/{}",
                        previousVersion.getCommitSha(), repository, specPath);
                return new BaseVersion(
                        previousVersion.getId().toString(),
                        "Previous ingested version: " + previousVersion.getCommitSha(),
                        previousVersion.getCommitSha(),
                        resolvedAt);
            }
            // Only one version exists — can't diff against itself, fall through to Layer 3
        }

        // Layer 3: Latest version across all specs for the same repository
        // Query for any version in the same repository to serve as a generic fallback
        var allSpecs = specRepository.findAllByOrderByIngestedAtDesc();
        var repoSpec = allSpecs.stream()
                .filter(s -> s.getRepository().equals(repository))
                .findFirst();
        if (repoSpec.isPresent()) {
            var spec = repoSpec.get();
            var versions = specRepository.findVersionsBySpecId(spec.getId(), 1);
            if (!versions.isEmpty()) {
                var latestVersion = versions.getFirst();
                log.info("Layer 3: Using latest version from repository '{}' for {}/{}",
                        latestVersion.getCommitSha(), repository, specPath);
                return new BaseVersion(
                        latestVersion.getId().toString(),
                        "Latest version from repository: " + latestVersion.getCommitSha(),
                        latestVersion.getCommitSha(),
                        resolvedAt);
            }
        }

        throw new NoBaseVersionException(
                "No base version found for " + repository + "/" + specPath
                        + " at commit " + targetCommitSha
                        + " — no explicit ref, no previous version, and no repository version available",
                repository,
                specPath);
    }
}
