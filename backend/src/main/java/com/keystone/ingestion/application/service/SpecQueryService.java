// Canonical Reference: .pi/architecture/modules/contract-ingestion.md
// Module: contract-ingestion
package com.keystone.ingestion.application.service;

import com.keystone.ingestion.application.dto.ApiInventoryItem;
import com.keystone.ingestion.application.dto.StaleApiItem;
import com.keystone.ingestion.domain.model.OpenApiSpec;
import com.keystone.ingestion.domain.model.SpecVersion;
import com.keystone.ingestion.infrastructure.repository.SpecRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for querying spec metadata for the Dashboard UI.
 *
 * <p>Provides methods to list all APIs and identify stale specs
 * that haven't been re-ingested past a configurable threshold.
 */
@Service
@Transactional(readOnly = true)
public class SpecQueryService {

    private static final Logger log = LoggerFactory.getLogger(SpecQueryService.class);

    private final SpecRepository specRepository;

    public SpecQueryService(SpecRepository specRepository) {
        this.specRepository = specRepository;
    }

    /**
     * Returns all ingested API specs formatted for the Dashboard inventory view.
     */
    public List<ApiInventoryItem> listAllApis() {
        List<OpenApiSpec> allSpecs = specRepository.findAllByOrderByIngestedAtDesc();
        return allSpecs.stream()
                .map(this::toApiInventoryItem)
                .toList();
    }

    /**
     * Returns specs whose latest version was ingested longer ago than the threshold.
     *
     * @param thresholdDays number of days since last ingestion to consider stale
     */
    public List<StaleApiItem> listStaleApis(int thresholdDays) {
        Instant threshold = Instant.now().minus(Duration.ofDays(thresholdDays));
        List<OpenApiSpec> staleSpecs = specRepository.findStaleSpecs(threshold);
        return staleSpecs.stream()
                .map(spec -> {
                    long daysStale = Duration.between(spec.getIngestedAt(), Instant.now()).toDays();
                    String latestVersion = findLatestVersion(spec.getId()).orElse("unknown");
                    return new StaleApiItem(
                            spec.getId(),
                            deriveServiceName(spec.getRepository()),
                            spec.getIngestedAt(),
                            Math.max(0, daysStale),
                            latestVersion);
                })
                .toList();
    }

    private ApiInventoryItem toApiInventoryItem(OpenApiSpec spec) {
        String latestVersion = findLatestVersion(spec.getId()).orElse("unknown");
        return new ApiInventoryItem(
                spec.getId(),
                deriveServiceName(spec.getRepository()),
                latestVersion,
                "OPENAPI_3_0",
                "healthy",
                spec.getIngestedAt(),
                deriveOwner(spec.getRepository()),
                null,  // policyPassRate — computed by Policy context
                null   // openBreakages — computed by Analysis context
        );
    }

    private Optional<String> findLatestVersion(UUID specId) {
        List<SpecVersion> versions = specRepository.findVersionsBySpecId(specId, 1);
        if (versions.isEmpty()) {
            return Optional.empty();
        }
        // Use commit SHA prefix as version identifier
        String sha = versions.getFirst().getCommitSha();
        return Optional.of(sha.length() >= 7 ? sha.substring(0, 7) : sha);
    }

    /**
     * Derives a service name from a repository identifier like "org/payment-service".
     */
    static String deriveServiceName(String repository) {
        int slash = repository.lastIndexOf('/');
        if (slash >= 0 && slash + 1 < repository.length()) {
            return repository.substring(slash + 1);
        }
        return repository;
    }

    /**
     * Derives an owner from a repository identifier like "org/payment-service".
     */
    static String deriveOwner(String repository) {
        int slash = repository.indexOf('/');
        if (slash > 0) {
            return "team-" + repository.substring(0, slash);
        }
        return "unknown";
    }
}
