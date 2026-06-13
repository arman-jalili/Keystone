// Canonical Reference: .pi/architecture/modules/contract-ingestion.md
package com.keystone.ingestion.infrastructure.event.impl;

import static org.assertj.core.api.Assertions.assertThat;

import com.keystone.ingestion.domain.event.SpecIngestedEvent;
import com.keystone.ingestion.infrastructure.event.IngestionEventPublisher;
import com.keystone.ingestion.infrastructure.event.IngestionEventPublisherImpl;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = {IngestionEventPublisherImpl.class, EventPublisherIntegrationTest.TestConfig.class})
@ActiveProfiles("test")
class EventPublisherIntegrationTest {

    @Autowired
    private IngestionEventPublisher publisher;

    @Autowired
    private TestEventListener listener;

    @Test
    void specIngested_shouldBeReceivedByListeners() {
        var event = new SpecIngestedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "a".repeat(40),
                "org/repo",
                "openapi.yaml",
                "checksum",
                Instant.now(),
                "idem-key");

        publisher.specIngested(event);

        assertThat(listener.events).hasSize(1);
        assertThat(listener.events.get(0).getEventType()).isEqualTo("SpecIngested");
        assertThat(listener.events.get(0).getSource()).isEqualTo("contract-ingestion");
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        TestEventListener testEventListener() {
            return new TestEventListener();
        }
    }

    @Component
    static class TestEventListener {
        final List<SpecIngestedEvent> events = new ArrayList<>();

        @EventListener
        public void handleSpecIngested(SpecIngestedEvent event) {
            events.add(event);
        }
    }
}
