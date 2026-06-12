package com.keystone.policy.application.service;

import com.keystone.policy.application.dto.PolicySourceConfigRequest;
import com.keystone.policy.application.dto.SyncPoliciesRequest;
import com.keystone.policy.application.dto.SyncPoliciesResponse;
import com.keystone.policy.domain.exception.PolicySyncException;

/**
 * Application service for synchronizing policies from external sources.
 *
 * <p>This is the primary inbound port (driving adapter) for policy sync operations.
 * Controllers and event listeners depend on this interface.
 *
 * <p>Orchestrates the sync flow:
 * <ol>
 *   <li>Resolve the policy source configuration</li>
 *   <li>Fetch policy files from the source via {@link com.keystone.policy.infrastructure.source.PolicySource}</li>
 *   <li>Parse and validate policy DSL expressions</li>
 *   <li>Diff against the current policy set and apply changes</li>
 *   <li>Persist updated policies via {@link com.keystone.policy.infrastructure.repository.PolicyRepository}</li>
 *   <li>Publish a {@link com.keystone.policy.domain.event.PolicySyncedEvent}</li>
 * </ol>
 */
public interface PolicySyncService {

    /**
     * Synchronizes policies from an external source.
     *
     * <p>Fetches the latest policies from the configured source,
     * diffs against the current state, and applies changes atomically.
     *
     * @param request the sync request specifying which source to sync
     * @return the sync result with counts of added/removed/updated policies
     * @throws PolicySyncException if the sync operation fails
     */
    SyncPoliciesResponse syncPolicies(SyncPoliciesRequest request) throws PolicySyncException;

    /**
     * Registers or updates a policy source configuration.
     *
     * <p>After registration, the source can be used for policy synchronization.
     * Updates to an existing source ID will overwrite the previous configuration.
     *
     * @param request the source configuration
     * @throws PolicySyncException if the configuration is invalid
     */
    void configureSource(PolicySourceConfigRequest request) throws PolicySyncException;

    /**
     * Removes a policy source configuration and optionally deletes
     * all policies that were loaded from it.
     *
     * @param sourceId           the identifier of the source to remove
     * @param deletePoliciesFromSource whether to also delete associated policies
     * @throws PolicySyncException if the source cannot be removed
     */
    void removeSource(String sourceId, boolean deletePoliciesFromSource) throws PolicySyncException;

    /**
     * Returns a list of configured source IDs.
     *
     * @return the list of registered source identifiers
     */
    java.util.List<String> listSources();
}
