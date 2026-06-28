// Canonical Reference: .pi/architecture/modules/breaking-change-analysis.md
// Module: breaking-change-analysis
package com.keystone.analysis.domain.service.impl;

import com.keystone.analysis.domain.detector.ChangeDetector;
import com.keystone.analysis.domain.event.BreakingChangeReportedEvent;
import com.keystone.analysis.domain.exception.DiffAnalysisException;
import com.keystone.analysis.domain.exception.NoBaseVersionException;
import com.keystone.analysis.domain.model.*;
import com.keystone.analysis.domain.service.BaseVersionResolver;
import com.keystone.analysis.domain.service.DetectorRegistry;
import com.keystone.analysis.domain.service.DiffOrchestrator;
import com.keystone.analysis.domain.service.SpecParser;
import com.keystone.analysis.infrastructure.event.AnalysisEventPublisher;
import com.keystone.analysis.infrastructure.repository.ChangeReportRepository;
import com.keystone.ingestion.domain.model.SpecVersion;
import com.keystone.ingestion.infrastructure.repository.SpecRepository;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Default implementation of {@link DiffOrchestrator}.
 *
 * <p>Coordinates the full diff pipeline:
 * <ol>
 *   <li>Resolve base version via {@link BaseVersionResolver}</li>
 *   <li>Parse base and target specs into endpoint lists</li>
 *   <li>Pair endpoints by operation key (METHOD path)</li>
 *   <li>Run all registered detectors on each pair</li>
 *   <li>Compute verdict</li>
 *   <li>Persist report</li>
 *   <li>Publish domain event</li>
 * </ol>
 */
