package com.keystone.ingestion.interfaces.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.keystone.ingestion.application.dto.IdempotencyCheckRequest;
import com.keystone.ingestion.application.dto.IncomingSpec;
import com.keystone.ingestion.application.dto.SpecIngestedResponse;
import com.keystone.ingestion.application.service.IngestionService;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class IngestionControllerTest {

    @Mock
    private IngestionService ingestionService;

    @InjectMocks
    private IngestionController controller;

    @Test
    void ingestSpec_shouldReturn201ForNewIngestion() {
        var request = new IncomingSpec("org/repo", "a".repeat(40), "openapi.yaml", "openapi: 3.0.0");
        var response = SpecIngestedResponse.newIngestion(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "org/repo",
                "openapi.yaml",
                "a".repeat(40),
                "checksum123",
                Instant.now());

        when(ingestionService.ingestSpec(any())).thenReturn(response);

        var result = controller.ingestSpec(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().duplicate()).isFalse();
    }

    @Test
    void ingestSpec_shouldReturn200ForDuplicate() {
        var request = new IncomingSpec("org/repo", "a".repeat(40), "openapi.yaml", "openapi: 3.0.0");
        var response = SpecIngestedResponse.duplicate(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "org/repo",
                "openapi.yaml",
                "a".repeat(40),
                "checksum123",
                Instant.now());

        when(ingestionService.ingestSpec(any())).thenReturn(response);

        var result = controller.ingestSpec(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().duplicate()).isTrue();
    }

    @Test
    void checkIdempotency_shouldReturnDuplicateTrueWhenExists() {
        var request = new IdempotencyCheckRequest("org/repo", "a".repeat(40), "openapi.yaml");
        UUID existingId = UUID.randomUUID();
        when(ingestionService.checkIdempotency(any())).thenReturn(Optional.of(existingId));

        var result = controller.checkIdempotency(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().duplicate()).isTrue();
        assertThat(result.getBody().existingEventId()).isEqualTo(existingId);
    }

    @Test
    void checkIdempotency_shouldReturnDuplicateFalseWhenNew() {
        var request = new IdempotencyCheckRequest("org/repo", "a".repeat(40), "openapi.yaml");
        when(ingestionService.checkIdempotency(any())).thenReturn(Optional.empty());

        var result = controller.checkIdempotency(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().duplicate()).isFalse();
        assertThat(result.getBody().existingEventId()).isNull();
    }
}
