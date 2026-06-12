package com.keystone.ingestion.infrastructure.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.keystone.ingestion.domain.model.OpenApiSpec;
import com.keystone.ingestion.domain.model.SpecVersion;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@Import(SpecRepositoryImpl.class)
@ActiveProfiles("test")
class SpecRepositoryImplTest {

    @Autowired
    private SpecRepository specRepository;

    @Test
    void findById_shouldReturnEmptyForUnknownId() {
        Optional<OpenApiSpec> result = specRepository.findById(UUID.randomUUID());
        assertThat(result).isEmpty();
    }

    @Test
    void saveAndFindById_shouldRoundTrip() {
        UUID specId = UUID.randomUUID();
        var spec = new OpenApiSpec(specId, "org/repo", "openapi.yaml", Instant.now());

        OpenApiSpec saved = specRepository.save(spec);
        assertThat(saved.getId()).isEqualTo(specId);

        Optional<OpenApiSpec> found = specRepository.findById(specId);
        assertThat(found).isPresent();
        assertThat(found.get().getRepository()).isEqualTo("org/repo");
        assertThat(found.get().getSpecPath()).isEqualTo("openapi.yaml");
    }

    @Test
    void findByRepositoryAndSpecPath_shouldReturnSpec() {
        UUID specId = UUID.randomUUID();
        var spec = new OpenApiSpec(specId, "org/repo", "openapi.yaml", Instant.now());
        specRepository.save(spec);

        Optional<OpenApiSpec> found = specRepository.findByRepositoryAndSpecPath("org/repo", "openapi.yaml");
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(specId);
    }

    @Test
    void findByRepositoryAndSpecPath_shouldReturnEmptyForUnknown() {
        Optional<OpenApiSpec> found = specRepository.findByRepositoryAndSpecPath("unknown", "unknown.yaml");
        assertThat(found).isEmpty();
    }

    @Test
    void saveVersionAndFindVersions_shouldReturnOrdered() {
        UUID specId = UUID.randomUUID();
        var spec = new OpenApiSpec(specId, "org/repo", "openapi.yaml", Instant.now());
        specRepository.save(spec);

        var version1 = new SpecVersion(
                UUID.randomUUID(),
                specId,
                "a".repeat(40),
                "chk1",
                "content1",
                Instant.now().minusSeconds(10));
        var version2 = new SpecVersion(UUID.randomUUID(), specId, "b".repeat(40), "chk2", "content2", Instant.now());

        specRepository.saveVersion(version1);
        specRepository.saveVersion(version2);

        List<SpecVersion> versions = specRepository.findVersionsBySpecId(specId, 10);

        assertThat(versions).hasSize(2);
        // Most recent first
        assertThat(versions.get(0).getCommitSha()).isEqualTo("b".repeat(40));
        assertThat(versions.get(1).getCommitSha()).isEqualTo("a".repeat(40));
    }

    @Test
    void findVersionsBySpecId_shouldRespectPageSize() {
        UUID specId = UUID.randomUUID();
        var spec = new OpenApiSpec(specId, "org/repo", "openapi.yaml", Instant.now());
        specRepository.save(spec);

        for (int i = 0; i < 5; i++) {
            var version = new SpecVersion(
                    UUID.randomUUID(),
                    specId,
                    String.valueOf(i).repeat(40),
                    "chk" + i,
                    "content" + i,
                    Instant.now().minusSeconds(i));
            specRepository.saveVersion(version);
        }

        List<SpecVersion> versions = specRepository.findVersionsBySpecId(specId, 3);
        assertThat(versions).hasSize(3);
    }

    @Test
    void saveMultipleSpecs_shouldAllBeRetrievable() {
        var spec1 = new OpenApiSpec(UUID.randomUUID(), "org/repo1", "spec1.yaml", Instant.now());
        var spec2 = new OpenApiSpec(UUID.randomUUID(), "org/repo2", "spec2.yaml", Instant.now());

        specRepository.save(spec1);
        specRepository.save(spec2);

        assertThat(specRepository.findById(spec1.getId())).isPresent();
        assertThat(specRepository.findById(spec2.getId())).isPresent();
    }
}
