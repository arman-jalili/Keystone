package com.keystone.notification.domain.channel;

import com.keystone.notification.domain.model.Notification;
import com.keystone.notification.domain.model.NotificationStatus;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Test implementation of {@link NotificationChannel} simulating a CI status channel.
 *
 * <p>Always returns DELIVERED status. Used for development and integration testing.
 * Will be replaced by the real CI status channel implementation.
 */
@Component
public class TestCiStatusChannel implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(TestCiStatusChannel.class);

    @Override
    public String getName() {
        return "CI_STATUS";
    }

    @Override
    public Notification send(Object event) {
        log.debug("TestCiStatusChannel delivering event: {}", event.getClass().getSimpleName());
        return new Notification(
                UUID.randomUUID(),
                getName(),
                "test-sha-" + System.currentTimeMillis(),
                NotificationStatus.DELIVERED,
                "Test delivery for " + event.getClass().getSimpleName(),
                event.getClass().getSimpleName(),
                Instant.now());
    }

    @Override
    public boolean isAvailable() {
        return true;
    }
}
