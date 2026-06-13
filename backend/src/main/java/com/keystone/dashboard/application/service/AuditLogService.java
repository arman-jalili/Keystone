// Canonical Reference: .pi/architecture/modules/dashboard.md#audit-log-service
// Implements: Application service interface for audit log queries
package com.keystone.dashboard.application.service;

import com.keystone.dashboard.domain.model.AuditEntry;
import java.util.List;

/**
 * Application service interface for querying the append-only audit event store.
 *
 * <p>Provides paginated access to audit events for the dashboard audit log view.
 * Only accessible to users with the COMPLIANCE_MANAGER role.
 */
public interface AuditLogService {

    /**
     * Queries the audit log with pagination and optional action filter.
     *
     * @param page   zero-based page number
     * @param size   page size
     * @param action optional action type filter (e.g. "SPEC_INGESTED", "POLICY_EVALUATED")
     * @return paginated list of audit entries
     */
    List<AuditEntry> query(int page, int size, String action);

    /**
     * Returns the total number of audit entries matching the optional action filter.
     *
     * @param action optional action type filter
     * @return total count
     */
    long count(String action);
}
