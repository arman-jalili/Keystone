// Canonical Reference: .pi/architecture/modules/breaking-change-analysis.md
// Module: breaking-change-analysis
package com.keystone.analysis.application.service;

import com.keystone.analysis.application.dto.AnalysisRequest;
import com.keystone.analysis.application.dto.AnalysisResponse;
import com.keystone.analysis.domain.exception.DiffAnalysisException;
import com.keystone.analysis.domain.exception.NoBaseVersionException;
import com.keystone.analysis.domain.model.BaseVersion;
import com.keystone.analysis.domain.model.BreakingChangeReport;
import com.keystone.analysis.domain.service.BaseVersionResolver;
import com.keystone.analysis.domain.service.DiffOrchestrator;
import com.keystone.analysis.infrastructure.repository.ChangeReportRepository;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Default implementation of {@link BreakingAnalysisService}.
 *
 * <p>Orchestrates the analysis flow:
 * <ol>
 *   <li>Resolve base version via {@link BaseVersionResolver}</li>
 *   <li>Execute diff analysis via {@link DiffOrchestrator}</li>
 *   <li>Format and return the response</li>
 * </ol>
 */
@Service
public class BreakingAnalysisServiceImpl implements BreakingAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(BreakingAnalysisServiceImpl.class);

    private final DiffOrchestrator diffOrchestrator;
    private final BaseVersionResolver baseVersionResolver;
    private final ChangeReportRepository reportRepository;

    public BreakingAnalysisServiceImpl(
            DiffOrchestrator diffOrchestrator,
            BaseVersionResolver baseVersionResolver,
            ChangeReportRepository reportRepository) {
        this.diffOrchestrator = diffOrchestrator;
        this.baseVersionResolver = baseVersionResolver;
        this.reportRepository = reportRepository;
    }

    @Override
    public AnalysisResponse analyze(AnalysisRequest request) throws DiffAnalysisException, NoBaseVersionException {
        log.info(
                "Analysis requested for {}/{} at commit {}",
                request.repository(),
                request.specPath(),
                request.commitSha());

        // Resolve base version (with or without explicit ref)
        if (request.hasExplicitBaseRef()) {
            BaseVersion baseVersion = baseVersionResolver.resolve(
                    request.repository(), request.specPath(),
                    request.commitSha(), request.explicitBaseRef());

            // TODO: In a full implementation, we'd need the target SpecVersion UUID.
            // For now, use a placeholder until the integration with SpecVersionRepository is wired.
            UUID targetSpecId = UUID.nameUUIDFromBytes(
                    (request.repository() + ":" + request.specPath() + ":" + request.commitSha()).getBytes());

            BreakingChangeReport report = diffOrchestrator.analyzeWithBase(
                    request.repository(), request.specPath(), targetSpecId, baseVersion);
            return AnalysisResponse.from(report);
        }

        // Auto-resolve base version
        UUID targetSpecId = UUID.nameUUIDFromBytes(
                (request.repository() + ":" + request.specPath() + ":" + request.commitSha()).getBytes());

        BreakingChangeReport report = diffOrchestrator.analyze(request.repository(), request.specPath(), targetSpecId);
        return AnalysisResponse.from(report);
    }

    @Override
    public AnalysisResponse reAnalyze(UUID reportId) throws DiffAnalysisException {
        log.info("Re-analysis requested for report {}", reportId);

        BreakingChangeReport existing = reportRepository
                .findById(reportId)
                .orElseThrow(() ->
                        new DiffAnalysisException("Report not found: " + reportId, "unknown", "unknown", "lookup"));

        // Re-run analysis with same parameters
        BaseVersion baseVersion = baseVersionResolver.resolve(
                existing.getRepository(), existing.getSpecPath(),
                existing.getTargetVersion(), existing.getBaseVersion());

        BreakingChangeReport newReport = diffOrchestrator.analyzeWithBase(
                existing.getRepository(), existing.getSpecPath(), existing.getTargetSpecId(), baseVersion);

        return AnalysisResponse.from(newReport);
    }
}
