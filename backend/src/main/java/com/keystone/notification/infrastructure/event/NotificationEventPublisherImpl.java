package com.keystone.notification.infrastructure.event;

import com.keystone.notification.domain.event.NotificationDeliveryFailedEvent;
import com.keystone.notification.domain.event.NotificationSentEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Default implementation of {@link NotificationEventPublisher} using Spring's
 * {@link ApplicationEventPublisher}.
 *
 * <p>Publishes events synchronously via the in-process Spring event bus.
 * Can be swapped for Redis Streams or Kafka later without affecting callers.
 */
@Component
public class NotificationEventPublisherImpl implements NotificationEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventPublisherImpl.class);

    private final ApplicationEventPublisher applicationEventPublisher;

    public NotificationEventPublisherImpl(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public void notificationSent(NotificationSentEvent event) {
        log.debug(
                "Publishing NotificationSentEvent: {} via {}",
                event.getEventId(),
                event.getPayload().channelName());
        applicationEventPublisher.publishEvent(event);
    }

    @Override
    public void notificationDeliveryFailed(NotificationDeliveryFailedEvent event) {
        log.warn(
                "Publishing NotificationDeliveryFailedEvent: {} for channel {}",
                event.getEventId(),
                event.getPayload().channelName());
        applicationEventPublisher.publishEvent(event);
    }
}
