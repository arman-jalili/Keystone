// Canonical Reference: .pi/architecture/modules/notification-engine.md
package com.keystone.notification.infrastructure.repository;

import com.keystone.notification.domain.model.Notification;
import com.keystone.notification.domain.model.NotificationStatus;
import com.keystone.notification.infrastructure.repository.jpa.NotificationEntity;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * JPA-backed implementation of {@link NotificationRepository}.
 *
 * <p>Persists notifications to the database. Data survives restarts.
 */
@Repository
@Transactional
public class NotificationRepositoryImpl implements NotificationRepository {

    private final SpringDataNotificationRepository jpaRepository;

    public NotificationRepositoryImpl(SpringDataNotificationRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Notification> findById(UUID notificationId) {
        return jpaRepository.findById(notificationId).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> findByChannelName(String channelName, int limit) {
        return jpaRepository.findByChannelNameOrderByCreatedAtDesc(channelName, PageRequest.of(0, limit)).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> findByStatus(NotificationStatus status, int limit) {
        return jpaRepository.findByStatusOrderByCreatedAtDesc(status.name(), PageRequest.of(0, limit)).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> findFailedBetween(Instant from, Instant to, int limit) {
        return jpaRepository.findFailedBetween(from, to, PageRequest.of(0, limit)).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> findByChannelId(String channelId, int limit) {
        if (channelId == null) return List.of();
        return jpaRepository.findByChannelIdOrderByCreatedAtDesc(channelId, PageRequest.of(0, limit)).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> findAll(int limit, boolean unreadFirst) {
        if (unreadFirst) {
            var all = jpaRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, limit)).stream()
                    .map(this::toDomain)
                    .toList();
            return all.stream()
                    .sorted(Comparator.comparing((Notification n) -> n.status() == NotificationStatus.DELIVERED
                                            || n.status() == NotificationStatus.FAILED
                                    ? 1
                                    : 0)
                            .thenComparing(Comparator.comparing(Notification::createdAt)
                                    .reversed()))
                    .limit(limit)
                    .toList();
        }
        return jpaRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, limit)).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Notification save(Notification notification) {
        var entity = toEntity(notification);
        var saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public long count() {
        return jpaRepository.count();
    }

    @Override
    public int deleteOlderThan(Instant before) {
        return jpaRepository.deleteByCreatedAtBefore(before);
    }

    private Notification toDomain(NotificationEntity e) {
        return new Notification(
                e.getId(),
                e.getChannelName(),
                e.getChannelId(),
                NotificationStatus.valueOf(e.getStatus()),
                e.getMessage(),
                e.getPayloadType(),
                e.getCreatedAt());
    }

    private NotificationEntity toEntity(Notification n) {
        return new NotificationEntity(
                n.id(), n.channelName(), n.channelId(), n.status().name(), n.message(), n.payloadType(), n.createdAt());
    }
}
