// Canonical Reference: .pi/architecture/modules/breaking-change-analysis.md
// Module: breaking-change-analysis
package com.keystone.analysis.infrastructure.event;

import com.keystone.analysis.application.service.BreakingAnalysisService;
import com.keystone.analysis.domain.service.DiffOrchestrator;
import com.keystone.ingestion.domain.event.SpecIngestedEvent;
import com.keystone.ingestion.infrastructure.repository.SpecRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Listens for {@link SpecIngestedEvent} from the contract-ingestion module
 * and triggers breaking change analysis automatically.
 *
 * <p>Per ADR-003 and the architecture, this is the primary trigger for
 * diff analysis. When a new spec is ingested, the analysis pipeline
 * runs asynchronously and publishes results via
 * {@link com.keystone.analysis.infrastructure.event.AnalysisEventPublisher}.
 */
@Component
public class SpecIngestionEventListener {

    private static final Logger log = LoggerFactory.getLogger(SpecIngestionEventListener.class);

    private final BreakingAnalysisService breakingAnalysisService;
    private final DiffOrchestrator diffOrchestrator;
    private final SpecRepository specRepository;

    public SpecIngestionEventListener(
            BreakingAnalysisService breakingAnalysisService,
            DiffOrchestrator diffOrchestrator,
            SpecRepository specRepository) {
        this.breakingAnalysisService = breakingAnalysisService;
        this.diffOrchestrator = diffOrchestrator;
        this.specRepository = specRepository;
    }

    /**
     * Triggered when a new spec is ingested.
     *
     * <p>The analysis runs synchronously within the event publisher's thread.
     * For production, consider moving this to an async executor or queue.
     *
     * @param event the spec ingested event from contract-ingestion
     */
    @EventListener
    public void onSpecIngested(SpecIngestedEvent event) {
        log.info(
                "Received SpecIngestedEvent: repository={}, specPath={}, commitSha={}, specId={}",
                event.repository(),
                event.specPath(),
                event.commitSha(),
                event.specId());

        try {
            // Use the real spec UUID from the ingested event rather than a deterministic hash.
            // The specId uniquely identifies the OpenApiSpec record in the ingestion context.
            diffOrchestrator.analyze(event.repository(), event.specPath(), event.specId());
            log.info("Auto-analysis completed for {}/{}", event.repository(), event.specPath());
        } catch (Exception e) {
            log.error("Auto-analysis failed for {}/{}: {}", event.repository(), event.specPath(), e.getMessage(), e);
        }
    }
}
