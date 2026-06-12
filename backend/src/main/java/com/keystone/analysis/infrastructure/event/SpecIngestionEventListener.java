package com.keystone.analysis.infrastructure.event;

import com.keystone.analysis.application.service.BreakingAnalysisService;
import com.keystone.analysis.domain.service.DiffOrchestrator;
import com.keystone.ingestion.domain.event.SpecIngestedEvent;
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

    public SpecIngestionEventListener(BreakingAnalysisService breakingAnalysisService,
                                       DiffOrchestrator diffOrchestrator) {
        this.breakingAnalysisService = breakingAnalysisService;
        this.diffOrchestrator = diffOrchestrator;
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
        log.info("Received SpecIngestedEvent: repository={}, specPath={}, commitSha={}",
                event.repository(), event.specPath(), event.commitSha());

        try {
            // TODO: In a full implementation, retrieve the target SpecVersion by specVersionId
            // For now, use a deterministic UUID based on the event data
            java.util.UUID targetSpecId = java.util.UUID.nameUUIDFromBytes(
                    (event.repository() + ":" + event.specPath() + ":" + event.commitSha()).getBytes());

            diffOrchestrator.analyze(event.repository(), event.specPath(), targetSpecId);
            log.info("Auto-analysis completed for {}/{}", event.repository(), event.specPath());
        } catch (Exception e) {
            log.error("Auto-analysis failed for {}/{}: {}",
                    event.repository(), event.specPath(), e.getMessage(), e);
        }
    }
}
