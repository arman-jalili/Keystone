package com.keystone.policy.infrastructure.source;

import com.keystone.policy.domain.exception.PolicySyncException;
import com.keystone.policy.domain.model.Policy;
import java.util.List;

/**
 * Interface for a policy source that provides policy definitions.
 *
 * <p>A policy source is an external origin from which policies are
 * loaded and synchronized. The primary implementation is
 * {@link GitPolicySource} which reads policies from a Git repository.
 *
 * <p>Each source returns a list of parsed {@link Policy} objects that
 * can be persisted and evaluated. The source is responsible for
 * fetching, parsing, and basic validation of policy DSL files.
 */
public interface PolicySource {

    /**
     * Returns a unique identifier for this source instance.
     *
     * @return the source identifier (e.g. "org-policies")
     */
    String getSourceId();

    /**
     * Returns the type of this source.
     *
     * @return the source type (e.g. "git", "local", "http")
     */
    String getSourceType();

    /**
     * Fetches and parses all policies from this source.
     *
     * <p>Implementations should be resilient to partial failures:
     * if some policy files fail to parse, the valid ones should
     * still be returned. Use {@link com.keystone.policy.domain.exception.PolicySyncException}
     * to report partial failures.
     *
     * @return the list of parsed policies from this source
     * @throws PolicySyncException if the source cannot be reached or all policies fail to load
     */
    List<Policy> fetchPolicies() throws PolicySyncException;

    /**
     * Fetches and parses a single policy by name from this source.
     *
     * <p>Searches the source's policy files for a policy with the given name.
     * If no match is found, throws {@link com.keystone.policy.domain.exception.PolicyNotFoundException}.
     *
     * @param name the name of the policy to fetch
     * @return the parsed policy
     * @throws PolicySyncException if the source cannot be reached
     * @throws com.keystone.policy.domain.exception.PolicyNotFoundException if no policy with the given name exists
     */
    Policy getPolicy(String name) throws PolicySyncException;

    /**
     * Returns the current version identifier for this source.
     *
     * <p>For Git sources, this is typically the commit SHA of HEAD.
     * Used for caching and change detection.
     *
     * @return the current source version string
     * @throws PolicySyncException if the version cannot be determined
     */
    String getCurrentVersion() throws PolicySyncException;

    /**
     * Checks whether the source has changed since the last sync.
     *
     * @param lastKnownVersion the version string from the last successful sync
     * @return true if the source has new content
     * @throws PolicySyncException if the source cannot be checked
     */
    boolean hasChanged(String lastKnownVersion) throws PolicySyncException;
}
