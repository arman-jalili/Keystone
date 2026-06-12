package com.keystone.notification.domain.channel;

import com.keystone.notification.domain.model.Notification;
import com.keystone.notification.domain.model.NotificationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Test implementation of {@link NotificationChannel} simulating an email channel.
 *
 * <p>Always returns DELIVERED status. Used for development and integration testing.
 * Will be replaced by the real email notification channel implementation.
 */
@Component
public class TestEmailChannel implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(TestEmailChannel.class);

    @Override
    public String getName() {
        return "EMAIL";
    }

    @Override
    public Notification send(Object event) {
        log.debug("TestEmailChannel delivering event: {}", event.getClass().getSimpleName());
        return new Notification(
                UUID.randomUUID(),
                getName(),
                "test-email-" + System.currentTimeMillis(),
                NotificationStatus.DELIVERED,
                "Test email delivery for " + event.getClass().getSimpleName(),
                event.getClass().getSimpleName(),
                Instant.now());
    }

    @Override
    public boolean isAvailable() {
        return true;
    }
}
