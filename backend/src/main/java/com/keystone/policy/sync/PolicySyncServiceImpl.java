package com.keystone.policy.sync;

import com.keystone.policy.application.dto.PolicySourceConfigRequest;
import com.keystone.policy.application.dto.SyncPoliciesRequest;
import com.keystone.policy.application.dto.SyncPoliciesResponse;
import com.keystone.policy.application.service.PolicySyncService;
import com.keystone.policy.domain.event.PolicySourceChangedEvent;
import com.keystone.policy.domain.event.PolicySyncedEvent;
import com.keystone.policy.domain.exception.PolicySyncException;
import com.keystone.policy.domain.model.Policy;
import com.keystone.policy.infrastructure.event.PolicyEventPublisher;
import com.keystone.policy.infrastructure.repository.PolicyRepository;
import com.keystone.policy.source.GitPolicySourceImpl;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestrates policy synchronization from external sources to the database cache.
 *
 * <p>Fetches policies from the configured Git source, validates them,
 * diffs against the current cache, applies changes, and publishes events.
 *
 * <p>Triggered by:
 * <ul>
 *   <li>{@link org.springframework.scheduling.annotation.Scheduled} poll timer (60s fallback)</li>
 *   <li>{@link org.springframework.context.event.EventListener} for Git webhook events</li>
 * </ul>
 */
@Service
@Transactional
public class PolicySyncServiceImpl implements PolicySyncService {

    private static final Logger log = LoggerFactory.getLogger(PolicySyncServiceImpl.class);

    private final GitPolicySourceImpl gitPolicySource;
    private final PolicyRepository policyRepository;
    private final PolicyEventPublisher eventPublisher;

    private final Map<String, PolicySourceConfigRequest> sourceConfigs = new ConcurrentHashMap<>();

    public PolicySyncServiceImpl(
            GitPolicySourceImpl gitPolicySource,
            PolicyRepository policyRepository,
            PolicyEventPublisher eventPublisher) {
        this.gitPolicySource = gitPolicySource;
        this.policyRepository = policyRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public SyncPoliciesResponse syncPolicies(SyncPoliciesRequest request) throws PolicySyncException {
        String sourceId = request.sourceId();
        log.info("Starting policy sync from source: {}", sourceId);

        Instant startedAt = Instant.now();

        try {
            // 1. Fetch remote policies
            List<Policy> remotePolicies;
            if (request.hasExplicitRef()) {
                remotePolicies = gitPolicySource.fetchPoliciesFromRef(request.ref());
            } else {
                remotePolicies = gitPolicySource.fetchPolicies();
            }

            log.info("Fetched {} policies from source '{}'", remotePolicies.size(), sourceId);

            // 2. Diff against current state
            List<Policy> currentPolicies = policyRepository.findPoliciesBySource(sourceId);
            Map<String, Policy> currentByName =
                    currentPolicies.stream().collect(Collectors.toMap(Policy::getName, p -> p));

            List<String> remoteNames =
                    remotePolicies.stream().map(Policy::getName).toList();

            int added = 0, updated = 0, removed = 0;

            // 3. Upsert new and updated policies
            for (Policy remote : remotePolicies) {
                Policy existing = currentByName.get(remote.getName());
                if (existing == null) {
                    // New policy
                    policyRepository.savePolicy(remote);
                    added++;
                } else if (existing.getVersion() < remote.getVersion()
                        || !existing.getDslExpression().equals(remote.getDslExpression())) {
                    // Updated policy
                    policyRepository.updatePolicy(remote);
                    updated++;
                }
            }

            // 4. Soft-delete policies no longer in remote
            List<String> orphanedNames = currentPolicies.stream()
                    .map(Policy::getName)
                    .filter(name -> !remoteNames.contains(name))
                    .toList();

            if (!orphanedNames.isEmpty()) {
                for (String name : orphanedNames) {
                    policyRepository.findPolicyByNameAndSource(name, sourceId).ifPresent(p -> {
                        // Soft delete via deactivation
                        var deactivated = new Policy(
                                p.getId(),
                                p.getName(),
                                p.getDescription(),
                                p.getSeverity(),
                                com.keystone.policy.domain.model.PolicyStatus.INACTIVE,
                                p.getScope(),
                                p.getDslExpression(),
                                p.getSourceId(),
                                p.getVersion(),
                                p.getCreatedAt(),
                                Instant.now());
                        policyRepository.updatePolicy(deactivated);
                    });
                }
                removed = orphanedNames.size();
            }

            // 5. Get version info
            String versionStr = gitPolicySource.getCurrentVersion();
            int newVersion = versionStr.hashCode();

            // 6. Publish event
            var syncedEvent = new PolicySyncedEvent(
                    UUID.randomUUID(),
                    sourceId,
                    UUID.randomUUID(),
                    sourceId + "-policy-set",
                    Math.abs(newVersion),
                    added,
                    removed,
                    updated,
                    Instant.now());
            eventPublisher.policySynced(syncedEvent);

            log.info(
                    "Policy sync completed for '{}': {} added, {} updated, {} removed",
                    sourceId,
                    added,
                    updated,
                    removed);

            return SyncPoliciesResponse.success(
                    sourceId,
                    UUID.randomUUID(),
                    sourceId + "-policy-set",
                    Math.abs(newVersion),
                    added,
                    removed,
                    updated,
                    Instant.now());

        } catch (PolicySyncException e) {
            log.error("Policy sync failed for '{}': {}", sourceId, e.getMessage());
            return SyncPoliciesResponse.failure(sourceId, e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during policy sync for '{}'", sourceId, e);
            return SyncPoliciesResponse.failure(sourceId, "Unexpected error: " + e.getMessage());
        }
    }

    @Override
    public void configureSource(PolicySourceConfigRequest request) throws PolicySyncException {
        log.info("Configuring policy source: {}", request.sourceId());
        sourceConfigs.put(request.sourceId(), request);

        var changeEvent = new PolicySourceChangedEvent(
                UUID.randomUUID(),
                request.sourceId(),
                request.sourceType(),
                PolicySourceChangedEvent.ChangeType.CREATED,
                Instant.now());
        eventPublisher.policySourceChanged(changeEvent);
    }

    @Override
    public void removeSource(String sourceId, boolean deletePoliciesFromSource) throws PolicySyncException {
        log.info("Removing policy source: {} (deletePolicies={})", sourceId, deletePoliciesFromSource);

        sourceConfigs.remove(sourceId);

        if (deletePoliciesFromSource) {
            int deleted = policyRepository.deletePoliciesBySource(sourceId);
            log.info("Deleted {} policies for source '{}'", deleted, sourceId);
        }

        var changeEvent = new PolicySourceChangedEvent(
                UUID.randomUUID(), sourceId, "git", PolicySourceChangedEvent.ChangeType.DELETED, Instant.now());
        eventPublisher.policySourceChanged(changeEvent);
    }

    @Override
    public List<String> listSources() {
        return List.copyOf(sourceConfigs.keySet());
    }
}
