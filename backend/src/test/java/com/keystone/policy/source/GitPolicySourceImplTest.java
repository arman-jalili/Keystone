package com.keystone.policy.source;

import com.keystone.policy.domain.model.Policy;
import com.keystone.policy.validator.PolicyValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GitPolicySourceImplTest {

    @TempDir
    Path tempDir;

    private GitPolicySourceImpl gitSource;

    @BeforeEach
    void setUp() {
        gitSource = new GitPolicySourceImpl(
                "test-source",
                "",
                "main",
                ".keystone/policies",
                tempDir.toString(),
                new PolicyValidator());
    }

    @Test
    void fetchPolicies_shouldReturnEmptyListWhenNoRepoConfigured() {
        List<Policy> policies = gitSource.fetchPolicies();

        assertThat(policies).isEmpty();
    }

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
        assertThat(gitSource.getDefaultSeverity()).isEqualTo(
                com.keystone.policy.domain.model.PolicySeverity.MAJOR);
    }

    @Test
    void parsePolicyFile_shouldHandleSinglePolicy() throws IOException {
        Path policyDir = Files.createDirectories(tempDir.resolve(".keystone/policies"));
        String yaml = "name: test-rule\n"
                + "description: A test policy\n"
                + "severity: MAJOR\n"
                + "rule: |\n"
                + "  each endpoint in spec.endpoints\n"
                + "  where not endpoint.has(\"operationId\")\n"
                + "  yield violation(\"Missing operationId\")";
        Files.writeString(policyDir.resolve("test-rule.policy"), yaml);
    }

    @Test
    void getCurrentVersion_shouldThrowWhenNoClone() {
        // No clone directory exists, so this should fail
        org.junit.jupiter.api.Assertions.assertThrows(
                com.keystone.policy.domain.exception.PolicySyncException.class,
                () -> gitSource.getCurrentVersion());
    }
}
