package com.keystone.graph.infrastructure.event;

import com.keystone.graph.domain.event.DependencyAddedEvent;
import com.keystone.graph.domain.event.DownstreamImpactComputedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Spring-based implementation of {@link GraphEventPublisher}.
 *
 * <p>Uses Spring's {@link ApplicationEventPublisher} for in-process event
 * delivery. Events are published synchronously by default but can be
 * made asynchronous by adding {@code @Async} to the publisher methods.
 *
 * <p>Per ADR-003, this can be swapped for Redis Streams or Kafka later
 * without changing the interface contract.
 */
@Component
public class GraphEventPublisherImpl implements GraphEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(GraphEventPublisherImpl.class);

    private final ApplicationEventPublisher publisher;

    public GraphEventPublisherImpl(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public void dependencyAdded(DependencyAddedEvent event) {
        log.info("Publishing DependencyAddedEvent for service '{}'", event.serviceName());
        publisher.publishEvent(event);
    }

    @Override
    public void downstreamImpactComputed(DownstreamImpactComputedEvent event) {
        log.info(
                "Publishing DownstreamImpactComputedEvent for report '{}': {} affected services",
                event.reportId(),
                event.totalAffected());
        publisher.publishEvent(event);
    }
}
