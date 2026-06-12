package com.keystone.notification.domain.channel;

import com.keystone.notification.domain.exception.CircuitBreakerOpenException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.keystone.notification.domain.model.CiStatusPayload;
import com.keystone.notification.domain.model.Notification;
import com.keystone.notification.domain.model.NotificationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Implementation of {@link CiStatusChannel} that posts commit status updates
 * to GitHub and GitLab commit status APIs.
 *
 * <p>Features:
 * <ul>
 *   <li>REST API calls via {@link RestTemplate}</li>
 *   <li>Configurable timeout via RestTemplate configuration</li>
 *   <li>Simple circuit breaker pattern (configurable threshold and cooldown)</li>
 *   <li>Event-to-payload mapping for supported domain events</li>
 * </ul>
 *
 * <p>Circuit breaker thresholds are configurable via application properties:
 * <ul>
 *   <li>{@code notification.ci-status.circuit-breaker.threshold} — failure count to open (default: 5)</li>
 *   <li>{@code notification.ci-status.circuit-breaker.cooldown} — cooldown duration in seconds (default: 30)</li>
 * </ul>
 */
@Component
public class CiStatusChannelImpl implements CiStatusChannel {

    private static final Logger log = LoggerFactory.getLogger(CiStatusChannelImpl.class);

    static final String DEFAULT_CONTEXT = "keystone/governance";
    static final int DEFAULT_CIRCUIT_BREAKER_THRESHOLD = 5;
    static final long DEFAULT_CIRCUIT_BREAKER_COOLDOWN_SECONDS = 30;

    private final RestTemplate restTemplate;
    private final String context;
    private final String githubApiBaseUrl;
    private final String githubToken;
    private final Clock clock;
    private final CircuitBreakerState circuitBreaker;

    public CiStatusChannelImpl(RestTemplate restTemplate,
                                @Value("${github.api.base-url:https://api.github.com}") String githubApiBaseUrl,
                                @Value("${github.token:}") String githubToken,
                                @Value("${notification.ci-status.circuit-breaker.threshold:5}") int cbThreshold,
                                @Value("${notification.ci-status.circuit-breaker.cooldown:30}") long cbCooldownSeconds,
                                Clock clock) {
        this.restTemplate = restTemplate;
        this.githubApiBaseUrl = githubApiBaseUrl;
        this.githubToken = githubToken;
        this.context = DEFAULT_CONTEXT;
        this.clock = clock;
        this.circuitBreaker = new CircuitBreakerState(cbThreshold, Duration.ofSeconds(cbCooldownSeconds));
    }

    @Override
    public String getName() {
        return "CI_STATUS";
    }

    @Override
    public String getContext() {
        return context;
    }

    @Override
    public boolean isAvailable() {
        return !circuitBreaker.isOpen();
    }

    @Override
    public Notification send(Object event) {
        log.debug("CiStatusChannel processing event: {}", event.getClass().getSimpleName());

        if (circuitBreaker.isOpen()) {
            log.warn("Circuit breaker is open for CI_STATUS channel");
            return buildNotification(NotificationStatus.FAILED,
                    "Circuit breaker open for channel: CI_STATUS", event.getClass().getSimpleName());
        }

        try {
            CiStatusPayload payload = extractPayload(event);
            return postStatus(payload);
        } catch (IllegalArgumentException e) {
            log.warn("Unsupported event type: {}", event.getClass().getSimpleName());
            return buildNotification(NotificationStatus.FAILED,
                    "Unsupported event type: " + event.getClass().getSimpleName(),
                    event.getClass().getSimpleName());
        }
    }

