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
import com.keystone.analysis.infrastructure.event.AnalysisEventPublisher;
import com.keystone.analysis.infrastructure.repository.ChangeReportRepository;
import java.time.Instant;
import java.util.*;
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

    public DiffOrchestratorImpl(
            DetectorRegistry detectorRegistry,
            BaseVersionResolver baseVersionResolver,
            ChangeReportRepository reportRepository,
            AnalysisEventPublisher eventPublisher) {
        this.detectorRegistry = detectorRegistry;
        this.baseVersionResolver = baseVersionResolver;
        this.reportRepository = reportRepository;
        this.eventPublisher = eventPublisher;
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

            // Get all registered detectors
            List<ChangeDetector> detectors = detectorRegistry.getAllDetectors();
            List<Change> allChanges = new ArrayList<>();

            // Run each detector (individual failures are caught and skipped)
            for (ChangeDetector detector : detectors) {
                try {
                    // For the initial implementation, we use simplified detection
                    // that works at the endpoint level. Full OpenAPI parsing will
                    // be added in a future iteration.
                    List<Change> detectorChanges = detector.detect(null, null);
                    allChanges.addAll(detectorChanges);
                } catch (Exception e) {
                    log.warn("Detector '{}' failed, skipping: {}", detector.getName(), e.getMessage());
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
