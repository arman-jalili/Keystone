package com.keystone.policy.domain.model;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Aggregate root representing a named, versioned collection of policies.
 *
 * <p>A PolicySet groups related policies that are evaluated together.
 * PolicySets are the unit of sync, versioning, and activation.
 *
 * <p>Examples:
 * <ul>
 *   <li>{@code "breaking-change-rules"} — policies detecting breaking API changes</li>
 *   <li>{@code "naming-conventions"} — policies enforcing naming standards</li>
 *   <li>{@code "security-best-practices"} — policies for API security requirements</li>
 * </ul>
 */
public class PolicySet {

    private final UUID id;
    private final String name;
    private final String description;
    private final List<Policy> policies;
    private final int version;
    private final Instant createdAt;
    private final Instant updatedAt;

    public PolicySet(
            UUID id,
            String name,
            String description,
            List<Policy> policies,
            int version,
            Instant createdAt,
            Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.description = description;
        this.policies = List.copyOf(Objects.requireNonNull(policies, "policies must not be null"));
        this.version = version;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public List<Policy> getPolicies() {
        return Collections.unmodifiableList(policies);
    }

    public int getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public List<Policy> getActivePolicies() {
        return policies.stream().filter(Policy::isActive).toList();
    }

    public boolean isEmpty() {
        return policies.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PolicySet policySet)) return false;
        return Objects.equals(id, policySet.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "PolicySet{id=" + id + ", name='" + name + "', policies=" + policies.size() + "}";
    }
}
