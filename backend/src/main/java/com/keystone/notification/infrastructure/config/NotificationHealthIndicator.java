package com.keystone.notification.infrastructure.config;

import com.keystone.notification.domain.service.ChannelRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Health indicator for the Notification Engine module.
 *
 * <p>Reports:
 * <ul>
 *   <li><b>UP</b> — At least one notification channel is registered</li>
 *   <li><b>DEGRADED</b> — Channels registered but none available</li>
 *   <li><b>UNKNOWN</b> — No channels registered</li>
 * </ul>
 *
 * <p>Exposed via {@code /actuator/health} as {@code notificationEngine}.
 */
@Component
public class NotificationHealthIndicator implements HealthIndicator {

    private static final Logger log = LoggerFactory.getLogger(NotificationHealthIndicator.class);

    private final ChannelRegistry channelRegistry;

    public NotificationHealthIndicator(ChannelRegistry channelRegistry) {
        this.channelRegistry = channelRegistry;
    }

    @Override
    public Health health() {
        int totalChannels = channelRegistry.channelCount();
        int availableChannels = (int) channelRegistry.getAllChannels().stream()
                .filter(c -> c.isAvailable())
                .count();

        Health.Builder builder;
        if (totalChannels == 0) {
            builder = Health.unknown();
        } else if (availableChannels == 0) {
            builder = Health.down();
        } else if (availableChannels < totalChannels) {
            builder = Health.up();
        } else {
            builder = Health.up();
        }

        return builder.withDetail("totalChannels", totalChannels)
                .withDetail("availableChannels", availableChannels)
                .withDetail("channels", channelRegistry.getChannelNames())
                .build();
    }
}
