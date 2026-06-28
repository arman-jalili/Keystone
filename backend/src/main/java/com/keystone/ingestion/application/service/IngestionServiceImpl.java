// Canonical Reference: .pi/architecture/modules/contract-ingestion.md
// Module: contract-ingestion
package com.keystone.ingestion.application.service;

import com.keystone.ingestion.application.dto.IdempotencyCheckRequest;
import com.keystone.ingestion.application.dto.IncomingSpec;
import com.keystone.ingestion.application.dto.SpecIngestedResponse;
import com.keystone.ingestion.domain.event.SpecIngestedEvent;
import com.keystone.ingestion.domain.event.SpecParseFailedEvent;
import com.keystone.ingestion.domain.exception.SpecParseException;
import com.keystone.ingestion.domain.filter.DeduplicationFilter;
import com.keystone.ingestion.domain.filter.SpecValidator;
import com.keystone.ingestion.domain.model.IdempotencyKey;
import com.keystone.ingestion.domain.model.OpenApiSpec;
import com.keystone.ingestion.domain.model.SpecVersion;
import com.keystone.ingestion.infrastructure.event.IngestionEventPublisher;
import com.keystone.ingestion.infrastructure.repository.SpecRepository;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestrates the spec ingestion flow.
 *
 * <p>Coordinates deduplication, validation, persistence, and event publication.
 */
@Service
@Transactional
public class IngestionServiceImpl implements IngestionService {

    private final DeduplicationFilter deduplicationFilter;
    private final SpecValidator specValidator;
    private final SpecRepository specRepository;
    private final IngestionEventPublisher eventPublisher;

    public IngestionServiceImpl(
            DeduplicationFilter deduplicationFilter,
            SpecValidator specValidator,
            SpecRepository specRepository,
            IngestionEventPublisher eventPublisher) {
        this.deduplicationFilter = deduplicationFilter;
        this.specValidator = specValidator;
        this.specRepository = specRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public SpecIngestedResponse ingestSpec(IncomingSpec request) throws SpecParseException {
        IdempotencyKey idempotencyKey =
                new IdempotencyKey(request.repository(), request.commitSha(), request.specPath());

        // 1. Check dedup
        Optional<UUID> existingEventId = deduplicationFilter.isDuplicate(idempotencyKey);
        if (existingEventId.isPresent()) {
            // Return existing response for duplicate
            return specRepository
                    .findByRepositoryAndSpecPath(request.repository(), request.specPath())
                    .map(spec -> SpecIngestedResponse.duplicate(
                            existingEventId.get(),
                            spec.getId(),
                            spec.getRepository(),
                            spec.getSpecPath(),
                            request.commitSha(),
                            checksum(request.content()),
                            spec.getIngestedAt()))
                    .orElseGet(() -> buildResponse(request, UUID.randomUUID(), Instant.now()));
        }

        // 2. Validate
        try {
            specValidator.validate(request.content());
        } catch (SpecParseException ex) {
            UUID eventId = UUID.randomUUID();
            var parseFailedEvent = new SpecParseFailedEvent(
                    eventId,
                    request.repository(),
                    request.commitSha(),
                    request.specPath(),
                    ex.getDetails().stream()
                            .map(d -> d.field() + ": " + d.message())
                            .toList(),
                    request.content()
                            .substring(0, Math.min(200, request.content().length())),
                    Instant.now(),
                    idempotencyKey.toString());
            eventPublisher.specParseFailed(parseFailedEvent);
            throw ex;
        }

        // 3. Persist — reuse spec if already exists for this (repository, specPath)
        UUID specId = specRepository.findByRepositoryAndSpecPath(request.repository(), request.specPath())
                .map(OpenApiSpec::getId)
                .orElseGet(() -> {
                    UUID newId = UUID.randomUUID();
                    OpenApiSpec newSpec = new OpenApiSpec(newId, request.repository(), request.specPath(), Instant.now());
                    specRepository.save(newSpec);
                    return newId;
                });

        UUID versionId = UUID.randomUUID();
        Instant now = Instant.now();
        String sha = checksum(request.content());

        SpecVersion version = new SpecVersion(versionId, specId, request.commitSha(), sha, request.content(), now);
        specRepository.saveVersion(version);

        // 4. Mark idempotent
        UUID eventId = UUID.randomUUID();
        deduplicationFilter.markProcessed(idempotencyKey, eventId);

        // 5. Publish event
        var ingestedEvent = new SpecIngestedEvent(
                eventId,
                specId,
                request.commitSha(),
                request.repository(),
                request.specPath(),
                sha,
                now,
                idempotencyKey.toString());
        eventPublisher.specIngested(ingestedEvent);

        return SpecIngestedResponse.newIngestion(
                eventId, specId, request.repository(), request.specPath(), request.commitSha(), sha, now);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UUID> checkIdempotency(IdempotencyCheckRequest request) {
        return deduplicationFilter.isDuplicate(request.toDomainKey());
    }

    private SpecIngestedResponse buildResponse(IncomingSpec request, UUID eventId, Instant now) {
        return SpecIngestedResponse.duplicate(
                eventId,
                UUID.randomUUID(),
                request.repository(),
                request.specPath(),
                request.commitSha(),
                checksum(request.content()),
                now);
    }

    private static String checksum(String content) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(content.getBytes());
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
