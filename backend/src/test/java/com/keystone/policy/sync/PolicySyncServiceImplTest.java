package com.keystone.policy.sync;

import com.keystone.policy.application.dto.SyncPoliciesRequest;
import com.keystone.policy.application.dto.SyncPoliciesResponse;
import com.keystone.policy.domain.event.PolicySyncedEvent;
import com.keystone.policy.domain.model.Policy;
import com.keystone.policy.domain.model.PolicyScope;
import com.keystone.policy.domain.model.PolicySeverity;
import com.keystone.policy.domain.model.PolicyStatus;
import com.keystone.policy.infrastructure.event.PolicyEventPublisher;
import com.keystone.policy.infrastructure.repository.PolicyRepository;
import com.keystone.policy.source.GitPolicySourceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PolicySyncServiceImplTest {

    @Mock
    private GitPolicySourceImpl gitPolicySource;
    @Mock
    private PolicyRepository policyRepository;
    @Mock
    private PolicyEventPublisher eventPublisher;

    @Captor
    private ArgumentCaptor<Policy> policyCaptor;

    private PolicySyncServiceImpl syncService;

    private final String sourceId = "test-source";
    private final Instant now = Instant.now();

    @BeforeEach
    void setUp() {
        syncService = new PolicySyncServiceImpl(gitPolicySource, policyRepository, eventPublisher);
    }

    @Test
    void syncPolicies_shouldAddNewPolicies() {
        var request = new SyncPoliciesRequest(sourceId, null);
        var remotePolicy = createPolicy("test-policy", 1);

        when(gitPolicySource.fetchPolicies()).thenReturn(List.of(remotePolicy));
        when(gitPolicySource.getCurrentVersion()).thenReturn("abc123");
        when(policyRepository.findPoliciesBySource(sourceId)).thenReturn(List.of());
        when(policyRepository.savePolicy(any())).thenAnswer(i -> i.getArgument(0));

        SyncPoliciesResponse response = syncService.syncPolicies(request);

        assertThat(response.success()).isTrue();
        assertThat(response.policiesAdded()).isEqualTo(1);
        assertThat(response.policiesUpdated()).isEqualTo(0);
        assertThat(response.policiesRemoved()).isEqualTo(0);

        verify(policyRepository).savePolicy(any(Policy.class));
        verify(eventPublisher).policySynced(any(PolicySyncedEvent.class));
    }

    @Test
    void syncPolicies_shouldUpdateExistingPolicies() {
        var request = new SyncPoliciesRequest(sourceId, null);
        var existingPolicy = createPolicy("existing-policy", 1);
        var updatedRemote = createPolicy("existing-policy", 2);

        when(gitPolicySource.fetchPolicies()).thenReturn(List.of(updatedRemote));
        when(gitPolicySource.getCurrentVersion()).thenReturn("def456");
        when(policyRepository.findPoliciesBySource(sourceId)).thenReturn(List.of(existingPolicy));
        when(policyRepository.updatePolicy(any())).thenAnswer(i -> i.getArgument(0));

        SyncPoliciesResponse response = syncService.syncPolicies(request);

        assertThat(response.success()).isTrue();
        assertThat(response.policiesUpdated()).isEqualTo(1);
        assertThat(response.policiesAdded()).isEqualTo(0);

        verify(policyRepository).updatePolicy(any(Policy.class));
        verify(eventPublisher).policySynced(any(PolicySyncedEvent.class));
    }

    @Test
    void syncPolicies_shouldDeactivateRemovedPolicies() {
        var request = new SyncPoliciesRequest(sourceId, null);
        var existingPolicy = createPolicy("removed-policy", 1);

        when(gitPolicySource.fetchPolicies()).thenReturn(List.of());
        when(gitPolicySource.getCurrentVersion()).thenReturn("ghi789");
        when(policyRepository.findPoliciesBySource(sourceId)).thenReturn(List.of(existingPolicy));
        when(policyRepository.findPolicyByNameAndSource(anyString(), anyString()))
                .thenReturn(java.util.Optional.of(existingPolicy));
        when(policyRepository.updatePolicy(any())).thenAnswer(i -> i.getArgument(0));

        SyncPoliciesResponse response = syncService.syncPolicies(request);

        assertThat(response.success()).isTrue();
        assertThat(response.policiesRemoved()).isEqualTo(1);

        verify(policyRepository, times(1)).updatePolicy(any(Policy.class));
    }

    @Test
    void syncPolicies_shouldHandleSourceFailure() {
        var request = new SyncPoliciesRequest("nonexistent", null);

        when(gitPolicySource.fetchPolicies())
                .thenThrow(new com.keystone.policy.domain.exception.PolicySyncException(
                        "nonexistent", "Repository not found"));

        SyncPoliciesResponse response = syncService.syncPolicies(request);

        assertThat(response.success()).isFalse();
        assertThat(response.errorMessage()).isNotNull();
    }

    @Test
    void syncPolicies_shouldUseExplicitRefWhenProvided() {
        var request = new SyncPoliciesRequest(sourceId, "develop");
        var remotePolicy = createPolicy("feature-policy", 1);

        when(gitPolicySource.fetchPoliciesFromRef("develop")).thenReturn(List.of(remotePolicy));
        when(gitPolicySource.getCurrentVersion()).thenReturn("ref123");
        when(policyRepository.findPoliciesBySource(sourceId)).thenReturn(List.of());
        when(policyRepository.savePolicy(any())).thenAnswer(i -> i.getArgument(0));

        SyncPoliciesResponse response = syncService.syncPolicies(request);

        assertThat(response.success()).isTrue();
        verify(gitPolicySource).fetchPoliciesFromRef("develop");
        verify(gitPolicySource, never()).fetchPolicies();
    }

    @Test
    void configureSource_shouldRegisterAndPublishEvent() {
        var config = new com.keystone.policy.application.dto.PolicySourceConfigRequest(
                "new-source", "git", "https://github.com/org/policies.git",
                "main", ".keystone/policies", null, true);

        syncService.configureSource(config);

        assertThat(syncService.listSources()).contains("new-source");
        verify(eventPublisher).policySourceChanged(any());
    }

    @Test
    void removeSource_shouldDeletePoliciesWhenRequested() {
        syncService.configureSource(new com.keystone.policy.application.dto.PolicySourceConfigRequest(
                "delete-source", "git", "https://example.com/repo.git",
                "main", ".keystone/policies", null, true));

        when(policyRepository.deletePoliciesBySource("delete-source")).thenReturn(3);

        syncService.removeSource("delete-source", true);

        verify(policyRepository).deletePoliciesBySource("delete-source");
        verify(eventPublisher, times(2)).policySourceChanged(any());
    }

    @Test
    void listSources_shouldReturnConfiguredSources() {
        syncService.configureSource(new com.keystone.policy.application.dto.PolicySourceConfigRequest(
                "source-a", "git", "https://example.com/a.git",
                "main", "policies", null, true));
        syncService.configureSource(new com.keystone.policy.application.dto.PolicySourceConfigRequest(
                "source-b", "git", "https://example.com/b.git",
                "main", "policies", null, true));

        var sources = syncService.listSources();

        assertThat(sources).containsExactlyInAnyOrder("source-a", "source-b");
    }

    private Policy createPolicy(String name, int version) {
        return new Policy(
                UUID.randomUUID(), name, "Test policy: " + name,
                PolicySeverity.MAJOR, PolicyStatus.ACTIVE,
                PolicyScope.all(), "each endpoint in spec.endpoints yield pass()",
                sourceId, version, now, now);
    }
}