    @Override
    public Notification postStatus(CiStatusPayload payload) {
        log.info("Posting CI status: {} {} {} {}", payload.context(), payload.state(),
                payload.owner(), payload.repo());

        try {
            HttpHeaders headers = buildHeaders();
            Map<String, Object> body = Map.of(
                    "state", payload.state(),
                    "description", payload.description(),
                    "target_url", payload.targetUrl() != null ? payload.targetUrl() : "",
                    "context", payload.context()
            );

            String url = githubApiBaseUrl + "/repos/" + payload.owner() + "/" + payload.repo()
                    + "/statuses/" + payload.sha();

            ResponseEntity<Void> response = restTemplate.postForEntity(
                    url, new HttpEntity<>(body, headers), Void.class);

            circuitBreaker.recordSuccess();
            log.debug("CI status posted successfully to {}/{}: HTTP {}", payload.owner(), payload.repo(),
                    response.getStatusCode());

            return buildNotification(NotificationStatus.DELIVERED,
                    "Status: " + response.getStatusCode(), payload.context());

        } catch (Exception e) {
            circuitBreaker.recordFailure();
            log.error("Failed to post CI status to {}/{}: {}", payload.owner(), payload.repo(), e.getMessage());
            return buildNotification(NotificationStatus.FAILED,
                    "Delivery failed: " + e.getMessage(), payload.context());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public CiStatusPayload extractPayload(Object event) {
        if (event instanceof String json) {
            return extractFromJsonString(json);
        }

        // Use reflection to detect PolicyEvaluatedEvent-like objects
        // by checking for the expected method signatures
        if (hasMethod(event, "repository") && hasMethod(event, "commitSha")
                && hasMethod(event, "verdict") && hasMethod(event, "violationCount")) {
            return extractFromPolicyEvaluated(event);
        }

        throw new IllegalArgumentException("Unsupported event type: " + event.getClass().getSimpleName());
    }

    static boolean hasMethod(Object obj, String methodName) {
        try {
            obj.getClass().getMethod(methodName);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    // ---- Private helpers ----

    private CiStatusPayload extractFromPolicyEvaluated(Object event) {
        try {
            // Use reflection to extract fields from PolicyEvaluatedEvent
            var repoMethod = event.getClass().getMethod("repository");
            var shaMethod = event.getClass().getMethod("commitSha");
            var verdictMethod = event.getClass().getMethod("verdict");
            var violationCountMethod = event.getClass().getMethod("violationCount");

            String repository = (String) repoMethod.invoke(event);
            String sha = (String) shaMethod.invoke(event);
            Object verdict = verdictMethod.invoke(event);
            int violationCount = (int) violationCountMethod.invoke(event);

            String[] parts = repository.split("/", 2);
            String owner = parts.length > 1 ? parts[0] : "unknown";
            String repo = parts.length > 1 ? parts[1] : repository;

            String state = mapVerdictToState(verdict.toString(), violationCount);
            String description = buildDescription(verdict.toString(), violationCount);

            return new CiStatusPayload(state, description, null, context, owner, repo, sha);

        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to extract payload from PolicyEvaluatedEvent: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private CiStatusPayload extractFromJsonString(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> map = mapper.readValue(json, Map.class);

            String state = (String) map.getOrDefault("state", "pending");
            String description = (String) map.getOrDefault("description", "");
            String targetUrl = (String) map.get("target_url");
            String ctx = (String) map.getOrDefault("context", context);
            String owner = (String) map.getOrDefault("owner", "unknown");
            String repo = (String) map.getOrDefault("repo", "unknown");
            String sha = (String) map.getOrDefault("sha", "");

            return new CiStatusPayload(state, description, targetUrl, ctx, owner, repo, sha);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse CI status JSON: " + e.getMessage(), e);
        }
    }

    private String mapVerdictToState(String verdict, int violationCount) {
        return switch (verdict.toUpperCase()) {
            case "PASS" -> "success";
            case "FAIL" -> "failure";
            case "WARNING" -> violationCount > 0 ? "failure" : "success";
            default -> "error";
        };
    }

    private String buildDescription(String verdict, int violationCount) {
        return switch (verdict.toUpperCase()) {
            case "PASS" -> "All policy checks passed";
            case "FAIL" -> violationCount + " policy violation(s) detected";
            case "WARNING" -> violationCount + " warning(s) detected";
            default -> "Policy evaluation completed with verdict: " + verdict;
        };
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
        if (githubToken != null && !githubToken.isBlank()) {
            headers.setBearerAuth(githubToken);
        }
        return headers;
    }

    private Notification buildNotification(NotificationStatus status, String message, String payloadType) {
        return new Notification(
                UUID.randomUUID(),
                getName(),
                null,
                status,
                message,
                payloadType,
                clock.instant());
    }

    /**
     * Simple circuit breaker state machine.
     *
     * <p>States: CLOSED → OPEN → HALF_OPEN → CLOSED (on success) or OPEN (on failure)
     *
     * <p>To be replaced by Resilience4j CircuitBreaker in production.
     */
    static class CircuitBreakerState {
        private enum State { CLOSED, OPEN, HALF_OPEN }

        private final int failureThreshold;
        private final Duration cooldown;
        private final AtomicReference<State> state;
        private final AtomicInteger failureCount;
        private volatile Instant openedAt;

        CircuitBreakerState(int failureThreshold, Duration cooldown) {
            this.failureThreshold = failureThreshold;
            this.cooldown = cooldown;
            this.state = new AtomicReference<>(State.CLOSED);
            this.failureCount = new AtomicInteger(0);
        }

        boolean isOpen() {
            State current = state.get();
            if (current == State.OPEN) {
                if (Duration.between(openedAt, Instant.now()).compareTo(cooldown) >= 0) {
                    if (state.compareAndSet(State.OPEN, State.HALF_OPEN)) {
                        log.info("Circuit breaker transitioning HALF_OPEN for CI_STATUS");
                    }
                    return false; // Allow probe request
                }
                return true;
            }
            return false;
        }

        void recordSuccess() {
            state.set(State.CLOSED);
            failureCount.set(0);
            openedAt = null;
        }

        void recordFailure() {
            int failures = failureCount.incrementAndGet();
            if (failures >= failureThreshold && state.compareAndSet(State.CLOSED, State.OPEN)) {
                openedAt = Instant.now();
                log.warn("Circuit breaker OPEN for CI_STATUS after {} failures", failures);
            }
        }
    }
}
