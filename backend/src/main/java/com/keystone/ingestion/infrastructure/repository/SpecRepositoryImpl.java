// Canonical Reference: .pi/architecture/modules/contract-ingestion.md
// Module: contract-ingestion
package com.keystone.ingestion.infrastructure.repository;

import com.keystone.ingestion.domain.model.OpenApiSpec;
import com.keystone.ingestion.domain.model.SpecVersion;
import com.keystone.ingestion.infrastructure.repository.jpa.OpenApiSpecEntity;
import com.keystone.ingestion.infrastructure.repository.jpa.SpecVersionEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adapts Spring Data JPA to the domain {@link SpecRepository} interface.
 */
@Repository
@Transactional
public class SpecRepositoryImpl implements SpecRepository {

    private final SpringDataSpecRepository specRepo;
    private final SpringDataSpecVersionRepository versionRepo;

    public SpecRepositoryImpl(SpringDataSpecRepository specRepo, SpringDataSpecVersionRepository versionRepo) {
        this.specRepo = specRepo;
        this.versionRepo = versionRepo;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OpenApiSpec> findById(UUID specId) {
        return specRepo.findById(specId).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OpenApiSpec> findByRepositoryAndSpecPath(String repository, String specPath) {
        return specRepo.findByRepositoryAndSpecPath(repository, specPath).map(this::toDomain);
    }

    @Override
    public OpenApiSpec save(OpenApiSpec spec) {
        var entity =
                new OpenApiSpecEntity(spec.getId(), spec.getRepository(), spec.getSpecPath(), spec.getIngestedAt());
        var saved = specRepo.save(entity);
        return toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SpecVersion> findVersionsBySpecId(UUID specId, int pageSize) {
        return versionRepo.findBySpecIdOrderByIngestedAtDesc(specId, PageRequest.of(0, pageSize)).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public SpecVersion saveVersion(SpecVersion version) {
        var entity = new SpecVersionEntity(
                version.getId(),
                version.getSpecId(),
                version.getCommitSha(),
                version.getChecksum(),
                version.getRawContent(),
                version.getIngestedAt());
        var saved = versionRepo.save(entity);
        return toDomain(saved);
    }

    private OpenApiSpec toDomain(OpenApiSpecEntity e) {
        return new OpenApiSpec(e.getId(), e.getRepository(), e.getSpecPath(), e.getIngestedAt());
    }

    private SpecVersion toDomain(SpecVersionEntity e) {
        return new SpecVersion(
                e.getId(), e.getSpecId(), e.getCommitSha(), e.getChecksum(), e.getRawContent(), e.getIngestedAt());
    }
}
