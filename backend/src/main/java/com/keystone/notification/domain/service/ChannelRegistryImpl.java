package com.keystone.notification.domain.service;

import com.keystone.notification.domain.channel.NotificationChannel;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Default implementation of {@link ChannelRegistry}.
 *
 * <p>Uses a {@link ConcurrentHashMap} for thread-safe channel storage.
 * Channels are registered via Spring dependency injection (auto-discovered
 * {@code @Component} channels) or programmatically via {@link #register}.
 */
@Component
public class ChannelRegistryImpl implements ChannelRegistry {

    private static final Logger log = LoggerFactory.getLogger(ChannelRegistryImpl.class);

    private final Map<String, NotificationChannel> channels = new ConcurrentHashMap<>();

    public ChannelRegistryImpl(List<NotificationChannel> discoveredChannels) {
        for (NotificationChannel channel : discoveredChannels) {
            channels.put(channel.getName(), channel);
            log.info("Registered notification channel: {}", channel.getName());
        }
    }

    @Override
    public List<NotificationChannel> getAllChannels() {
        return List.copyOf(channels.values());
    }

    @Override
    public List<String> getChannelNames() {
        return List.copyOf(channels.keySet());
    }

    @Override
    public Optional<NotificationChannel> getChannel(String name) {
        return Optional.ofNullable(channels.get(name));
    }

    @Override
    public void register(NotificationChannel channel) {
        channels.put(channel.getName(), channel);
        log.info("Registered notification channel: {}", channel.getName());
    }

    @Override
    public boolean unregister(String name) {
        NotificationChannel removed = channels.remove(name);
        if (removed != null) {
            log.info("Unregistered notification channel: {}", name);
            return true;
        }
        return false;
    }

    @Override
    public int channelCount() {
        return channels.size();
    }

    @Override
    public boolean hasAvailableChannels() {
        return channels.values().stream().anyMatch(NotificationChannel::isAvailable);
    }
}
