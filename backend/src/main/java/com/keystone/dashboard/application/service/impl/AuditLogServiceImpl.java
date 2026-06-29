// Canonical Reference: .pi/architecture/modules/dashboard.md#audit-log-service
package com.keystone.dashboard.application.service.impl;

import com.keystone.dashboard.application.service.AuditLogService;
import com.keystone.dashboard.domain.model.AuditEntry;
import com.keystone.dashboard.infrastructure.repository.SpringDataAuditEntryRepository;
import com.keystone.dashboard.infrastructure.repository.jpa.AuditEntryEntity;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * JPA-backed implementation of {@link AuditLogService}.
 *
 * <p>Queries the append-only audit event store backed by the {@code audit_log_entries} table.
 */
@Service
@Transactional(readOnly = true)
public class AuditLogServiceImpl implements AuditLogService {

    private final SpringDataAuditEntryRepository repository;

    public AuditLogServiceImpl(SpringDataAuditEntryRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<AuditEntry> query(int page, int size, String action) {
        var pageable = PageRequest.of(page, size);
        if (action != null && !action.isBlank()) {
            return repository.findByActionOrderByTimestampDesc(action, pageable).stream()
                    .map(this::toDomain)
                    .toList();
        }
        return repository.findAllByOrderByTimestampDesc(pageable).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public long count(String action) {
        if (action != null && !action.isBlank()) {
            return repository.countByAction(action);
        }
        return repository.countAll();
    }

    /**
     * Appends an entry to the audit log. Called by event listeners or services
     * to record significant events (ingestion, analysis, policy evaluation, etc.).
     */
    @Transactional
    public void record(String action, String actor, String target, String details) {
        var entity = new AuditEntryEntity(
                UUID.randomUUID().toString(), action, actor != null ? actor : "system", target, details, Instant.now());
        repository.save(entity);
    }

    private AuditEntry toDomain(AuditEntryEntity e) {
        return new AuditEntry(e.getId(), e.getAction(), e.getActor(), e.getTarget(), e.getDetails(), e.getTimestamp());
    }
}
