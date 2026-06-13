// Canonical Reference: .pi/architecture/modules/notification-engine.md
// Module: notification-engine
package com.keystone.notification.domain.service;

import com.keystone.notification.domain.channel.NotificationChannel;
import java.util.List;
import java.util.Optional;

/**
 * Registry interface for managing available {@link NotificationChannel} instances.
 *
 * <p>Channels are registered at startup and can be dynamically added or
 * removed at runtime. The registry is consumed by
 * {@link com.keystone.notification.application.service.NotificationDispatcher}
 * to retrieve all channels for event dispatch.
 *
 * <p>Implementations may auto-discover {@code @Component} channels via
 * Spring dependency injection, or provide a manual registration mechanism.
 */
public interface ChannelRegistry {

    /**
     * Returns all registered notification channels.
     *
     * @return list of registered channels (never null, may be empty)
     */
    List<NotificationChannel> getAllChannels();

    /**
     * Returns the names of all registered channels.
     *
     * @return list of channel names
     */
    List<String> getChannelNames();

    /**
     * Finds a channel by its unique name.
     *
     * @param name the channel name (e.g. "CI_STATUS", "EMAIL", "SLACK")
     * @return the channel, or empty if not found
     */
    Optional<NotificationChannel> getChannel(String name);

    /**
     * Registers a notification channel, replacing any existing channel
     * with the same name.
     *
     * @param channel the channel to register
     */
    void register(NotificationChannel channel);

    /**
     * Removes a notification channel by name.
     *
     * @param name the channel name to remove
     * @return true if a channel was removed
     */
    boolean unregister(String name);

    /**
     * Returns the number of currently registered channels.
     *
     * @return the channel count
     */
    int channelCount();

    /**
     * Returns true if at least one channel is registered and available.
     *
     * @return true if any channel is available
     */
    boolean hasAvailableChannels();
}
