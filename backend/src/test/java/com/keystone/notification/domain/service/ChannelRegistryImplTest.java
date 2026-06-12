package com.keystone.notification.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.keystone.notification.domain.channel.NotificationChannel;
import com.keystone.notification.domain.model.Notification;
import com.keystone.notification.domain.model.NotificationStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ChannelRegistryImplTest {

    private ChannelRegistryImpl registry;
    private TestChannel channel1;
    private TestChannel channel2;

    @BeforeEach
    void setUp() {
        channel1 = new TestChannel("CHANNEL_1", true);
        channel2 = new TestChannel("CHANNEL_2", false);
        registry = new ChannelRegistryImpl(List.of(channel1, channel2));
    }

    @Test
    void constructor_shouldRegisterAllDiscoveredChannels() {
        assertThat(registry.channelCount()).isEqualTo(2);
        assertThat(registry.getChannelNames()).containsExactlyInAnyOrder("CHANNEL_1", "CHANNEL_2");
    }

    @Test
    void constructor_shouldHandleEmptyChannelList() {
        ChannelRegistryImpl emptyRegistry = new ChannelRegistryImpl(List.of());
        assertThat(emptyRegistry.channelCount()).isZero();
    }

    @Test
    void getAllChannels_shouldReturnAllRegisteredChannels() {
        List<NotificationChannel> channels = registry.getAllChannels();

        assertThat(channels).hasSize(2);
        assertThat(channels)
                .extracting(NotificationChannel::getName)
                .containsExactlyInAnyOrder("CHANNEL_1", "CHANNEL_2");
    }

    @Test
    void getAllChannels_shouldReturnImmutableCopy() {
        List<NotificationChannel> channels = registry.getAllChannels();
        assertThat(channels).isNotEmpty();
    }

    @Test
    void getChannelNames_shouldReturnAllNames() {
        List<String> names = registry.getChannelNames();

        assertThat(names).hasSize(2);
        assertThat(names).containsExactlyInAnyOrder("CHANNEL_1", "CHANNEL_2");
    }

    @Test
    void getChannel_shouldReturnChannelByName() {
        Optional<NotificationChannel> result = registry.getChannel("CHANNEL_1");

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("CHANNEL_1");
    }

    @Test
    void getChannel_shouldReturnEmptyForUnknownName() {
        Optional<NotificationChannel> result = registry.getChannel("NONEXISTENT");

        assertThat(result).isEmpty();
    }

    @Test
    void register_shouldAddNewChannel() {
        TestChannel channel3 = new TestChannel("CHANNEL_3", true);
        registry.register(channel3);

        assertThat(registry.channelCount()).isEqualTo(3);
        assertThat(registry.getChannel("CHANNEL_3")).isPresent();
    }

    @Test
    void register_shouldReplaceExistingChannelWithSameName() {
        TestChannel replacement = new TestChannel("CHANNEL_1", false);
        registry.register(replacement);

        assertThat(registry.channelCount()).isEqualTo(2);
        assertThat(registry.getChannel("CHANNEL_1")).isPresent();
    }

    @Test
    void unregister_shouldRemoveChannel() {
        boolean removed = registry.unregister("CHANNEL_1");

        assertThat(removed).isTrue();
        assertThat(registry.channelCount()).isEqualTo(1);
        assertThat(registry.getChannel("CHANNEL_1")).isEmpty();
    }

    @Test
    void unregister_shouldReturnFalseForUnknownName() {
        boolean removed = registry.unregister("NONEXISTENT");

        assertThat(removed).isFalse();
        assertThat(registry.channelCount()).isEqualTo(2);
    }

    @Test
    void channelCount_shouldTrackRegistrationCount() {
        assertThat(registry.channelCount()).isEqualTo(2);

        registry.register(new TestChannel("CHANNEL_3", true));
        assertThat(registry.channelCount()).isEqualTo(3);

        registry.unregister("CHANNEL_1");
        assertThat(registry.channelCount()).isEqualTo(2);
    }

    @Test
    void hasAvailableChannels_shouldReturnTrueWhenAtLeastOneAvailable() {
        // CHANNEL_1 is available, CHANNEL_2 is not
        assertThat(registry.hasAvailableChannels()).isTrue();
    }

    @Test
    void hasAvailableChannels_shouldReturnFalseWhenNoneAvailable() {
        ChannelRegistryImpl unavailableRegistry = new ChannelRegistryImpl(
                List.of(new TestChannel("UNAVAIL_1", false), new TestChannel("UNAVAIL_2", false)));

        assertThat(unavailableRegistry.hasAvailableChannels()).isFalse();
    }

    @Test
    void hasAvailableChannels_shouldReturnFalseWhenEmpty() {
        ChannelRegistryImpl emptyRegistry = new ChannelRegistryImpl(List.of());
        assertThat(emptyRegistry.hasAvailableChannels()).isFalse();
    }

    @Test
    void registerAndUnregister_shouldWorkCorrectly() {
        assertThat(registry.getChannel("CHANNEL_1")).isPresent();

        registry.unregister("CHANNEL_1");
        assertThat(registry.getChannel("CHANNEL_1")).isEmpty();

        TestChannel reRegistered = new TestChannel("CHANNEL_1", true);
        registry.register(reRegistered);
        assertThat(registry.getChannel("CHANNEL_1")).isPresent();
        assertThat(registry.channelCount()).isEqualTo(2);
    }

    // ---- Test channel implementation ----

    private static class TestChannel implements NotificationChannel {
        private final String name;
        private final boolean available;

        TestChannel(String name, boolean available) {
            this.name = name;
            this.available = available;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Notification send(Object event) {
            return new Notification(
                    UUID.randomUUID(), name, null, NotificationStatus.DELIVERED, "test", "test", Instant.now());
        }

        @Override
        public boolean isAvailable() {
            return available;
        }
    }
}
