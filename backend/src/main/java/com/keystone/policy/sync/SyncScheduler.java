package com.keystone.policy.sync;

import com.keystone.policy.application.dto.SyncPoliciesRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Scheduled task that periodically triggers policy synchronization.
 *
 * <p>Acts as a fallback for missed webhook events. The default interval
 * is 60 seconds, configurable via {@code policy.sync.interval.ms}.
 *
 * <p>Also listens for webhook-triggered sync events delivered via
 * Spring's {@link org.springframework.context.event.EventListener}.
 */
@Component
@EnableScheduling
public class SyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(SyncScheduler.class);

    private final PolicySyncServiceImpl syncService;
    private final String defaultSourceId;

    public SyncScheduler(PolicySyncServiceImpl syncService,
                         @Value("${policy.git.source-id:default}") String defaultSourceId) {
        this.syncService = syncService;
        this.defaultSourceId = defaultSourceId;
    }

    /**
     * Periodic sync triggered every 60 seconds (configurable).
     *
     * <p>Only syncs the default source. Webhook events can trigger
     * syncs for specific sources on demand.
     */
    @Scheduled(fixedRateString = "${policy.sync.interval.ms:60000}")
    public void scheduledSync() {
        log.debug("Running scheduled policy sync for source '{}'", defaultSourceId);
        try {
            var request = new SyncPoliciesRequest(defaultSourceId, null);
            var response = syncService.syncPolicies(request);
            if (response.success()) {
                log.debug("Scheduled sync completed: {} added, {} updated, {} removed",
                        response.policiesAdded(), response.policiesUpdated(),
                        response.policiesRemoved());
            } else {
                log.warn("Scheduled sync failed: {}", response.errorMessage());
            }
        } catch (Exception e) {
            log.error("Unexpected error during scheduled policy sync", e);
        }
    }
}
