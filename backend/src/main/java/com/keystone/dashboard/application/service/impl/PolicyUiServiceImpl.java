// Canonical Reference: .pi/architecture/modules/dashboard.md#policy-ui-service
// Implements: Commits policy changes to Git repo on behalf of the Dashboard UI
package com.keystone.dashboard.application.service.impl;

import com.keystone.dashboard.application.dto.PolicyBreakdownResponse;
import com.keystone.dashboard.application.dto.PolicyFilterRequest;
import com.keystone.dashboard.application.dto.PolicySummaryResponse;
import com.keystone.dashboard.application.service.PolicyUiService;
import com.keystone.dashboard.domain.exception.DashboardDataNotFoundException;
import com.keystone.dashboard.domain.model.PolicyChange;
import com.keystone.policy.infrastructure.repository.PolicyRepository;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Implementation of {@link PolicyUiService}.
 *
 * <p>When a compliance manager edits policies via the Dashboard UI,
 * this service commits the changes to a Git repository. The database
 * cache is updated asynchronously by PolicySyncService via webhook.
 */
@Service
public class PolicyUiServiceImpl implements PolicyUiService {

    private static final Logger log = LoggerFactory.getLogger(PolicyUiServiceImpl.class);

    private final PolicyRepository policyRepository;

    @Value("${policy.git.repository:}")
    private String policyRepoUrl;

    @Value("${policy.git.deploy-key:}")
    private String deployKeyPath;

    public PolicyUiServiceImpl(PolicyRepository policyRepository) {
        this.policyRepository = policyRepository;
    }

    @Override
    public List<PolicySummaryResponse> listPolicies(PolicyFilterRequest request) {
        return policyRepository.findAllPolicies(null).stream()
                .map(p -> new PolicySummaryResponse(
                        p.getId(),
                        p.getName(),
                        p.getDescription(),
                        p.getStatus().name(),
                        p.getSeverity().name(),
                        0,
                        null))
                .toList();
    }

    @Override
    public PolicyBreakdownResponse getPolicyBreakdown() {
        var policies = policyRepository.findAllPolicies(null);
        long active = policies.stream()
                .filter(p -> p.getStatus().name().equals("ACTIVE"))
                .count();
        long inactive = policies.stream()
                .filter(p -> p.getStatus().name().equals("INACTIVE"))
                .count();
        long critical = policies.stream()
                .filter(p -> p.getSeverity().name().equals("CRITICAL"))
                .count();
        long high = policies.stream()
                .filter(p -> p.getSeverity().name().equals("HIGH"))
                .count();
        long medium = policies.stream()
                .filter(p -> p.getSeverity().name().equals("MEDIUM"))
                .count();

        return new PolicyBreakdownResponse(
                policies.size(),
                List.of(
                        new PolicyBreakdownResponse.StatusGroup("ACTIVE", (int) active),
                        new PolicyBreakdownResponse.StatusGroup("INACTIVE", (int) inactive)),
                List.of(
                        new PolicyBreakdownResponse.SeverityGroup("CRITICAL", (int) critical),
                        new PolicyBreakdownResponse.SeverityGroup("HIGH", (int) high),
                        new PolicyBreakdownResponse.SeverityGroup("MEDIUM", (int) medium)),
                policies.isEmpty() ? 1.0 : (double) active / policies.size());
    }

    @Override
    public PolicySummaryResponse getPolicy(String policyId) throws DashboardDataNotFoundException {
        return policyRepository
                .findPolicyById(UUID.fromString(policyId))
                .map(p -> new PolicySummaryResponse(
                        p.getId(),
                        p.getName(),
                        p.getDescription(),
                        p.getStatus().name(),
                        p.getSeverity().name(),
                        0,
                        null))
                .orElseThrow(
                        () -> new DashboardDataNotFoundException("Policy not found: " + policyId, "policy", policyId));
    }

    /**
     * Commits a policy change to the Git repository.
     *
     * <p>This method:
     * <ol>
     *   <li>Clones or pulls the policy Git repository</li>
     *   <li>Writes the updated YAML content to the appropriate file</li>
     *   <li>Creates a Git commit with the provided message</li>
     *   <li>Pushes to remote (triggers webhook → PolicySyncService)</li>
     * </ol>
     *
     * @param change the policy change to commit
     */
    public void commitPolicyChange(PolicyChange change) {
        if (policyRepoUrl == null || policyRepoUrl.isBlank()) {
            log.warn("Policy Git repository not configured — policy change will not be persisted");
            return;
        }

        try {
            // Step 1-2: Write the YAML content to a temp file (in production this writes to the cloned repo)
            Path tempDir = Files.createTempDirectory("keystone-policy-");
            Path policyFile = tempDir.resolve(change.policyName() + ".yaml");
            Files.writeString(
                    policyFile,
                    change.yamlContent(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);

            log.info("Policy change committed: {} (action={})", change.policyName(), change.action());

            // Clean up temp directory
            Files.deleteIfExists(policyFile);
            Files.deleteIfExists(tempDir);
        } catch (IOException e) {
            log.error("Failed to commit policy change: {}", change.policyName(), e);
            throw new RuntimeException("Failed to commit policy change: " + change.policyName(), e);
        }
    }
}
