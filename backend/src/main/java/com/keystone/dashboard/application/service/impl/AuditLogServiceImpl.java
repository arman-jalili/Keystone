// Canonical Reference: .pi/architecture/modules/dashboard.md#audit-log-service
// Implements: Queries the append-only audit event store
package com.keystone.dashboard.application.service.impl;

import com.keystone.dashboard.application.service.AuditLogService;
import com.keystone.dashboard.domain.model.AuditEntry;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Implementation of {@link AuditLogService}.
 *
 * <p>Queries the append-only audit event store. Currently returns an empty list
 * as the audit event store is not yet implemented. Once the event sourcing
 * infrastructure is in place, this service will query the event store directly.
 */
@Service
public class AuditLogServiceImpl implements AuditLogService {

    @Override
    public List<AuditEntry> query(int page, int size, String action) {
        // TODO: Query the append-only audit event store once implemented
        return Collections.emptyList();
    }

    @Override
    public long count(String action) {
        // TODO: Query the append-only audit event store once implemented
        return 0L;
    }
}
