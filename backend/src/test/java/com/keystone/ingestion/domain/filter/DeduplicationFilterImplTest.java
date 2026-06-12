package com.keystone.ingestion.domain.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.keystone.ingestion.domain.model.IdempotencyKey;
import com.keystone.ingestion.infrastructure.event.IdempotencyStore;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeduplicationFilterImplTest {

    @Mock
    private IdempotencyStore idempotencyStore;

    @InjectMocks
    private DeduplicationFilterImpl filter;

    private final IdempotencyKey key = new IdempotencyKey("org/repo", "a".repeat(40), "openapi.yaml");

    @Test
    void isDuplicate_shouldReturnEmptyWhenKeyNotFound() {
        when(idempotencyStore.findEventIdByKey(key)).thenReturn(Optional.empty());

        Optional<UUID> result = filter.isDuplicate(key);

        assertThat(result).isEmpty();
        verify(idempotencyStore).findEventIdByKey(key);
    }

    @Test
    void isDuplicate_shouldReturnEventIdWhenKeyExists() {
        UUID eventId = UUID.randomUUID();
        when(idempotencyStore.findEventIdByKey(key)).thenReturn(Optional.of(eventId));

        Optional<UUID> result = filter.isDuplicate(key);

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(eventId);
    }

    @Test
    void markProcessed_shouldDelegateToStore() {
        UUID eventId = UUID.randomUUID();
        when(idempotencyStore.save(key, eventId)).thenReturn(eventId);

        UUID result = filter.markProcessed(key, eventId);

        assertThat(result).isEqualTo(eventId);
        verify(idempotencyStore).save(key, eventId);
    }

    @Test
    void markProcessed_shouldReturnExistingEventIdOnConflict() {
        UUID requestedEventId = UUID.randomUUID();
        UUID existingEventId = UUID.randomUUID();
        when(idempotencyStore.save(key, requestedEventId)).thenReturn(existingEventId);

        UUID result = filter.markProcessed(key, requestedEventId);

        assertThat(result).isEqualTo(existingEventId);
    }

    @Test
    void dedup_shouldReturnNewEventIdForNewKey() {
        UUID eventId = UUID.randomUUID();
        when(idempotencyStore.findEventIdByKey(key)).thenReturn(Optional.empty());
        when(idempotencyStore.save(key, eventId)).thenReturn(eventId);

        Optional<UUID> duplicate = filter.isDuplicate(key);
        assertThat(duplicate).isEmpty();

        UUID stored = filter.markProcessed(key, eventId);
        assertThat(stored).isEqualTo(eventId);
    }

    @Test
    void dedup_shouldReturnExistingEventIdForDuplicateKey() {
        UUID existingEventId = UUID.randomUUID();
        when(idempotencyStore.findEventIdByKey(key)).thenReturn(Optional.of(existingEventId));

        Optional<UUID> duplicate = filter.isDuplicate(key);

        assertThat(duplicate).isPresent();
        assertThat(duplicate.get()).isEqualTo(existingEventId);
        verify(idempotencyStore, never()).save(any(), any());
    }
}
