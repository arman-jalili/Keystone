// Canonical Reference: .pi/architecture/modules/notification-engine.md
package com.keystone.notification.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.keystone.notification.application.dto.ChannelStatusResponse;
import com.keystone.notification.application.dto.DispatchNotificationRequest;
import com.keystone.notification.application.dto.NotificationResponse;
import com.keystone.notification.domain.event.NotificationSentEvent;
import com.keystone.notification.domain.model.Notification;
import com.keystone.notification.domain.model.NotificationStatus;
import com.keystone.notification.infrastructure.event.NotificationEventPublisherImpl;
import com.keystone.notification.infrastructure.repository.NotificationRepositoryImpl;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
        classes = {
            NotificationDispatcherImpl.class,
            NotificationRepositoryImpl.class,
            NotificationEventPublisherImpl.class,
            com.keystone.notification.domain.service.ChannelRegistryImpl.class,
            com.keystone.notification.domain.channel.TestCiStatusChannel.class,
            com.keystone.notification.domain.channel.TestEmailChannel.class,
            NotificationDispatcherIntegrationTest.TestConfig.class
        })
@ActiveProfiles("test")
class NotificationDispatcherIntegrationTest {

    @Autowired
    private NotificationDispatcher dispatcher;

    @Autowired
    private NotificationRepositoryImpl repository;

    @Autowired
    private TestNotificationListener listener;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        listener.events.clear();
        // Clear repository state between tests
        repository.deleteOlderThan(Instant.parse("2030-01-01T00:00:00Z"));
    }

    @Test
    void dispatch_shouldDeliverToAllRegisteredChannels() {
        Object event = new Object();

        List<NotificationResponse> responses = dispatcher.dispatch(event);

        assertThat(responses).hasSize(2);
        assertThat(responses).allMatch(r -> r.status().equals("DELIVERED"));
        assertThat(responses)
                .extracting(NotificationResponse::channelName)
                .containsExactlyInAnyOrder("CI_STATUS", "EMAIL");
    }

    @Test
    void dispatch_shouldPersistNotifications() {
        Object event = new Object();

        dispatcher.dispatch(event);

        assertThat(repository.count()).isEqualTo(2);
        List<Notification> allNotifications = repository.findByChannelName("CI_STATUS", 10);
        assertThat(allNotifications).isNotEmpty();
        assertThat(allNotifications.get(0).status()).isEqualTo(NotificationStatus.DELIVERED);
    }

    @Test
    void dispatch_shouldPublishEvents() {
        Object event = new Object();

        dispatcher.dispatch(event);

        assertThat(listener.events).hasSize(2);
        assertThat(listener.events.get(0).getEventType()).isEqualTo("NotificationSent");
        assertThat(listener.events.get(0).getSource()).isEqualTo("notification-engine");
    }

    @Test
    void dispatchRequest_shouldHandleProgrammaticDispatch() throws Exception {
        String payload = "{\"type\":\"test\",\"value\":42}";
        DispatchNotificationRequest request = new DispatchNotificationRequest("TestEvent", payload, null, "idem-1");

        List<NotificationResponse> responses = dispatcher.dispatchRequest(request);

        assertThat(responses).hasSize(2);
        assertThat(responses).allMatch(r -> r.status().equals("DELIVERED"));
    }

    @Test
    void dispatchToChannel_shouldTargetSpecificChannel() {
        String payload = "{\"type\":\"test\"}";

        NotificationResponse response = dispatcher.dispatchToChannel("EMAIL", payload);

        assertThat(response.channelName()).isEqualTo("EMAIL");
        assertThat(response.status()).isEqualTo("DELIVERED");
    }

    @Test
    void getChannelStatus_shouldShowRegisteredChannels() {
        ChannelStatusResponse status = dispatcher.getChannelStatus();

        assertThat(status.total()).isEqualTo(2);
        assertThat(status.available()).isEqualTo(2);
        assertThat(status.channels()).extracting(cs -> cs.name()).containsExactlyInAnyOrder("CI_STATUS", "EMAIL");
    }

    @Test
    void getNotificationStatus_shouldReturnPersistedNotification() {
        Object event = new Object();
        List<NotificationResponse> responses = dispatcher.dispatch(event);
        UUID notificationId = responses.get(0).notificationId();

        NotificationResponse retrieved =
                dispatcher.getNotificationStatus(notificationId).orElseThrow();

        assertThat(retrieved.notificationId()).isEqualTo(notificationId);
        assertThat(retrieved.status()).isEqualTo("DELIVERED");
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        TestNotificationListener testNotificationListener() {
            return new TestNotificationListener();
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        Clock clock() {
            return Clock.fixed(Instant.parse("2026-06-12T12:00:00Z"), ZoneId.of("UTC"));
        }
    }

    @Component
    static class TestNotificationListener {
        final List<NotificationSentEvent> events = new CopyOnWriteArrayList<>();

        @EventListener
        public void handleNotificationSent(NotificationSentEvent event) {
            events.add(event);
        }
    }
}
