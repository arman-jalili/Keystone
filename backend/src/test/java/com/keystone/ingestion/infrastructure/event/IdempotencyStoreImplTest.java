package com.keystone.ingestion.infrastructure.event;

import static org.assertj.core.api.Assertions.assertThat;

import com.keystone.ingestion.domain.model.IdempotencyKey;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@Import(IdempotencyStoreImpl.class)
@ActiveProfiles("test")
class IdempotencyStoreImplTest {

    @Autowired
    private IdempotencyStore idempotencyStore;

    private final IdempotencyKey key = new IdempotencyKey("org/repo", "a".repeat(40), "openapi.yaml");

    @Test
    void shouldReturnEmptyForUnknownKey() {
        Optional<UUID> result = idempotencyStore.findEventIdByKey(key);
        assertThat(result).isEmpty();
    }

    @Test
    void shouldSaveAndRetrieveKey() {
        UUID eventId = UUID.randomUUID();

        UUID saved = idempotencyStore.save(key, eventId);
        assertThat(saved).isEqualTo(eventId);

        Optional<UUID> found = idempotencyStore.findEventIdByKey(key);
        assertThat(found).isPresent();
        assertThat(found.get()).isEqualTo(eventId);
    }

    @Test
    void shouldDeleteOldKeys() {
        UUID eventId = UUID.randomUUID();
        idempotencyStore.save(key, eventId);

        int deleted = idempotencyStore.deleteOlderThan(Instant.now().plusSeconds(1));
        assertThat(deleted).isPositive();

        Optional<UUID> found = idempotencyStore.findEventIdByKey(key);
        assertThat(found).isEmpty();
    }

    @Test
    void shouldNotDeleteRecentKeys() {
        UUID eventId = UUID.randomUUID();
        idempotencyStore.save(key, eventId);

        int deleted = idempotencyStore.deleteOlderThan(Instant.now().minusSeconds(1));
        assertThat(deleted).isZero();

        Optional<UUID> found = idempotencyStore.findEventIdByKey(key);
        assertThat(found).isPresent();
    }

    @Test
    void shouldHandleMultipleKeys() {
        IdempotencyKey key1 = new IdempotencyKey("org/repo1", "a".repeat(40), "spec1.yaml");
        IdempotencyKey key2 = new IdempotencyKey("org/repo2", "b".repeat(40), "spec2.yaml");
        UUID eventId1 = UUID.randomUUID();
        UUID eventId2 = UUID.randomUUID();

        idempotencyStore.save(key1, eventId1);
        idempotencyStore.save(key2, eventId2);

        assertThat(idempotencyStore.findEventIdByKey(key1)).hasValue(eventId1);
        assertThat(idempotencyStore.findEventIdByKey(key2)).hasValue(eventId2);
    }
}
