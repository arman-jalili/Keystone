// Canonical Reference: .pi/architecture/modules/notification-engine.md
// Module: notification-engine
package com.keystone.notification.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.keystone.notification.application.dto.ChannelStatusResponse;
import com.keystone.notification.application.dto.DispatchNotificationRequest;
import com.keystone.notification.application.dto.NotificationResponse;
import com.keystone.notification.domain.channel.NotificationChannel;
import com.keystone.notification.domain.event.NotificationDeliveryFailedEvent;
import com.keystone.notification.domain.event.NotificationSentEvent;
import com.keystone.notification.domain.exception.NotificationDeliveryException;
import com.keystone.notification.domain.model.Notification;
import com.keystone.notification.domain.model.NotificationStatus;
import com.keystone.notification.domain.service.ChannelRegistry;
import com.keystone.notification.infrastructure.event.NotificationEventPublisher;
import com.keystone.notification.infrastructure.repository.NotificationRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Default implementation of {@link NotificationDispatcher}.
 *
 * <p>Dispatches events to registered notification channels with parallel
 * execution, retry logic with exponential backoff, and event publishing.
 *
 * <p>Dispatch flow:
 * <ol>
 *   <li>Resolve target channel(s) from {@link ChannelRegistry}</li>
 *   <li>Invoke each channel's {@code send()} method in parallel</li>
 *   <li>Persist the resulting {@link Notification} records</li>
 *   <li>Publish {@link NotificationSentEvent} or {@link NotificationDeliveryFailedEvent}</li>
 *   <li>Aggregate and return {@link NotificationResponse} list</li>
 * </ol>
 */
@Service
public class NotificationDispatcherImpl implements NotificationDispatcher {

    private static final Logger log = LoggerFactory.getLogger(NotificationDispatcherImpl.class);

    static final int MAX_RETRIES = 3;
    static final long[] BACKOFF_MS = {1_000L, 4_000L, 10_000L};

    private final ChannelRegistry channelRegistry;
    private final NotificationRepository notificationRepository;
    private final NotificationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final Executor executor;

    public NotificationDispatcherImpl(
            ChannelRegistry channelRegistry,
            NotificationRepository notificationRepository,
            NotificationEventPublisher eventPublisher,
            ObjectMapper objectMapper,
            Clock clock) {
        this.channelRegistry = channelRegistry;
        this.notificationRepository = notificationRepository;
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
    }

    @Override
    public List<NotificationResponse> dispatch(Object event) {
        log.debug("Dispatching event to all channels: {}", event.getClass().getSimpleName());

        List<NotificationChannel> channels = channelRegistry.getAllChannels();
        if (channels.isEmpty()) {
            log.warn("No channels registered for dispatch");
            return List.of();
        }

        List<CompletableFuture<NotificationResponse>> futures = new ArrayList<>();
        for (NotificationChannel channel : channels) {
            futures.add(CompletableFuture.supplyAsync(() -> dispatchToChannel(channel, event), executor)
                    .exceptionally(ex -> {
                        log.error(
                                "Unhandled error dispatching to channel {}: {}",
                                channel.getName(),
                                ex.getMessage(),
                                ex);
                        return buildFailedResponse(channel.getName(), ex.getMessage());
                    }));
        }

        return futures.stream().map(CompletableFuture::join).toList();
    }

    @Override
    public NotificationResponse dispatchToChannel(String channelName, Object event)
            throws NotificationDeliveryException {
        NotificationChannel channel = channelRegistry
                .getChannel(channelName)
                .orElseThrow(() -> new NotificationDeliveryException(channelName, "Channel not found", 0));
        return dispatchToChannel(channel, event);
    }

    @Override
    public List<NotificationResponse> dispatchRequest(DispatchNotificationRequest request)
            throws NotificationDeliveryException {
        Object event;
        try {
            event = objectMapper.readValue(request.eventPayload(), Object.class);
        } catch (JsonProcessingException e) {
            throw new NotificationDeliveryException(
                    "dispatch", "Failed to parse event payload: " + e.getMessage(), 0, e);
        }

        if (request.hasTargetChannel()) {
            return List.of(dispatchToChannel(request.channelName(), event));
        }
        return dispatch(event);
    }

