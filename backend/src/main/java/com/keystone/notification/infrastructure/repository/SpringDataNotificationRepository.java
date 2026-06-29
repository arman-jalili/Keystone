// Canonical Reference: .pi/architecture/modules/notification-engine.md
package com.keystone.notification.infrastructure.repository;

import com.keystone.notification.infrastructure.repository.jpa.NotificationEntity;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link NotificationEntity}.
 */
@Repository
public interface SpringDataNotificationRepository extends JpaRepository<NotificationEntity, UUID> {

    @Query("SELECT n FROM NotificationEntity n WHERE n.channelName = :channelName ORDER BY n.createdAt DESC")
    List<NotificationEntity> findByChannelNameOrderByCreatedAtDesc(
            @Param("channelName") String channelName, Pageable pageable);

    @Query("SELECT n FROM NotificationEntity n WHERE n.status = :status ORDER BY n.createdAt DESC")
    List<NotificationEntity> findByStatusOrderByCreatedAtDesc(@Param("status") String status, Pageable pageable);

    @Query(
            "SELECT n FROM NotificationEntity n WHERE n.status = 'FAILED' AND n.createdAt >= :from AND n.createdAt < :to ORDER BY n.createdAt DESC")
    List<NotificationEntity> findFailedBetween(@Param("from") Instant from, @Param("to") Instant to, Pageable pageable);

    @Query("SELECT n FROM NotificationEntity n WHERE n.channelId = :channelId ORDER BY n.createdAt DESC")
    List<NotificationEntity> findByChannelIdOrderByCreatedAtDesc(
            @Param("channelId") String channelId, Pageable pageable);

    @Query("SELECT n FROM NotificationEntity n ORDER BY n.createdAt DESC")
    List<NotificationEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);

    long count();

    @Query("DELETE FROM NotificationEntity n WHERE n.createdAt < :before")
    int deleteByCreatedAtBefore(@Param("before") Instant before);
}
