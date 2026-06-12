package com.keystone.notification.domain.channel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.keystone.notification.domain.model.CiStatusPayload;
import com.keystone.notification.domain.model.Notification;
import com.keystone.notification.domain.model.NotificationStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class CiStatusChannelImplTest {

    private static final Instant NOW = Instant.parse("2026-06-12T12:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(NOW, ZoneId.of("UTC"));

    @Mock
    private RestTemplate restTemplate;

    private CiStatusChannelImpl channel;

    @BeforeEach
    void setUp() {
        channel = new CiStatusChannelImpl(restTemplate, "https://api.github.com", "test-token", 5, 30, FIXED_CLOCK);
    }

    @Test
    void getName_shouldReturnCiStatus() {
        assertThat(channel.getName()).isEqualTo("CI_STATUS");
    }

    @Test
    void getContext_shouldReturnDefaultContext() {
        assertThat(channel.getContext()).isEqualTo("keystone/governance");
    }

    @Test
    void isAvailable_shouldReturnTrueInitially() {
        assertThat(channel.isAvailable()).isTrue();
    }

    @Test
    void send_shouldDeliverForPolicyEvaluatedEvent() {
        PolicyEvalEvent event = new PolicyEvalEvent("org/repo", "a".repeat(40), "PASS", 0);
        when(restTemplate.postForEntity(anyString(), any(), eq(Void.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.CREATED));

        Notification notification = channel.send(event);

        assertThat(notification.status()).isEqualTo(NotificationStatus.DELIVERED);
        assertThat(notification.channelName()).isEqualTo("CI_STATUS");
        verify(restTemplate).postForEntity(anyString(), any(), eq(Void.class));
    }

    @Test
    void send_shouldFailForUnsupportedEvent() {
        Notification notification = channel.send("unsupported string event");

        assertThat(notification.status()).isEqualTo(NotificationStatus.FAILED);
        assertThat(notification.message()).contains("Unsupported event type");
        verify(restTemplate, never()).postForEntity(anyString(), any(), any());
    }

    @Test
    void postStatus_shouldReturnDeliveredOnSuccess() {
        CiStatusPayload payload = new CiStatusPayload(
                "success", "All checks passed", null, "keystone/governance", "my-org", "my-service", "a".repeat(40));
        when(restTemplate.postForEntity(anyString(), any(), eq(Void.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.CREATED));

        Notification notification = channel.postStatus(payload);

        assertThat(notification.status()).isEqualTo(NotificationStatus.DELIVERED);
        assertThat(notification.message()).contains("201");
    }

    @Test
    void postStatus_shouldReturnFailedOnApiError() {
        CiStatusPayload payload = new CiStatusPayload(
                "success", "All checks passed", null, "keystone/governance", "my-org", "my-service", "a".repeat(40));

        when(restTemplate.postForEntity(anyString(), any(), eq(Void.class)))
                .thenThrow(new RuntimeException("API rate limit exceeded"));

        Notification notification = channel.postStatus(payload);

        assertThat(notification.status()).isEqualTo(NotificationStatus.FAILED);
        assertThat(notification.message()).contains("API rate limit exceeded");
    }

    @Test
    void extractPayload_shouldHandlePolicyEvaluatedEvent() {
        PolicyEvalEvent event = new PolicyEvalEvent("my-org/my-service", "abcdef12345", "PASS", 0);

        CiStatusPayload payload = channel.extractPayload(event);

        assertThat(payload.state()).isEqualTo("success");
        assertThat(payload.owner()).isEqualTo("my-org");
        assertThat(payload.repo()).isEqualTo("my-service");
        assertThat(payload.sha()).isEqualTo("abcdef12345");
        assertThat(payload.description()).isEqualTo("All policy checks passed");
    }

    @Test
    void extractPayload_shouldHandleFailVerdict() {
        PolicyEvalEvent event = new PolicyEvalEvent("org/repo", "sha", "FAIL", 3);

        CiStatusPayload payload = channel.extractPayload(event);

        assertThat(payload.state()).isEqualTo("failure");
        assertThat(payload.description()).contains("3 policy violation(s)");
    }

    @Test
    void extractPayload_shouldThrowForUnsupportedEvent() {
        // Non-JSON strings throw via JSON parsing
        assertThatThrownBy(() -> channel.extractPayload("string event")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void extractPayload_shouldThrowForCustomObjectWithoutMethods() {
        assertThatThrownBy(() -> channel.extractPayload(42))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported event type");
    }

    @Test
    void isAvailable_shouldReturnFalseWhenCircuitBreakerIsOpen() {
        when(restTemplate.postForEntity(anyString(), any(), eq(Void.class)))
                .thenThrow(new RuntimeException("Service unavailable"));

        for (int i = 0; i < 6; i++) {
            channel.postStatus(new CiStatusPayload("error", "fail", null, "ctx", "o", "r", "s"));
        }

        assertThat(channel.isAvailable()).isFalse();
    }

    @Test
    void send_shouldReturnFailedWhenCircuitBreakerIsOpen() {
        when(restTemplate.postForEntity(anyString(), any(), eq(Void.class)))
                .thenThrow(new RuntimeException("Service unavailable"));

        for (int i = 0; i < 6; i++) {
            channel.postStatus(new CiStatusPayload("error", "fail", null, "ctx", "o", "r", "s"));
        }

        assertThat(channel.isAvailable()).isFalse();

        Notification notification = channel.send(new Object());

        assertThat(notification.status()).isEqualTo(NotificationStatus.FAILED);
        assertThat(notification.message()).contains("Circuit breaker open");
    }

    @Test
    void extractPayload_shouldHandleJsonString() {
        String json = "{\"state\":\"success\",\"description\":\"Build passed\",\"context\":\"ci/test\","
                + "\"owner\":\"org\",\"repo\":\"repo\",\"sha\":\"abc123\"}";

        CiStatusPayload payload = channel.extractPayload(json);

        assertThat(payload.state()).isEqualTo("success");
        assertThat(payload.owner()).isEqualTo("org");
        assertThat(payload.repo()).isEqualTo("repo");
        assertThat(payload.sha()).isEqualTo("abc123");
    }

    @Test
    void extractPayload_shouldUseDefaultsForMissingJsonFields() {
        String json = "{\"state\":\"pending\",\"sha\":\"abc123\"}";

        CiStatusPayload payload = channel.extractPayload(json);

        assertThat(payload.state()).isEqualTo("pending");
        assertThat(payload.context()).isEqualTo("keystone/governance");
        assertThat(payload.owner()).isEqualTo("unknown");
        assertThat(payload.repo()).isEqualTo("unknown");
    }

    // ---- Helpers ----

    /**
     * Test DTO that mimics PolicyEvaluatedEvent for reflection-based extraction.
     */
    public static class PolicyEvalEvent {
        private final String repository;
        private final String commitSha;
        private final String verdict;
        private final int violationCount;

        public PolicyEvalEvent(String repository, String commitSha, String verdict, int violationCount) {
            this.repository = repository;
            this.commitSha = commitSha;
            this.verdict = verdict;
            this.violationCount = violationCount;
        }

        public String repository() {
            return repository;
        }

        public String commitSha() {
            return commitSha;
        }

        public String verdict() {
            return verdict;
        }

        public int violationCount() {
            return violationCount;
        }
    }
}