    @Override
    public Optional<NotificationResponse> getNotificationStatus(UUID notificationId) {
        return notificationRepository.findById(notificationId).map(NotificationResponse::from);
    }

    @Override
    public ChannelStatusResponse getChannelStatus() {
        List<NotificationChannel> channels = channelRegistry.getAllChannels();
        List<ChannelStatusResponse.ChannelStatusEntry> entries = channels.stream()
                .map(c -> new ChannelStatusResponse.ChannelStatusEntry(c.getName(), c.isAvailable()))
                .toList();
        return new ChannelStatusResponse(entries, entries.size(), (int) entries.stream()
                .filter(ChannelStatusResponse.ChannelStatusEntry::available)
                .count());
    }

    // ---- Private helpers ----

    private NotificationResponse dispatchToChannel(NotificationChannel channel, Object event) {
        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            try {
                if (!channel.isAvailable()) {
                    log.warn("Channel {} is not available, skipping", channel.getName());
                    return buildFailedResponse(channel.getName(), "Channel not available");
                }

                Notification notification = channel.send(event);
                Notification saved = notificationRepository.save(notification);

                if (notification.isDelivered()) {
                    publishSentEvent(saved);
                    log.info("Delivered via channel {}: {}", channel.getName(), saved.id());
                } else if (notification.isFailed() && attempt < MAX_RETRIES) {
                    log.warn("Retry {} for channel {}: {}", attempt + 1, channel.getName(), notification.message());
                    sleep(BACKOFF_MS[attempt]);
                    continue;
                } else if (notification.isFailed()) {
                    publishFailedEvent(channel.getName(), event, notification.message(), attempt);
                    log.error(
                            "Delivery failed after {} retries via channel {}: {}",
                            attempt,
                            channel.getName(),
                            notification.message());
                }

                return NotificationResponse.from(saved);

            } catch (Exception e) {
                if (attempt < MAX_RETRIES) {
                    log.warn(
                            "Retry {} for channel {} due to error: {}", attempt + 1, channel.getName(), e.getMessage());
                    sleep(BACKOFF_MS[attempt]);
                } else {
                    publishFailedEvent(channel.getName(), event, e.getMessage(), attempt);
                    log.error(
                            "Delivery failed after {} retries via channel {}: {}",
                            attempt,
                            channel.getName(),
                            e.getMessage(),
                            e);
                    return buildFailedResponse(channel.getName(), e.getMessage());
                }
            }
        }

        return buildFailedResponse(channel.getName(), "Max retries exceeded");
    }

    private void publishSentEvent(Notification notification) {
        try {
            NotificationSentEvent event = new NotificationSentEvent(
                    UUID.randomUUID(),
                    notification,
                    clock.instant(),
                    notification.id().toString());
            eventPublisher.notificationSent(event);
        } catch (Exception e) {
            log.warn("Failed to publish NotificationSentEvent: {}", e.getMessage());
        }
    }

    private void publishFailedEvent(String channelName, Object event, String errorMessage, int retryCount) {
        try {
            NotificationDeliveryFailedEvent failedEvent = new NotificationDeliveryFailedEvent(
                    UUID.randomUUID(),
                    channelName,
                    event.getClass().getSimpleName(),
                    errorMessage,
                    retryCount,
                    true,
                    clock.instant(),
                    UUID.randomUUID().toString());
            eventPublisher.notificationDeliveryFailed(failedEvent);
        } catch (Exception e) {
            log.warn("Failed to publish NotificationDeliveryFailedEvent: {}", e.getMessage());
        }
    }

    private NotificationResponse buildFailedResponse(String channelName, String errorMessage) {
        Notification notification = new Notification(
                UUID.randomUUID(),
                channelName,
                null,
                NotificationStatus.FAILED,
                errorMessage,
                "unknown",
                clock.instant());
        Notification saved = notificationRepository.save(notification);
        return NotificationResponse.from(saved);
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
