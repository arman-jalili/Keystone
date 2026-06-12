package com.keystone.ingestion.application.service;

import com.keystone.ingestion.application.dto.IdempotencyCheckRequest;
import com.keystone.ingestion.application.dto.IncomingSpec;
import com.keystone.ingestion.application.dto.SpecIngestedResponse;
import com.keystone.ingestion.domain.exception.SpecParseException;
import java.util.Optional;
import java.util.UUID;

/**
 * Application service interface for the spec ingestion use case.
 *
 * <p>This is the primary inbound port (driving adapter) for the
 * contract-ingestion module. It defines the contract that the
 * {@link com.keystone.ingestion.interfaces.http.IngestionController} depends on.
 *
 * <p>Orchestrates the ingestion flow:
 * <ol>
 *   <li>Deduplication check via {@link com.keystone.ingestion.domain.filter.DeduplicationFilter}</li>
 *   <li>OpenAPI validation</li>
 *   <li>Persistence via {@link com.keystone.ingestion.infrastructure.repository.SpecRepository}</li>
 *   <li>Event publication via {@link com.keystone.ingestion.infrastructure.event.IngestionEventPublisher}</li>
 * </ol>
 */
public interface IngestionService {

    /**
     * Ingests an OpenAPI specification.
     *
     * <p>If the spec has already been processed (determined by the idempotency key),
     * returns the existing {@link SpecIngestedResponse} with a 200 OK semantics.
     * Otherwise processes the spec and returns a 201 Created response.
     *
     * @param request the incoming spec payload
     * @return the ingestion result
     * @throws SpecParseException if the spec content fails validation or parsing
     */
    SpecIngestedResponse ingestSpec(IncomingSpec request) throws SpecParseException;

    /**
     * Pre-flight idempotency check.
     *
     * <p>Allows the CLI to check whether a spec has already been ingested
     * without uploading the full content. This is an optional optimization.
     *
     * @param request the idempotency key to check
     * @return the existing event ID if already processed, or empty if not
     */
    Optional<UUID> checkIdempotency(IdempotencyCheckRequest request);
}
