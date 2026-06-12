package com.keystone.notification.infrastructure.repository;

import com.keystone.notification.domain.model.Notification;
import com.keystone.notification.domain.model.NotificationStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for accessing {@link Notification} entities.
 *
 * <p>This is the data access contract for the notification bounded context.
 * The implementation may use Spring Data JPA, raw JDBC, or any other
 * persistence mechanism. Callers must not depend on implementation
 * details such as table names or column mappings.
 *
 * <p>This interface intentionally follows repository pattern conventions
 * that are compatible with Spring Data JPA for the initial implementation,
 * but is defined as a plain interface to keep the contract framework-agnostic.
 */
public interface NotificationRepository {

    /**
     * Finds a notification by its unique identifier.
     *
     * @param notificationId the notification UUID
     * @return the notification if found, or empty if not
     */
    Optional<Notification> findById(UUID notificationId);

    /**
     * Returns all notifications delivered through a specific channel,
     * ordered by creation timestamp descending.
     *
     * @param channelName the channel name
     * @param limit       the maximum number of results to return
     * @return list of notifications for that channel
     */
    List<Notification> findByChannelName(String channelName, int limit);

    /**
     * Returns all notifications with a specific delivery status.
     *
     * @param status the notification status to filter by
     * @param limit  the maximum number of results to return
     * @return list of notifications with that status
     */
    List<Notification> findByStatus(NotificationStatus status, int limit);

    /**
     * Returns failed notifications created between the given timestamps.
     * Used by the retry mechanism to find notifications eligible for replay.
     *
     * @param from start of the time range (inclusive)
     * @param to   end of the time range (exclusive)
     * @param limit the maximum number of results to return
     * @return list of failed notifications in the time range
     */
    List<Notification> findFailedBetween(Instant from, Instant to, int limit);

    /**
     * Returns all notifications for a given channel ID (e.g. commit SHA),
     * ordered by creation timestamp descending.
     *
     * @param channelId the channel-specific identifier
     * @param limit     the maximum number of results to return
     * @return list of notifications for that channel ID
     */
    List<Notification> findByChannelId(String channelId, int limit);

    /**
     * Persists a new notification record.
     *
     * @param notification the notification to save
     * @return the saved notification with any generated fields populated
     */
    Notification save(Notification notification);

    /**
     * Returns the total number of notifications in the data store.
     *
     * @return the notification count
     */
    long count();

    /**
     * Deletes notification records older than the given timestamp.
     * Used by the retention policy to prevent unbounded storage growth.
     *
     * @param before the cutoff timestamp; records older than this are deleted
     * @return the number of deleted records
     */
    int deleteOlderThan(Instant before);
}
