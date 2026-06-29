// Canonical Reference: .pi/architecture/modules/dashboard.md#data-flow
// Implements: Outbound event publisher for dashboard domain events
package com.keystone.dashboard.infrastructure.event.impl;

import com.keystone.dashboard.domain.event.DashboardViewAccessedEvent;
import com.keystone.dashboard.domain.event.HealthScoreRecalculatedEvent;
import com.keystone.dashboard.domain.event.PolicyStatusChangedEvent;
import com.keystone.dashboard.infrastructure.event.DashboardEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Implementation of {@link DashboardEventPublisher}.
 *
 * <p>Currently logs events to the console. In production, this should
 * publish to RabbitMQ or Spring's ApplicationEventPublisher.
 */
@Component
public class DashboardEventPublisherImpl implements DashboardEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(DashboardEventPublisherImpl.class);

    @Override
    public void healthScoreRecalculated(HealthScoreRecalculatedEvent event) {
        log.info(
                "Health score recalculated: {} for {}/{}",
                event.getEventType(),
                event.getPayload().entityType(),
                event.getPayload().entityId());
    }

    @Override
    public void dashboardViewAccessed(DashboardViewAccessedEvent event) {
        log.debug(
                "Dashboard view accessed: {} by {}",
                event.getPayload().viewType(),
                event.getPayload().userId());
    }

    @Override
    public void policyStatusChanged(PolicyStatusChangedEvent event) {
        log.info(
                "Policy status changed: {} → {} (policy: {})",
                event.getPayload().previousStatus(),
                event.getPayload().newStatus(),
                event.getPayload().policyName());
    }
}
