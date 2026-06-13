// Canonical Reference: .pi/architecture/modules/notification-engine.md
package com.keystone.notification.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.keystone.notification.application.dto.ChannelStatusResponse;
import com.keystone.notification.application.dto.DispatchNotificationRequest;
import com.keystone.notification.application.dto.NotificationResponse;
import com.keystone.notification.domain.channel.NotificationChannel;
import com.keystone.notification.domain.exception.NotificationDeliveryException;
import com.keystone.notification.domain.model.Notification;
import com.keystone.notification.domain.model.NotificationStatus;
import com.keystone.notification.domain.service.ChannelRegistry;
import com.keystone.notification.infrastructure.event.NotificationEventPublisher;
import com.keystone.notification.infrastructure.repository.NotificationRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationDispatcherImplTest {

    private static final Instant NOW = Instant.parse("2026-06-12T12:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(NOW, ZoneId.of("UTC"));

    @Mock
    private ChannelRegistry channelRegistry;

    @Mock
    private NotificationChannel channel1;

    @Mock
    private NotificationChannel channel2;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationEventPublisher eventPublisher;

    @Captor
    private ArgumentCaptor<Notification> notificationCaptor;

    private ObjectMapper objectMapper;
    private NotificationDispatcherImpl dispatcher;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        lenient().when(channel1.getName()).thenReturn("CI_STATUS");
        lenient().when(channel2.getName()).thenReturn("EMAIL");
        dispatcher = new NotificationDispatcherImpl(
                channelRegistry, notificationRepository, eventPublisher, objectMapper, FIXED_CLOCK);
    }

    @Test
    void dispatch_shouldSendToAllRegisteredChannels() {
        Object event = new Object();
        when(channelRegistry.getAllChannels()).thenReturn(List.of(channel1, channel2));
        when(channel1.isAvailable()).thenReturn(true);
        when(channel2.isAvailable()).thenReturn(true);
        when(channel1.send(event)).thenReturn(createDeliveredNotification("CI_STATUS", "sha123"));
        when(channel2.send(event)).thenReturn(createDeliveredNotification("EMAIL", "user@example.com"));
        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        List<NotificationResponse> responses = dispatcher.dispatch(event);

        assertThat(responses).hasSize(2);
        verify(channel1).send(event);
        verify(channel2).send(event);
        verify(eventPublisher, times(2)).notificationSent(any());
    }

    @Test
    void dispatch_shouldReturnEmptyListWhenNoChannels() {
        when(channelRegistry.getAllChannels()).thenReturn(List.of());

        List<NotificationResponse> responses = dispatcher.dispatch(new Object());

        assertThat(responses).isEmpty();
    }

    @Test
    void dispatchToChannel_shouldSendToNamedChannel() {
        Object event = new Object();
        when(channelRegistry.getChannel("CI_STATUS")).thenReturn(Optional.of(channel1));
        when(channel1.isAvailable()).thenReturn(true);
        when(channel1.send(event)).thenReturn(createDeliveredNotification("CI_STATUS", "sha456"));
        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        NotificationResponse response = dispatcher.dispatchToChannel("CI_STATUS", event);

        assertThat(response.channelName()).isEqualTo("CI_STATUS");
        assertThat(response.status()).isEqualTo("DELIVERED");
        verify(channel1).send(event);
    }

    @Test
    void dispatchToChannel_shouldThrowWhenChannelNotFound() {
        when(channelRegistry.getChannel("NONEXISTENT")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dispatcher.dispatchToChannel("NONEXISTENT", new Object()))
                .isInstanceOf(NotificationDeliveryException.class)
                .hasMessageContaining("Channel not found");
    }

    @Test
    void dispatch_shouldSkipUnavailableChannel() {
        Object event = new Object();
        when(channelRegistry.getAllChannels()).thenReturn(List.of(channel1, channel2));
        when(channel1.isAvailable()).thenReturn(false);
        when(channel2.isAvailable()).thenReturn(true);
        when(channel2.send(event)).thenReturn(createDeliveredNotification("EMAIL", "user@test.com"));
        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        List<NotificationResponse> responses = dispatcher.dispatch(event);

        assertThat(responses).hasSize(2);
        NotificationResponse failedResponse = responses.stream()
                .filter(r -> r.channelName().equals("CI_STATUS"))
                .findFirst()
                .orElseThrow();
        assertThat(failedResponse.status()).isEqualTo("FAILED");
        assertThat(failedResponse.message()).contains("not available");

        NotificationResponse deliveredResponse = responses.stream()
                .filter(r -> r.channelName().equals("EMAIL"))
                .findFirst()
                .orElseThrow();
        assertThat(deliveredResponse.status()).isEqualTo("DELIVERED");
    }

    @Test
    void dispatch_shouldRetryOnFailure() {
        Object event = new Object();
        when(channelRegistry.getAllChannels()).thenReturn(List.of(channel1));
        when(channel1.isAvailable()).thenReturn(true);
        Notification failed = new Notification(
                UUID.randomUUID(), "CI_STATUS", null, NotificationStatus.FAILED, "Service unavailable", "test", NOW);
        Notification delivered = createDeliveredNotification("CI_STATUS", "sha789");
        when(channel1.send(event))
                .thenThrow(new RuntimeException("Connection timeout"))
                .thenReturn(delivered);
        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        List<NotificationResponse> responses = dispatcher.dispatch(event);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).status()).isEqualTo("DELIVERED");
        verify(channel1, times(2)).send(event);
    }

    @Test
    void dispatch_shouldFailAfterMaxRetries() {
        Object event = new Object();
        when(channelRegistry.getAllChannels()).thenReturn(List.of(channel1));
        when(channel1.isAvailable()).thenReturn(true);
        when(channel1.send(event)).thenThrow(new RuntimeException("Persistent failure"));
        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        List<NotificationResponse> responses = dispatcher.dispatch(event);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).status()).isEqualTo("FAILED");
        verify(channel1, times(NotificationDispatcherImpl.MAX_RETRIES + 1)).send(event);
    }

    @Test
    void dispatchRequest_shouldParsePayloadAndDispatch() throws Exception {
        String payload = "{\"key\":\"value\"}";
        DispatchNotificationRequest request = new DispatchNotificationRequest("TestEvent", payload, null, null);
        when(channelRegistry.getAllChannels()).thenReturn(List.of(channel1));
        when(channel1.isAvailable()).thenReturn(true);
        when(channel1.send(any())).thenReturn(createDeliveredNotification("CI_STATUS", "sha"));
        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        List<NotificationResponse> responses = dispatcher.dispatchRequest(request);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).status()).isEqualTo("DELIVERED");
    }

    @Test
    void dispatchRequest_shouldThrowForInvalidPayload() {
        DispatchNotificationRequest request =
                new DispatchNotificationRequest("TestEvent", "not valid json{{{", null, null);

        assertThatThrownBy(() -> dispatcher.dispatchRequest(request))
                .isInstanceOf(NotificationDeliveryException.class)
                .hasMessageContaining("Failed to parse event payload");
    }

    @Test
    void dispatchRequest_shouldTargetSpecificChannel() throws Exception {
        String payload = "{\"key\":\"value\"}";
        DispatchNotificationRequest request = new DispatchNotificationRequest("TestEvent", payload, "CI_STATUS", null);
        when(channelRegistry.getChannel("CI_STATUS")).thenReturn(Optional.of(channel1));
        when(channel1.isAvailable()).thenReturn(true);
        when(channel1.send(any())).thenReturn(createDeliveredNotification("CI_STATUS", "sha"));
        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        List<NotificationResponse> responses = dispatcher.dispatchRequest(request);

        assertThat(responses).hasSize(1);
        verify(channel1).send(any());
        verifyNoInteractions(channel2);
    }

    @Test
    void getNotificationStatus_shouldReturnResponse() {
        UUID id = UUID.randomUUID();
        Notification notification = createDeliveredNotification("CI_STATUS", "sha");
        when(notificationRepository.findById(id)).thenReturn(Optional.of(notification));

        Optional<NotificationResponse> result = dispatcher.getNotificationStatus(id);

        assertThat(result).isPresent();
        assertThat(result.get().notificationId()).isEqualTo(notification.id());
        assertThat(result.get().status()).isEqualTo("DELIVERED");
    }

    @Test
    void getNotificationStatus_shouldReturnEmptyWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(notificationRepository.findById(id)).thenReturn(Optional.empty());

        Optional<NotificationResponse> result = dispatcher.getNotificationStatus(id);

        assertThat(result).isEmpty();
    }

    @Test
    void getChannelStatus_shouldReturnAllChannels() {
        when(channelRegistry.getAllChannels()).thenReturn(List.of(channel1, channel2));
        when(channel1.isAvailable()).thenReturn(true);
        when(channel2.isAvailable()).thenReturn(false);

        ChannelStatusResponse status = dispatcher.getChannelStatus();

        assertThat(status.total()).isEqualTo(2);
        assertThat(status.available()).isEqualTo(1);
        assertThat(status.channels()).hasSize(2);
        assertThat(status.channels().get(0).name()).isEqualTo("CI_STATUS");
        assertThat(status.channels().get(0).available()).isTrue();
        assertThat(status.channels().get(1).available()).isFalse();
    }

    @Test
    void dispatch_shouldPersistAndPublishOnDelivery() {
        Object event = new Object();
        when(channelRegistry.getAllChannels()).thenReturn(List.of(channel1));
        when(channel1.isAvailable()).thenReturn(true);
        Notification delivered = createDeliveredNotification("CI_STATUS", "sha");
        when(channel1.send(event)).thenReturn(delivered);
        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        dispatcher.dispatch(event);

        verify(notificationRepository).save(any());
        verify(eventPublisher).notificationSent(any());
    }

    @Test
    void dispatch_shouldContinueWhenOneChannelFails() {
        Object event = new Object();
        when(channelRegistry.getAllChannels()).thenReturn(List.of(channel1, channel2));
        when(channel1.isAvailable()).thenReturn(true);
        when(channel2.isAvailable()).thenReturn(true);
        when(channel1.send(event)).thenReturn(createDeliveredNotification("CI_STATUS", "sha"));
        when(channel2.send(event)).thenThrow(new RuntimeException("Channel 2 failed"));
        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        List<NotificationResponse> responses = dispatcher.dispatch(event);

        assertThat(responses).hasSize(2);
        Optional<NotificationResponse> failed =
                responses.stream().filter(r -> r.status().equals("FAILED")).findFirst();
        assertThat(failed).isPresent();
        assertThat(failed.get().channelName()).isEqualTo("EMAIL");

        Optional<NotificationResponse> delivered =
                responses.stream().filter(r -> r.status().equals("DELIVERED")).findFirst();
        assertThat(delivered).isPresent();
        assertThat(delivered.get().channelName()).isEqualTo("CI_STATUS");
    }

    // ---- Helpers ----

    private Notification createDeliveredNotification(String channelName, String channelId) {
        return new Notification(
                UUID.randomUUID(), channelName, channelId, NotificationStatus.DELIVERED, "Delivered", "test", NOW);
    }
}
