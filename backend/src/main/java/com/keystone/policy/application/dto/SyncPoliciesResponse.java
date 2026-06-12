package com.keystone.policy.application.dto;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Output DTO for policy synchronization results.
 *
 * @param sourceId        The identifier of the policy source that was synced
 * @param policySetId     The UUID of the synced policy set
 * @param policySetName   The name of the synced policy set
 * @param version         The new version number after sync
 * @param policiesAdded   Number of policies added
 * @param policiesRemoved Number of policies removed
 * @param policiesUpdated Number of policies updated
 * @param syncedAt        When the sync completed
 * @param success         Whether the sync completed successfully (partial success = true with counts)
 * @param errorMessage    Error message if the sync failed entirely
 */
public record SyncPoliciesResponse(
    String sourceId,
    UUID policySetId,
    String policySetName,
    int version,
    int policiesAdded,
    int policiesRemoved,
    int policiesUpdated,
    Instant syncedAt,
    boolean success,
    String errorMessage
) {
    public SyncPoliciesResponse {
        Objects.requireNonNull(sourceId, "sourceId must not be null");
        Objects.requireNonNull(policySetId, "policySetId must not be null");
        Objects.requireNonNull(policySetName, "policySetName must not be null");
        Objects.requireNonNull(syncedAt, "syncedAt must not be null");
    }

    public static SyncPoliciesResponse success(String sourceId, UUID policySetId,
                                                String policySetName, int version,
                                                int added, int removed, int updated,
                                                Instant syncedAt) {
        return new SyncPoliciesResponse(sourceId, policySetId, policySetName,
                                        version, added, removed, updated,
                                        syncedAt, true, null);
    }

    public static SyncPoliciesResponse failure(String sourceId, String errorMessage) {
        return new SyncPoliciesResponse(sourceId, null, null, 0,
                                        0, 0, 0, Instant.now(), false, errorMessage);
    }
}
