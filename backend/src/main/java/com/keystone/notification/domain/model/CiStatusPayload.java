package com.keystone.notification.domain.model;

import java.util.Objects;

/**
 * Value object representing a CI status update posted to a Git provider
 * (GitHub / GitLab) commit status API.
 *
 * @param state       The commit status state: "pending", "success", "failure", or "error"
 * @param description A short human-readable description of the status
 * @param targetUrl   Optional URL linking to the build or analysis details
 * @param context     The CI context label (e.g. "keystone/governance")
 * @param owner       The repository owner (e.g. "my-org")
 * @param repo        The repository name (e.g. "my-service")
 * @param sha         The full commit SHA this status applies to
 */
public record CiStatusPayload(
    String state,
    String description,
    String targetUrl,
    String context,
    String owner,
    String repo,
    String sha
) {
    public CiStatusPayload {
        Objects.requireNonNull(state, "state must not be null");
        Objects.requireNonNull(description, "description must not be null");
        Objects.requireNonNull(context, "context must not be null");
        Objects.requireNonNull(owner, "owner must not be null");
        Objects.requireNonNull(repo, "repo must not be null");
        Objects.requireNonNull(sha, "sha must not be null");
    }

    /**
     * Returns true if this payload represents a terminal state (success, failure, or error).
     */
    public boolean isTerminal() {
        return "success".equals(state) || "failure".equals(state) || "error".equals(state);
    }

    /**
     * Returns the full API path: /repos/{owner}/{repo}/statuses/{sha}
     */
    public String apiPath() {
        return "/repos/" + owner + "/" + repo + "/statuses/" + sha;
    }
}
