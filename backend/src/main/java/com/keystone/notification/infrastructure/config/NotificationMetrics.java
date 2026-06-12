package com.keystone.notification.infrastructure.config;

import com.keystone.notification.domain.event.NotificationDeliveryFailedEvent;
import com.keystone.notification.domain.event.NotificationSentEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Micrometer metrics collector for the Notification Engine module.
 *
 * <p>Listens to domain events and records the following metrics:
 * <ul>
 *   <li>{@code notification.dispatched} — Counter, total events dispatched</li>
 *   <li>{@code notification.delivered} — Counter, successful deliveries by channel</li>
 *   <li>{@code notification.failed} — Counter, failed deliveries by channel</li>
 *   <li>{@code notification.delivery.time} — Timer, delivery duration</li>
 * </ul>
 */
@Component
public class NotificationMetrics {

    private static final Logger log = LoggerFactory.getLogger(NotificationMetrics.class);

    private final MeterRegistry meterRegistry;

    private Counter dispatchedCounter;
    private Counter deliveredCounter;
    private Counter failedCounter;
    private Timer deliveryTimer;

    public NotificationMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    void init() {
        dispatchedCounter = Counter.builder("notification.dispatched")
                .description("Total events dispatched")
                .register(meterRegistry);

        deliveredCounter = Counter.builder("notification.delivered")
                .description("Successful deliveries")
                .tag("channel", "all")
                .register(meterRegistry);

        failedCounter = Counter.builder("notification.failed")
                .description("Failed deliveries")
                .tag("channel", "all")
                .register(meterRegistry);

        deliveryTimer = Timer.builder("notification.delivery.time")
                .description("Delivery duration")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);

        log.info("Notification metrics registered");
    }

    @EventListener
    public void onNotificationSent(NotificationSentEvent event) {
        dispatchedCounter.increment();

        Counter.builder("notification.delivered")
                .description("Successful deliveries")
                .tag("channel", event.getPayload().channelName())
                .register(meterRegistry)
                .increment();

        deliveredCounter.increment();
    }

    @EventListener
    public void onNotificationDeliveryFailed(NotificationDeliveryFailedEvent event) {
        Counter.builder("notification.failed")
                .description("Failed deliveries by channel")
                .tag("channel", event.getPayload().channelName())
                .tag("reason", event.getPayload().errorMessage())
                .register(meterRegistry)
                .increment();

        failedCounter.increment();
    }

    /**
     * Records delivery duration for a specific channel.
     *
     * @param channelName the channel name
     * @param durationMs the delivery duration in milliseconds
     */
    public void recordDeliveryTime(String channelName, long durationMs) {
        Timer timer = Timer.builder("notification.delivery.time")
                .description("Delivery duration by channel")
                .tag("channel", channelName)
                .register(meterRegistry);
        timer.record(durationMs, TimeUnit.MILLISECONDS);
        deliveryTimer.record(durationMs, TimeUnit.MILLISECONDS);
    }
}
