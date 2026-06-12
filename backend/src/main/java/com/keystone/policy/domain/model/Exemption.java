package com.keystone.policy.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Value object representing a time-bound exemption from a policy rule.
 *
 * <p>Exemptions allow certain changes to bypass policy violations for a
 * limited period. They must be approved by a compliance manager and
 * have an explicit expiry date.
 *
 * @param id              Unique identifier for this exemption
 * @param policyId        The UUID of the exempted policy
 * @param changeId        The identifier of the specific change being exempted
 * @param grantedBy       The user who granted the exemption
 * @param reason          The reason for granting the exemption
 * @param expiresAt       When the exemption expires
 * @param grantedAt       When the exemption was granted
 * @param revokedAt       When the exemption was revoked (null if active)
 * @param active          Whether the exemption is currently active
 */
public record Exemption(
        UUID id,
        UUID policyId,
        String changeId,
        String grantedBy,
        String reason,
        Instant expiresAt,
        Instant grantedAt,
        Instant revokedAt,
        boolean active) {
    public Exemption {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(policyId, "policyId must not be null");
        Objects.requireNonNull(changeId, "changeId must not be null");
        Objects.requireNonNull(grantedBy, "grantedBy must not be null");
        Objects.requireNonNull(reason, "reason must not be null");
        Objects.requireNonNull(expiresAt, "expiresAt must not be null");
        Objects.requireNonNull(grantedAt, "grantedAt must not be null");
    }

    /**
     * Returns true if this exemption is still valid (active and not expired).
     */
    public boolean isValid() {
        return active && Instant.now().isBefore(expiresAt) && revokedAt == null;
    }

    /**
     * Returns true if this exemption has expired.
     */
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}
