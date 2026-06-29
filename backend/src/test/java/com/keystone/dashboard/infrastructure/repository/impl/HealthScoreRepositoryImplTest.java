package com.keystone.dashboard.infrastructure.repository.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.keystone.dashboard.infrastructure.repository.SpringDataHealthScoreRepository;
import com.keystone.dashboard.infrastructure.repository.jpa.HealthScoreEntity;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class HealthScoreRepositoryImplTest {

    @Mock
    private SpringDataHealthScoreRepository jpaRepository;

    @Test
    void findLatestByEntity_shouldReturnLatestScore() {
        var repo = new HealthScoreRepositoryImpl(jpaRepository);
        var id = UUID.randomUUID();
        var now = Instant.now();
        var entity = new HealthScoreEntity(id, "repo", "org/test", 0.95, 0.9, 0.95, 0.98, 0.92, now);

        when(jpaRepository.findByEntityOrderByComputedAtDesc(any(), any(), any(Pageable.class)))
                .thenReturn(List.of(entity));

        var result = repo.findLatestByEntity("repo", "org/test");

        assertThat(result).isPresent();
        assertThat(result.get().score()).isEqualTo(0.95);
    }

    @Test
    void findAllLatest_shouldDeduplicateByEntity() {
        var repo = new HealthScoreRepositoryImpl(jpaRepository);
        var id1 = UUID.randomUUID();
        var now = Instant.now();

        when(jpaRepository.findAll())
                .thenReturn(List.of(
                        new HealthScoreEntity(id1, "repo", "org/test", 0.8, 0.8, 0.8, 0.8, 0.8, now),
                        new HealthScoreEntity(
                                UUID.randomUUID(), "repo", "org/test", 0.95, 0.9, 0.95, 0.98, 0.92, now)));

        var results = repo.findAllLatest();

        assertThat(results).hasSize(1);
        assertThat(results.get(0).score()).isEqualTo(0.95);
    }
}
