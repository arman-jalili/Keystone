package com.keystone.policy.evaluator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.keystone.policy.application.dto.EvaluateSpecRequest;
import com.keystone.policy.application.dto.EvaluateSpecResponse;
import com.keystone.policy.domain.event.PolicyEvaluatedEvent;
import com.keystone.policy.domain.model.*;
import com.keystone.policy.domain.service.EvaluationEngine;
import com.keystone.policy.infrastructure.event.PolicyEventPublisher;
import com.keystone.policy.infrastructure.repository.PolicyRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PolicyEvaluationServiceImplTest {

    @Mock
    private PolicyRepository policyRepository;

    @Mock
    private EvaluationEngine evaluationEngine;

    @Mock
    private PolicyEventPublisher eventPublisher;

    @Captor
    private ArgumentCaptor<PolicyEvaluatedEvent> eventCaptor;

    private PolicyEvaluationServiceImpl evaluationService;

    private final Instant now = Instant.now();

    @BeforeEach
    void setUp() {
        evaluationService = new PolicyEvaluationServiceImpl(policyRepository, evaluationEngine, eventPublisher);
    }

    @Test
    void evaluateSpec_shouldEvaluateSpecificPolicySet() {
        UUID policySetId = UUID.randomUUID();
        UUID specId = UUID.nameUUIDFromBytes("org/repo:openapi.yaml".getBytes());
        var request = new EvaluateSpecRequest("org/repo", "openapi.yaml", "a".repeat(40), policySetId);

        var policySet = new PolicySet(policySetId, "test-set", "Test", List.of(), 1, now, now);
        var evaluationResult = new PolicyEvaluationResult(
                UUID.randomUUID(),
                specId,
                policySetId,
                "org/repo",
                "openapi.yaml",
                "a".repeat(40),
                PolicyEvaluationResult.Verdict.PASS,
                List.of(),
                0,
                0,
                0,
                now);

        when(policyRepository.findPolicySetById(policySetId)).thenReturn(Optional.of(policySet));
        when(evaluationEngine.evaluate(policySet, specId)).thenReturn(evaluationResult);

        EvaluateSpecResponse response = evaluationService.evaluateSpec(request);

        assertThat(response).isNotNull();
        assertThat(response.verdict()).isEqualTo(PolicyEvaluationResult.Verdict.PASS);
        verify(eventPublisher).policyEvaluated(any(PolicyEvaluatedEvent.class));
    }

    @Test
    void evaluateSpec_shouldThrowWhenPolicySetNotFound() {
        UUID policySetId = UUID.randomUUID();
        var request = new EvaluateSpecRequest("org/repo", "openapi.yaml", "a".repeat(40), policySetId);

        when(policyRepository.findPolicySetById(policySetId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> evaluationService.evaluateSpec(request))
                .isInstanceOf(com.keystone.policy.domain.exception.PolicyNotFoundException.class);
    }

    @Test
    void evaluateSpec_shouldEvaluateAllPolicySetsWhenNoneSpecified() {
        var request = new EvaluateSpecRequest("org/repo", "openapi.yaml", "a".repeat(40), null);
        UUID specId = UUID.nameUUIDFromBytes("org/repo:openapi.yaml".getBytes());

        var policySet = new PolicySet(UUID.randomUUID(), "all-set", "All", List.of(), 1, now, now);
        var evaluationResult = new PolicyEvaluationResult(
                UUID.randomUUID(),
                specId,
                policySet.getId(),
                "org/repo",
                "openapi.yaml",
                "a".repeat(40),
                PolicyEvaluationResult.Verdict.PASS,
                List.of(),
                0,
                0,
                0,
                now);

        when(policyRepository.findAllPolicySets()).thenReturn(List.of(policySet));
        when(evaluationEngine.evaluate(policySet, specId)).thenReturn(evaluationResult);

        EvaluateSpecResponse response = evaluationService.evaluateSpec(request);

        assertThat(response).isNotNull();
        assertThat(response.verdict()).isEqualTo(PolicyEvaluationResult.Verdict.PASS);
    }

    @Test
    void evaluateSpec_shouldReturnPassWhenNoPolicySets() {
        var request = new EvaluateSpecRequest("org/repo", "openapi.yaml", "a".repeat(40), null);

        when(policyRepository.findAllPolicySets()).thenReturn(List.of());
        when(policyRepository.saveEvaluation(any())).thenAnswer(i -> i.getArgument(0));

        EvaluateSpecResponse response = evaluationService.evaluateSpec(request);

        assertThat(response.verdict()).isEqualTo(PolicyEvaluationResult.Verdict.PASS);
    }

    @Test
    void evaluateSpec_shouldPublishEventForEachEvaluation() {
        var request = new EvaluateSpecRequest("org/repo", "openapi.yaml", "a".repeat(40), null);
        UUID specId = UUID.nameUUIDFromBytes("org/repo:openapi.yaml".getBytes());

        var ps1 = new PolicySet(UUID.randomUUID(), "set-1", "Set 1", List.of(), 1, now, now);
        var ps2 = new PolicySet(UUID.randomUUID(), "set-2", "Set 2", List.of(), 1, now, now);
        var result = new PolicyEvaluationResult(
                UUID.randomUUID(),
                specId,
                ps1.getId(),
                "org/repo",
                "openapi.yaml",
                "a".repeat(40),
                PolicyEvaluationResult.Verdict.PASS,
                List.of(),
                0,
                0,
                0,
                now);

        when(policyRepository.findAllPolicySets()).thenReturn(List.of(ps1, ps2));
        when(evaluationEngine.evaluate(any(), eq(specId))).thenReturn(result);

        evaluationService.evaluateSpec(request);

        verify(eventPublisher, times(2)).policyEvaluated(any(PolicyEvaluatedEvent.class));
    }

    @Test
    void getEvaluationResult_shouldReturnResultWhenFound() {
        UUID evalId = UUID.randomUUID();
        var result = new PolicyEvaluationResult(
                evalId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "org/repo",
                "openapi.yaml",
                "a".repeat(40),
                PolicyEvaluationResult.Verdict.PASS,
                List.of(),
                0,
                0,
                0,
                now);

        when(policyRepository.findEvaluationById(evalId)).thenReturn(Optional.of(result));

        EvaluateSpecResponse response = evaluationService.getEvaluationResult(evalId);

        assertThat(response.id()).isEqualTo(evalId);
    }

    @Test
    void getEvaluationResult_shouldThrowWhenNotFound() {
        UUID evalId = UUID.randomUUID();
        when(policyRepository.findEvaluationById(evalId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> evaluationService.getEvaluationResult(evalId))
                .isInstanceOf(com.keystone.policy.domain.exception.PolicyNotFoundException.class);
    }
}
