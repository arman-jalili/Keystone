// Canonical Reference: .pi/architecture/modules/contract-ingestion.md
// Module: contract-ingestion
package com.keystone.ingestion.infrastructure.repository;

import com.keystone.ingestion.domain.model.OpenApiSpec;
import com.keystone.ingestion.domain.model.SpecVersion;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for accessing {@link OpenApiSpec} and {@link SpecVersion} entities.
 *
 * <p>This is the data access contract. The implementation may use Spring Data JPA,
 * raw JDBC, or any other persistence mechanism. Callers must not depend on
 * implementation details such as table names or column mappings.
 *
 * <p>This interface intentionally follows repository pattern conventions
 * that are compatible with Spring Data JPA for the initial implementation,
 * but is defined as a plain interface to keep the contract framework-agnostic.
 */
public interface SpecRepository {

    /**
     * Finds an OpenApiSpec by its unique identifier.
     *
     * @param specId the spec UUID
     * @return the spec if found, or empty if not
     */
    Optional<OpenApiSpec> findById(UUID specId);

    /**
     * Returns all OpenApiSpecs ordered by ingestion time descending.
     * Used by the Dashboard API inventory view.
     *
     * @return all specs, most recently ingested first
     */
    List<OpenApiSpec> findAllByOrderByIngestedAtDesc();

    /**
     * Finds specs whose latest version was ingested before the given threshold.
     * Used by the stale APIs query for the Dashboard.
     *
     * @param threshold specs with latest ingestion before this time are considered stale
     * @return list of stale specs
     */
    List<OpenApiSpec> findStaleSpecs(Instant threshold);

    /**
     * Finds an OpenApiSpec by repository and spec path.
     *
     * @param repository the repository identifier (e.g. "org/repo")
     * @param specPath   the relative spec path within the repository
     * @return the spec if found, or empty if not
     */
    Optional<OpenApiSpec> findByRepositoryAndSpecPath(String repository, String specPath);

    /**
     * Saves a new OpenApiSpec to the data store.
     *
     * @param spec the spec to save
     * @return the saved spec with any generated fields populated
     */
    OpenApiSpec save(OpenApiSpec spec);

    /**
     * Finds all versions for a given spec, ordered by ingestion timestamp descending.
     *
     * @param specId   the spec UUID
     * @param pageSize the maximum number of versions to return
     * @return the list of versions, most recent first
     */
    List<SpecVersion> findVersionsBySpecId(UUID specId, int pageSize);

    /**
     * Finds a single SpecVersion by its unique identifier.
     *
     * @param versionId the version UUID
     * @return the version if found, or empty if not
     */
    Optional<SpecVersion> findVersionById(UUID versionId);

    /**
     * Saves a new SpecVersion associated with the given spec.
     *
     * @param version the spec version to save
     * @return the saved version with any generated fields populated
     */
    SpecVersion saveVersion(SpecVersion version);
}
