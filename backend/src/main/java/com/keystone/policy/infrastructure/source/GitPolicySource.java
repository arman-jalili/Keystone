package com.keystone.policy.infrastructure.source;

import com.keystone.policy.domain.exception.PolicySyncException;
import com.keystone.policy.domain.model.Policy;
import com.keystone.policy.domain.model.PolicySeverity;

import java.util.List;

/**
 * Interface for a policy source backed by a Git repository.
 *
 * <p>Reads policy DSL files from a configured Git repository path.
 * Supports branch-specific policies, webhook-triggered syncs, and
 * version tracking via commit SHA.
 *
 * <p>Policies are stored as {@code .policy} files in a designated
 * directory within the repository (e.g. {@code .keystone/policies/}).
 * Each file may contain one or more policy rules.
 *
 * <p>Configuration parameters (set during implementation):
 * <ul>
 *   <li>Repository URL (HTTPS or SSH)</li>
 *   <li>Branch name</li>
 *   <li>Policy directory path</li>
 *   <li>Authentication credentials (token or SSH key)</li>
 *   <li>Webhook secret for push event validation</li>
 * </ul>
 */
public interface GitPolicySource extends PolicySource {

    /**
     * Returns the Git repository URL.
     *
     * @return the repository URL
     */
    String getRepositoryUrl();

    /**
     * Returns the configured branch name.
     *
     * @return the branch name (e.g. "main", "develop")
     */
    String getBranch();

    /**
     * Returns the directory path within the repository where
     * policy files are stored.
     *
     * @return the policy directory path (e.g. ".keystone/policies")
     */
    String getPolicyDirectory();

    /**
     * Fetches policies from a specific branch or tag ref,
     * overriding the configured default branch.
     *
     * @param ref the git ref (branch name, tag, or commit SHA)
     * @return the list of parsed policies
     * @throws PolicySyncException if fetching from the ref fails
     */
    List<Policy> fetchPoliciesFromRef(String ref) throws PolicySyncException;

    /**
     * Returns the commit SHA of the latest version on the configured branch.
     *
     * @return the full commit SHA
     * @throws PolicySyncException if the commit cannot be resolved
     */
    String getHeadCommitSha() throws PolicySyncException;

    /**
     * Returns the default severity to apply to policies that do not
     * specify a severity in their DSL definition.
     *
     * @return the default severity (default: {@link PolicySeverity#MAJOR})
     */
    PolicySeverity getDefaultSeverity();
}
