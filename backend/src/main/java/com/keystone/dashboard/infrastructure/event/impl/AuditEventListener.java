// Canonical Reference: .pi/architecture/modules/dashboard.md#audit-log-service
package com.keystone.dashboard.infrastructure.event.impl;

import com.keystone.analysis.domain.event.BreakingChangeReportedEvent;
import com.keystone.dashboard.application.service.impl.AuditLogServiceImpl;
import com.keystone.ingestion.domain.event.SpecIngestedEvent;
import com.keystone.policy.domain.event.PolicyEvaluatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Listens for domain events across all bounded contexts and records them
 * to the append-only audit log.
 */
@Component
public class AuditEventListener {

    private static final Logger log = LoggerFactory.getLogger(AuditEventListener.class);

    private final AuditLogServiceImpl auditLog;

    public AuditEventListener(AuditLogServiceImpl auditLog) {
        this.auditLog = auditLog;
    }

    @EventListener
    public void onSpecIngested(SpecIngestedEvent event) {
        auditLog.record(
                "SPEC_INGESTED",
                "system",
                event.specId().toString(),
                "Ingested " + event.specPath() + " at " + event.commitSha());
    }

    @EventListener
    public void onBreakingChangeReported(BreakingChangeReportedEvent event) {
        auditLog.record(
                "BREAKING_CHANGE_REPORTED",
                "system",
                event.reportId().toString(),
                "Verdict: " + event.verdict() + " for " + event.specPath());
    }

    @EventListener
    public void onPolicyEvaluated(PolicyEvaluatedEvent event) {
        auditLog.record(
                "POLICY_EVALUATED",
                "system",
                event.specId().toString(),
                "Verdict: " + event.verdict() + ", violations: " + event.violationCount());
    }
}
