package com.keystone.policy.infrastructure.event;

import com.keystone.policy.domain.event.PolicyEvaluatedEvent;
import com.keystone.policy.domain.event.PolicySourceChangedEvent;
import com.keystone.policy.domain.event.PolicySyncedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Publishes policy domain events via Spring's {@link ApplicationEventPublisher}.
 *
 * <p>Per the event-driven architecture, this is the in-process event bus implementation.
 * Can be swapped for Redis Streams or Kafka later without changing callers.
 */
@Component
public class PolicyEventPublisherImpl implements PolicyEventPublisher {

    private final ApplicationEventPublisher publisher;

    public PolicyEventPublisherImpl(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public void policyEvaluated(PolicyEvaluatedEvent event) {
        publisher.publishEvent(event);
    }

    @Override
    public void policySynced(PolicySyncedEvent event) {
        publisher.publishEvent(event);
    }

    @Override
    public void policySourceChanged(PolicySourceChangedEvent event) {
        publisher.publishEvent(event);
    }
}
