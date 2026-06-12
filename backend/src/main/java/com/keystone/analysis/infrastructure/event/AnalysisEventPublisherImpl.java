package com.keystone.analysis.infrastructure.event;

import com.keystone.analysis.domain.event.BreakingChangeReportedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Default implementation of {@link AnalysisEventPublisher}.
 *
 * <p>Uses Spring's {@link ApplicationEventPublisher} for in-process
 * event distribution. This is the same pattern used by the
 * contract-ingestion module and aligns with ADR-003.
 *
 * <p>Events are published synchronously. For async delivery, configure
 * Spring's {@code @Async} on the listener or swap to a message broker
 * (Redis Streams / Kafka) in a future iteration.
 */
@Component
public class AnalysisEventPublisherImpl implements AnalysisEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(AnalysisEventPublisherImpl.class);

    private final ApplicationEventPublisher springEventPublisher;

    public AnalysisEventPublisherImpl(ApplicationEventPublisher springEventPublisher) {
        this.springEventPublisher = springEventPublisher;
    }

    @Override
    public void breakingChangeReported(BreakingChangeReportedEvent event) {
        log.info("Publishing BreakingChangeReportedEvent: reportId={}, repository={}/{}, verdict={}",
                event.reportId(), event.repository(), event.specPath(), event.verdict());
        springEventPublisher.publishEvent(event);
    }
}