@Service
public class DiffOrchestratorImpl implements DiffOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(DiffOrchestratorImpl.class);

    private final DetectorRegistry detectorRegistry;
    private final BaseVersionResolver baseVersionResolver;
    private final ChangeReportRepository reportRepository;
    private final AnalysisEventPublisher eventPublisher;
    private final SpecRepository specRepository;
    private final SpecParser specParser;

    public DiffOrchestratorImpl(
            DetectorRegistry detectorRegistry,
            BaseVersionResolver baseVersionResolver,
            ChangeReportRepository reportRepository,
            AnalysisEventPublisher eventPublisher,
            SpecRepository specRepository,
            SpecParser specParser) {
        this.detectorRegistry = detectorRegistry;
        this.baseVersionResolver = baseVersionResolver;
        this.reportRepository = reportRepository;
        this.eventPublisher = eventPublisher;
        this.specRepository = specRepository;
        this.specParser = specParser;
    }

    @Override
    public BreakingChangeReport analyze(String repository, String specPath, UUID targetSpecId)
            throws DiffAnalysisException {
        try {
            BaseVersion baseVersion = baseVersionResolver.resolve(repository, specPath, targetSpecId.toString());
            return analyzeWithBase(repository, specPath, targetSpecId, baseVersion);
        } catch (NoBaseVersionException e) {
            log.warn("No base version for {}/{}: {}", repository, specPath, e.getMessage());
            // Return INCONCLUSIVE report
            BreakingChangeReport report = new BreakingChangeReport(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    targetSpecId,
                    repository,
                    specPath,
                    null,
                    targetSpecId.toString(),
                    Verdict.INCONCLUSIVE,
                    List.of(),
                    Instant.now());
            return reportRepository.save(report);
        } catch (Exception e) {
            throw new DiffAnalysisException(
                    "Diff analysis failed: " + e.getMessage(), repository, specPath, "resolve-version", e);
        }
    }

    @Override
    public BreakingChangeReport analyzeWithBase(
            String repository, String specPath, UUID targetSpecId, BaseVersion baseVersion)
            throws DiffAnalysisException {
        try {
            Instant start = Instant.now();
            log.info("Starting diff analysis for {}/{} (target: {})", repository, specPath, targetSpecId);

            // Fetch base and target spec contents
            SpecVersion baseSpecVersion = fetchVersionById(baseVersion.versionId());
            SpecVersion targetSpecVersion = fetchLatestVersion(targetSpecId, repository, specPath);

            // Parse both specs into endpoint lists
            List<ParsedEndpoint> baseEndpoints = baseSpecVersion != null
                    ? specParser.parse(baseSpecVersion.getRawContent())
                    : List.of();
            List<ParsedEndpoint> targetEndpoints = targetSpecVersion != null
                    ? specParser.parse(targetSpecVersion.getRawContent())
                    : List.of();

            log.info("Parsed {} base endpoints and {} target endpoints for {}/{}",
                    baseEndpoints.size(), targetEndpoints.size(), repository, specPath);

            // Build endpoint map by key (METHOD path) for pairing
            Map<String, ParsedEndpoint> baseByKey = baseEndpoints.stream()
                    .collect(Collectors.toMap(ParsedEndpoint::endpointKey, e -> e));
            Map<String, ParsedEndpoint> targetByKey = targetEndpoints.stream()
                    .collect(Collectors.toMap(ParsedEndpoint::endpointKey, e -> e));

            // Get all unique endpoint keys from both specs
            Set<String> allKeys = new HashSet<>();
            allKeys.addAll(baseByKey.keySet());
            allKeys.addAll(targetByKey.keySet());

            // Get all registered detectors
            List<ChangeDetector> detectors = detectorRegistry.getAllDetectors();
            List<Change> allChanges = new ArrayList<>();

            // Run each detector on every endpoint pair
            for (String key : allKeys) {
                ParsedEndpoint base = baseByKey.get(key);
                ParsedEndpoint target = targetByKey.get(key);

                for (ChangeDetector detector : detectors) {
                    try {
                        // PathRemovalDetector handles base != null && target == null
                        // New endpoint detectors handle base == null && target != null
                        // Change detectors handle base != null && target != null
                        List<Change> detectorChanges = detector.detect(base, target);
                        allChanges.addAll(detectorChanges);
                    } catch (Exception e) {
                        log.warn("Detector '{}' failed on endpoint '{}', skipping: {}",
                                detector.getName(), key, e.getMessage());
                    }
                }
            }

            // Compute verdict
            Verdict verdict = computeVerdict(allChanges);

            // Build and persist report
            BreakingChangeReport report = new BreakingChangeReport(
                    UUID.randomUUID(),
                    UUID.fromString(baseVersion.versionId()),
                    targetSpecId,
                    repository,
                    specPath,
                    baseVersion.label(),
                    targetSpecId.toString(),
                    verdict,
                    List.copyOf(allChanges),
                    Instant.now());

            BreakingChangeReport saved = reportRepository.save(report);

            // Publish event
            BreakingChangeReportedEvent event = new BreakingChangeReportedEvent(
                    UUID.randomUUID(),
                    saved.getId(),
                    repository,
                    specPath,
                    verdict,
                    saved.getChanges(),
                    Instant.now(),
                    repository + ":" + specPath + ":" + targetSpecId);
            eventPublisher.breakingChangeReported(event);

            log.info(
                    "Diff analysis completed for {}/{}: verdict={}, changes={}",
                    repository,
                    specPath,
                    verdict,
                    allChanges.size());

            return saved;

        } catch (Exception e) {
            throw new DiffAnalysisException(
                    "Diff analysis failed: " + e.getMessage(), repository, specPath, "analysis", e);
        }
    }

    @Override
    public void registerDetector(ChangeDetector detector) {
        detectorRegistry.register(detector);
    }

    /**
     * Fetches a SpecVersion by its UUID.
     */
    private SpecVersion fetchVersionById(String versionId) {
        try {
            return specRepository.findVersionById(UUID.fromString(versionId)).orElse(null);
        } catch (Exception e) {
            log.warn("Failed to fetch version by id '{}': {}", versionId, e.getMessage());
            return null;
        }
    }

    /**
     * Fetches the latest SpecVersion for a given spec.
     * Falls back to finding the spec by repository+path if targetSpecId fails.
     */
    private SpecVersion fetchLatestVersion(UUID targetSpecId, String repository, String specPath) {
        try {
            var versions = specRepository.findVersionsBySpecId(targetSpecId, 1);
            if (!versions.isEmpty()) {
                return versions.getFirst();
            }
            // Fallback: try to find the spec by repository+path
            var optSpec = specRepository.findByRepositoryAndSpecPath(repository, specPath);
            if (optSpec.isPresent()) {
                versions = specRepository.findVersionsBySpecId(optSpec.get().getId(), 1);
                if (!versions.isEmpty()) {
                    return versions.getFirst();
                }
            }
            log.warn("No version found for spec {}", targetSpecId);
            return null;
        } catch (Exception e) {
            log.warn("Failed to fetch latest version for spec '{}': {}", targetSpecId, e.getMessage());
            return null;
        }
    }

    private Verdict computeVerdict(List<Change> changes) {
        if (changes.isEmpty()) {
            return Verdict.PASS;
        }
        boolean hasBreaking = changes.stream().anyMatch(c -> c.severity() == ChangeSeverity.BREAKING);
        if (hasBreaking) {
            return Verdict.BREAKING;
        }
        return Verdict.NON_BREAKING;
    }
}
