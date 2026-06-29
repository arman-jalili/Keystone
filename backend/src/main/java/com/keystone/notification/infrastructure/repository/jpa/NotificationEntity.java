// Canonical Reference: .pi/architecture/modules/notification-engine.md
package com.keystone.notification.infrastructure.repository.jpa;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * JPA entity for the {@code notifications} table.
 */
@Entity
@Table(name = "notifications")
public class NotificationEntity {

    @Id
    private UUID id;

    @Column(name = "channel_name", nullable = false, length = 128)
    private String channelName;

    @Column(name = "channel_id", length = 256)
    private String channelId;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(length = 2048)
    private String message;

    @Column(name = "payload_type", nullable = false, length = 128)
    private String payloadType;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected NotificationEntity() {}

    public NotificationEntity(
            UUID id,
            String channelName,
            String channelId,
            String status,
            String message,
            String payloadType,
            Instant createdAt) {
        this.id = Objects.requireNonNull(id);
        this.channelName = Objects.requireNonNull(channelName);
        this.channelId = channelId;
        this.status = Objects.requireNonNull(status);
        this.message = message;
        this.payloadType = Objects.requireNonNull(payloadType);
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    public UUID getId() {
        return id;
    }

    public String getChannelName() {
        return channelName;
    }

    public String getChannelId() {
        return channelId;
    }

    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public String getPayloadType() {
        return payloadType;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
