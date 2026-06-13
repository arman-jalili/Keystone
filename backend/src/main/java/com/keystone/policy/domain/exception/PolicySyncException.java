// Canonical Reference: .pi/architecture/modules/policy-engine.md
// Module: policy-engine
package com.keystone.policy.domain.exception;

import java.util.Objects;

/**
 * Exception thrown when synchronizing policies from an external source fails.
 *
 * <p>This covers Git clone failures, authentication errors, network issues,
 * and partial sync failures where some policies could not be loaded.
 */
public class PolicySyncException extends RuntimeException {

    private final String sourceId;
    private final int partiallyLoadedCount;

    public PolicySyncException(String sourceId, String message) {
        super("Policy sync failed for source '" + sourceId + "': " + message);
        this.sourceId = Objects.requireNonNull(sourceId, "sourceId must not be null");
        this.partiallyLoadedCount = 0;
    }

    public PolicySyncException(String sourceId, String message, Throwable cause) {
        super("Policy sync failed for source '" + sourceId + "': " + message, cause);
        this.sourceId = Objects.requireNonNull(sourceId, "sourceId must not be null");
        this.partiallyLoadedCount = 0;
    }

    public PolicySyncException(String sourceId, String message, int partiallyLoadedCount) {
        super("Policy sync partially failed for source '" + sourceId + "': " + message + " (loaded "
                + partiallyLoadedCount + " policies)");
        this.sourceId = Objects.requireNonNull(sourceId, "sourceId must not be null");
        this.partiallyLoadedCount = partiallyLoadedCount;
    }

    public String getSourceId() {
        return sourceId;
    }

    public int getPartiallyLoadedCount() {
        return partiallyLoadedCount;
    }

    public boolean isPartialFailure() {
        return partiallyLoadedCount > 0;
    }
}
