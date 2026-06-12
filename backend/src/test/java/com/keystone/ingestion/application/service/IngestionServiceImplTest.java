package com.keystone.ingestion.application.service;

import com.keystone.ingestion.application.dto.IncomingSpec;
import com.keystone.ingestion.application.dto.SpecIngestedResponse;
import com.keystone.ingestion.domain.event.SpecParseFailedEvent;
import com.keystone.ingestion.domain.exception.SpecParseException;
import com.keystone.ingestion.domain.filter.DeduplicationFilter;
import com.keystone.ingestion.domain.filter.SpecValidator;
import com.keystone.ingestion.domain.model.IdempotencyKey;
import com.keystone.ingestion.domain.model.OpenApiSpec;
import com.keystone.ingestion.domain.model.SpecVersion;
import com.keystone.ingestion.infrastructure.event.IngestionEventPublisher;
import com.keystone.ingestion.infrastructure.repository.SpecRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IngestionServiceImplTest {

    @Mock
    private DeduplicationFilter deduplicationFilter;
    @Mock
    private SpecValidator specValidator;
    @Mock
    private SpecRepository specRepository;
    @Mock
    private IngestionEventPublisher eventPublisher;

    @InjectMocks
    private IngestionServiceImpl ingestionService;

    @Captor
    private ArgumentCaptor<OpenApiSpec> specCaptor;
    @Captor
    private ArgumentCaptor<SpecVersion> versionCaptor;

    private final IncomingSpec request = new IncomingSpec(
            "org/repo", "a".repeat(40), "openapi.yaml",
            "openapi: 3.0.0\ninfo:\n  title: Test\n  version: '1.0'\npaths: {}\n");

    @Test
    void ingestSpec_shouldProcessNewSpec() {
        when(deduplicationFilter.isDuplicate(any())).thenReturn(Optional.empty());
        when(specRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(specRepository.saveVersion(any())).thenAnswer(i -> i.getArgument(0));
        when(deduplicationFilter.markProcessed(any(), any())).thenReturn(UUID.randomUUID());

        SpecIngestedResponse response = ingestionService.ingestSpec(request);

        assertThat(response).isNotNull();
        assertThat(response.repository()).isEqualTo("org/repo");
        assertThat(response.specPath()).isEqualTo("openapi.yaml");
        assertThat(response.duplicate()).isFalse();

        verify(specValidator).validate(request.content());
        verify(specRepository).save(any(OpenApiSpec.class));
        verify(specRepository).saveVersion(any(SpecVersion.class));
        verify(eventPublisher).specIngested(any());
    }

    @Test
    void ingestSpec_shouldReturnDuplicateForExistingKey() {
        UUID existingEventId = UUID.randomUUID();
        UUID specId = UUID.randomUUID();

        when(deduplicationFilter.isDuplicate(any())).thenReturn(Optional.of(existingEventId));
        when(specRepository.findByRepositoryAndSpecPath(request.repository(), request.specPath()))
                .thenReturn(Optional.of(new OpenApiSpec(specId, request.repository(),
                        request.specPath(), Instant.now())));

        SpecIngestedResponse response = ingestionService.ingestSpec(request);

        assertThat(response.duplicate()).isTrue();
        assertThat(response.eventId()).isEqualTo(existingEventId);

        verify(specValidator, never()).validate(any());
        verify(eventPublisher, never()).specIngested(any());
    }

    @Test
    void ingestSpec_shouldPublishParseFailedOnValidationError() {
        var validationError = new SpecParseException.ValidationError("content", "Invalid spec");
        when(deduplicationFilter.isDuplicate(any())).thenReturn(Optional.empty());
        doThrow(new SpecParseException("Validation failed", java.util.List.of(validationError)))
                .when(specValidator).validate(any());

        assertThatThrownBy(() -> ingestionService.ingestSpec(request))
                .isInstanceOf(SpecParseException.class);

        verify(eventPublisher).specParseFailed(any(SpecParseFailedEvent.class));
        verify(specRepository, never()).save(any());
    }

    @Test
    void ingestSpec_shouldGenerateChecksum() {
        when(deduplicationFilter.isDuplicate(any())).thenReturn(Optional.empty());
        when(specRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(specRepository.saveVersion(any())).thenAnswer(i -> i.getArgument(0));
        when(deduplicationFilter.markProcessed(any(), any())).thenReturn(UUID.randomUUID());

        SpecIngestedResponse response = ingestionService.ingestSpec(request);

        assertThat(response.checksum()).isNotBlank();
        assertThat(response.checksum()).hasSize(64); // SHA-256 hex
    }

    @Test
    void checkIdempotency_shouldDelegateToDedupFilter() {
        var checkRequest = new com.keystone.ingestion.application.dto.IdempotencyCheckRequest(
                "org/repo", "a".repeat(40), "openapi.yaml");
        UUID eventId = UUID.randomUUID();

        when(deduplicationFilter.isDuplicate(any())).thenReturn(Optional.of(eventId));

        Optional<UUID> result = ingestionService.checkIdempotency(checkRequest);

        assertThat(result).hasValue(eventId);
        verify(deduplicationFilter).isDuplicate(any(IdempotencyKey.class));
    }
}
