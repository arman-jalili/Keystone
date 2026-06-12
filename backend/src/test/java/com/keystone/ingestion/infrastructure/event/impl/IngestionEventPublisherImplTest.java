package com.keystone.ingestion.infrastructure.event.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.keystone.ingestion.domain.event.SpecIngestedEvent;
import com.keystone.ingestion.domain.event.SpecParseFailedEvent;
import com.keystone.ingestion.infrastructure.event.IngestionEventPublisherImpl;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class IngestionEventPublisherImplTest {

    @Mock
    private ApplicationEventPublisher springPublisher;

    @InjectMocks
    private IngestionEventPublisherImpl publisher;

    @Captor
    private ArgumentCaptor<SpecIngestedEvent> ingestedCaptor;

    @Captor
    private ArgumentCaptor<SpecParseFailedEvent> failedCaptor;

    @Test
    void specIngested_shouldPublishEvent() {
        var event = new SpecIngestedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "a".repeat(40),
                "org/repo",
                "openapi.yaml",
                "checksum123",
                Instant.now(),
                "idem-key");

        publisher.specIngested(event);

        verify(springPublisher).publishEvent(ingestedCaptor.capture());
        assertThat(ingestedCaptor.getValue().getEventType()).isEqualTo("SpecIngested");
        assertThat(ingestedCaptor.getValue().getSource()).isEqualTo("contract-ingestion");
        assertThat(ingestedCaptor.getValue().getPayload().repository()).isEqualTo("org/repo");
    }

    @Test
    void specParseFailed_shouldPublishEvent() {
        var event = new SpecParseFailedEvent(
                UUID.randomUUID(),
                "org/repo",
                "a".repeat(40),
                "openapi.yaml",
                List.of("Invalid spec"),
                "excerpt...",
                Instant.now(),
                "idem-key");

        publisher.specParseFailed(event);

        verify(springPublisher).publishEvent(failedCaptor.capture());
        assertThat(failedCaptor.getValue().getEventType()).isEqualTo("SpecParseFailed");
        assertThat(failedCaptor.getValue().getSource()).isEqualTo("contract-ingestion");
        assertThat(failedCaptor.getValue().getPayload().errors()).contains("Invalid spec");
    }

    @Test
    void specIngested_shouldPreserveAllFields() {
        UUID eventId = UUID.randomUUID();
        UUID specId = UUID.randomUUID();
        Instant now = Instant.now();

        var event = new SpecIngestedEvent(
                eventId, specId, "abc123def", "my-org/my-repo", "api/openapi.yaml", "sha256checksum", now, "idem-001");

        publisher.specIngested(event);

        verify(springPublisher).publishEvent(ingestedCaptor.capture());
        var captured = ingestedCaptor.getValue();

        assertThat(captured.eventId()).isEqualTo(eventId);
        assertThat(captured.specId()).isEqualTo(specId);
        assertThat(captured.commitSha()).isEqualTo("abc123def");
        assertThat(captured.repository()).isEqualTo("my-org/my-repo");
        assertThat(captured.specPath()).isEqualTo("api/openapi.yaml");
        assertThat(captured.checksum()).isEqualTo("sha256checksum");
        assertThat(captured.timestamp()).isEqualTo(now);
        assertThat(captured.idempotencyKey()).isEqualTo("idem-001");
    }

    @Test
    void specParseFailed_shouldPreserveAllFields() {
        UUID eventId = UUID.randomUUID();
        Instant now = Instant.now();
        var errors = List.of("error1", "error2");

        var event = new SpecParseFailedEvent(
                eventId, "org/repo", "abc123", "spec.yaml", errors, "raw excerpt...", now, "idem-002");

        publisher.specParseFailed(event);

        verify(springPublisher).publishEvent(failedCaptor.capture());
        var captured = failedCaptor.getValue();

        assertThat(captured.eventId()).isEqualTo(eventId);
        assertThat(captured.repository()).isEqualTo("org/repo");
        assertThat(captured.commitSha()).isEqualTo("abc123");
        assertThat(captured.specPath()).isEqualTo("spec.yaml");
        assertThat(captured.errors()).containsExactly("error1", "error2");
        assertThat(captured.rawContentExcerpt()).isEqualTo("raw excerpt...");
        assertThat(captured.timestamp()).isEqualTo(now);
        assertThat(captured.idempotencyKey()).isEqualTo("idem-002");
    }
}
