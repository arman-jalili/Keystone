package com.keystone.ingestion.infrastructure.event;

import com.keystone.ingestion.domain.event.SpecIngestedEvent;
import com.keystone.ingestion.domain.event.SpecParseFailedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Publishes ingestion domain events via Spring's {@link ApplicationEventPublisher}.
 *
 * <p>Per ADR-003, this is the in-process event bus implementation.
 * Can be swapped for Redis Streams or Kafka later without changing callers.
 */
@Component
public class IngestionEventPublisherImpl implements IngestionEventPublisher {

    private final ApplicationEventPublisher publisher;

    public IngestionEventPublisherImpl(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public void specIngested(SpecIngestedEvent event) {
        publisher.publishEvent(event);
    }

    @Override
    public void specParseFailed(SpecParseFailedEvent event) {
        publisher.publishEvent(event);
    }
}
