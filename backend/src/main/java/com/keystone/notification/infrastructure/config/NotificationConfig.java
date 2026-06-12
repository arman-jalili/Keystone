package com.keystone.notification.infrastructure.config;

import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Spring configuration for the Notification Engine module.
 *
 * <p>Provides shared beans including the {@link RestTemplate} for external
 * API calls and a {@link Clock} for time-related operations.
 */
@Configuration
public class NotificationConfig {

    private static final Logger log = LoggerFactory.getLogger(NotificationConfig.class);

    @Bean
    public RestTemplate notificationRestTemplate() {
        return new RestTemplate();
    }

    @Bean
    public Clock notificationClock() {
        return Clock.systemUTC();
    }
}
