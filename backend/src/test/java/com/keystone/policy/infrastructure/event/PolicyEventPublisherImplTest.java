// Canonical Reference: .pi/architecture/modules/policy-engine.md
package com.keystone.policy.infrastructure.event;

import static org.mockito.Mockito.verify;

import com.keystone.policy.domain.event.PolicyEvaluatedEvent;
import com.keystone.policy.domain.event.PolicySourceChangedEvent;
import com.keystone.policy.domain.event.PolicySyncedEvent;
import com.keystone.policy.domain.model.PolicyEvaluationResult;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class PolicyEventPublisherImplTest {

    @Mock
    private ApplicationEventPublisher springPublisher;

    private PolicyEventPublisherImpl publisher;

    @BeforeEach
    void setUp() {
        publisher = new PolicyEventPublisherImpl(springPublisher);
    }

    @Test
    void policyEvaluated_shouldDelegateToSpringPublisher() {
        var event = new PolicyEvaluatedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "org/repo",
                "openapi.yaml",
                "a".repeat(40),
                PolicyEvaluationResult.Verdict.PASS,
                0,
                Instant.now());

        publisher.policyEvaluated(event);

        verify(springPublisher).publishEvent(event);
    }

    @Test
    void policySynced_shouldDelegateToSpringPublisher() {
        var event = new PolicySyncedEvent(
                UUID.randomUUID(), "source-1", UUID.randomUUID(), "test-policy-set", 2, 3, 1, 0, Instant.now());

        publisher.policySynced(event);

        verify(springPublisher).publishEvent(event);
    }

    @Test
    void policySourceChanged_shouldDelegateToSpringPublisher() {
        var event = new PolicySourceChangedEvent(
                UUID.randomUUID(), "source-1", "git", PolicySourceChangedEvent.ChangeType.UPDATED, Instant.now());

        publisher.policySourceChanged(event);

        verify(springPublisher).publishEvent(event);
    }
}
