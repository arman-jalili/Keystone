// Canonical Reference: .pi/architecture/modules/notification-engine.md
// Module: notification-engine
package com.keystone.notification.application.dto;

import java.util.List;
import java.util.Objects;

/**
 * Response DTO listing the status of all registered notification channels.
 *
 * @param channels The list of channel status entries
 * @param total    Total number of registered channels
 * @param available Number of channels currently available for delivery
 */
public record ChannelStatusResponse(List<ChannelStatusEntry> channels, int total, int available) {
    public ChannelStatusResponse {
        Objects.requireNonNull(channels, "channels must not be null");
    }

    /**
     * Status entry for a single notification channel.
     *
     * @param name      The channel name (e.g. "CI_STATUS")
     * @param available Whether the channel is currently available
     */
    public record ChannelStatusEntry(String name, boolean available) {
        public ChannelStatusEntry {
            Objects.requireNonNull(name, "name must not be null");
        }
    }
}
