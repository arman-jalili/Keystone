package com.keystone.notification.infrastructure.repository;

import com.keystone.notification.domain.model.Notification;
import com.keystone.notification.domain.model.NotificationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory implementation of {@link NotificationRepository}.
 *
 * <p>Uses a {@link ConcurrentHashMap} for storage. Suitable for development
 * and testing. Will be replaced by a JPA-backed implementation in production.
 */
@Repository
public class NotificationRepositoryImpl implements NotificationRepository {

    private static final Logger log = LoggerFactory.getLogger(NotificationRepositoryImpl.class);

    private final Map<UUID, Notification> store = new ConcurrentHashMap<>();

    @Override
    public Optional<Notification> findById(UUID notificationId) {
        return Optional.ofNullable(store.get(notificationId));
    }

    @Override
    public List<Notification> findByChannelName(String channelName, int limit) {
        return store.values().stream()
                .filter(n -> n.channelName().equals(channelName))
                .sorted((a, b) -> b.createdAt().compareTo(a.createdAt()))
                .limit(limit)
                .toList();
    }

    @Override
    public List<Notification> findByStatus(NotificationStatus status, int limit) {
        return store.values().stream()
                .filter(n -> n.status() == status)
                .sorted((a, b) -> b.createdAt().compareTo(a.createdAt()))
                .limit(limit)
                .toList();
    }

    @Override
    public List<Notification> findFailedBetween(Instant from, Instant to, int limit) {
        return store.values().stream()
                .filter(n -> n.status() == NotificationStatus.FAILED)
                .filter(n -> !n.createdAt().isBefore(from) && n.createdAt().isBefore(to))
                .sorted((a, b) -> b.createdAt().compareTo(a.createdAt()))
                .limit(limit)
                .toList();
    }

    @Override
    public List<Notification> findByChannelId(String channelId, int limit) {
        if (channelId == null) {
            return List.of();
        }
        return store.values().stream()
                .filter(n -> channelId.equals(n.channelId()))
                .sorted((a, b) -> b.createdAt().compareTo(a.createdAt()))
                .limit(limit)
                .toList();
    }

    @Override
    public Notification save(Notification notification) {
        UUID id = notification.id() != null ? notification.id() : UUID.randomUUID();
        Notification toSave = new Notification(
                id,
                notification.channelName(),
                notification.channelId(),
                notification.status(),
                notification.message(),
                notification.payloadType(),
                notification.createdAt()
        );
        store.put(id, toSave);
        log.trace("Saved notification {} with status {}", id, notification.status());
        return toSave;
    }

    @Override
    public long count() {
        return store.size();
    }

    @Override
    public int deleteOlderThan(Instant before) {
        List<UUID> toRemove = store.values().stream()
                .filter(n -> n.createdAt().isBefore(before))
                .map(Notification::id)
                .toList();
        toRemove.forEach(store::remove);
        log.debug("Deleted {} notifications older than {}", toRemove.size(), before);
        return toRemove.size();
    }
}
