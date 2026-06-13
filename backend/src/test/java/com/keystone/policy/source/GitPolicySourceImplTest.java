// Canonical Reference: .pi/architecture/modules/policy-engine.md
package com.keystone.policy.source;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.keystone.policy.domain.exception.PolicyNotFoundException;
import com.keystone.policy.domain.exception.PolicySyncException;
import com.keystone.policy.domain.model.Policy;
import com.keystone.policy.domain.model.PolicySeverity;
import com.keystone.policy.validator.PolicyValidator;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GitPolicySourceImplTest {

    @TempDir
    Path tempDir;

    private GitPolicySourceImpl gitSource;

    @BeforeEach
    void setUp() {
        gitSource = new GitPolicySourceImpl(
                "test-source", "", "main", ".keystone/policies", tempDir.toString(), new PolicyValidator());
    }

    // ---- Identity/Config ----

    @Test
    void getSourceId_shouldReturnConfiguredId() {
        assertThat(gitSource.getSourceId()).isEqualTo("test-source");
    }

    @Test
    void getSourceType_shouldReturnGit() {
        assertThat(gitSource.getSourceType()).isEqualTo("git");
    }

    @Test
    void getRepositoryUrl_shouldReturnConfiguredUrl() {
        assertThat(gitSource.getRepositoryUrl()).isEmpty();
    }

    @Test
    void getBranch_shouldReturnConfiguredBranch() {
        assertThat(gitSource.getBranch()).isEqualTo("main");
    }

    @Test
    void getPolicyDirectory_shouldReturnConfiguredPath() {
        assertThat(gitSource.getPolicyDirectory()).isEqualTo(".keystone/policies");
    }

    @Test
    void getDefaultSeverity_shouldReturnMajor() {
        assertThat(gitSource.getDefaultSeverity()).isEqualTo(PolicySeverity.MAJOR);
    }

    // ---- fetchPolicies ----

    @Test
    void fetchPolicies_shouldReturnEmptyListWhenNoRepoConfigured() {
        List<Policy> policies = gitSource.fetchPolicies();
        assertThat(policies).isEmpty();
    }

    @Test
    void fetchPoliciesFromRef_shouldReturnEmptyWhenNoRepoConfigured() {
        List<Policy> policies = gitSource.fetchPoliciesFromRef("main");
        assertThat(policies).isEmpty();
    }

    // ---- getPolicy ----

    @Test
    void getPolicy_shouldThrowNotFoundWhenNoRepoConfigured() {
        assertThatThrownBy(() -> gitSource.getPolicy("test-rule"))
                .isInstanceOf(PolicyNotFoundException.class)
                .hasMessageContaining("test-rule");
    }

    // ---- Versioning ----

    @Test
    void getCurrentVersion_shouldThrowWhenNoClone() {
        assertThatThrownBy(() -> gitSource.getCurrentVersion()).isInstanceOf(PolicySyncException.class);
    }

    @Test
    void getHeadCommitSha_shouldThrowWhenNoClone() {
        assertThatThrownBy(() -> gitSource.getHeadCommitSha()).isInstanceOf(PolicySyncException.class);
    }

    @Test
    void hasChanged_shouldThrowWhenNoClone() {
        assertThatThrownBy(() -> gitSource.hasChanged("abc123")).isInstanceOf(PolicySyncException.class);
    }
}
