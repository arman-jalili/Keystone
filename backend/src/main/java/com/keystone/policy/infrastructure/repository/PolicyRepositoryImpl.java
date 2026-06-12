package com.keystone.policy.infrastructure.repository;

import com.keystone.policy.domain.model.*;
import com.keystone.policy.infrastructure.repository.jpa.EvaluationResultEntity;
import com.keystone.policy.infrastructure.repository.jpa.PolicyEntity;
import com.keystone.policy.infrastructure.repository.jpa.PolicySetEntity;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Adapts Spring Data JPA to the domain {@link PolicyRepository} interface.
 */
@Repository
@Transactional
public class PolicyRepositoryImpl implements PolicyRepository {

    private final SpringDataPolicyRepository policyRepo;
    private final SpringDataPolicySetRepository policySetRepo;
    private final SpringDataEvaluationResultRepository evalRepo;

    public PolicyRepositoryImpl(SpringDataPolicyRepository policyRepo,
                                SpringDataPolicySetRepository policySetRepo,
                                SpringDataEvaluationResultRepository evalRepo) {
        this.policyRepo = policyRepo;
        this.policySetRepo = policySetRepo;
        this.evalRepo = evalRepo;
    }

    // ---- Policy operations ----

    @Override
    @Transactional(readOnly = true)
    public Optional<Policy> findPolicyById(UUID policyId) {
        return policyRepo.findById(policyId).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Policy> findPolicyByNameAndSource(String name, String sourceId) {
        return policyRepo.findByNameAndSourceId(name, sourceId).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Policy> findAllPolicies(PolicyStatus status) {
        if (status == null) {
            return policyRepo.findAll().stream().map(this::toDomain).toList();
        }
        return policyRepo.findByStatus(status.name()).stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Policy> findPoliciesBySource(String sourceId) {
        return policyRepo.findBySourceId(sourceId).stream().map(this::toDomain).toList();
    }

    @Override
    public Policy savePolicy(Policy policy) {
        var entity = toEntity(policy);
        var saved = policyRepo.save(entity);
        return toDomain(saved);
    }

    @Override
    public Policy updatePolicy(Policy policy) {
        return policyRepo.findById(policy.getId())
                .map(entity -> {
                    entity.setStatus(policy.getStatus().name());
                    entity.setVersion(policy.getVersion());
                    entity.setDslExpression(policy.getDslExpression());
                    entity.setUpdatedAt(policy.getUpdatedAt());
                    return toDomain(policyRepo.save(entity));
                })
                .orElseThrow(() -> new RuntimeException("Policy not found: " + policy.getId()));
    }

    @Override
    public void deletePolicy(UUID policyId) {
        policyRepo.deleteById(policyId);
    }

    @Override
    public int deletePoliciesBySource(String sourceId) {
        var policies = policyRepo.findBySourceId(sourceId);
        policyRepo.deleteAll(policies);
        return policies.size();
    }

    // ---- PolicySet operations ----

    @Override
    @Transactional(readOnly = true)
    public Optional<PolicySet> findPolicySetById(UUID policySetId) {
        return policySetRepo.findById(policySetId).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PolicySet> findPolicySetByName(String name) {
        return policySetRepo.findByName(name).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PolicySet> findAllPolicySets() {
        return policySetRepo.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public PolicySet savePolicySet(PolicySet policySet) {
        var entity = new PolicySetEntity(
                policySet.getId(), policySet.getName(), policySet.getDescription(),
                policySet.getVersion(), policySet.getCreatedAt(), policySet.getUpdatedAt());
        // Save associated policies
        for (Policy policy : policySet.getPolicies()) {
            savePolicy(policy);
        }
        var saved = policySetRepo.save(entity);
        return toDomain(saved);
    }

    @Override
    public PolicySet updatePolicySet(PolicySet policySet) {
        return policySetRepo.findById(policySet.getId())
                .map(entity -> {
                    entity.setVersion(policySet.getVersion());
                    entity.setUpdatedAt(policySet.getUpdatedAt());
                    return toDomain(policySetRepo.save(entity));
                })
                .orElseThrow(() -> new RuntimeException("PolicySet not found: " + policySet.getId()));
    }

    @Override
    public void deletePolicySet(UUID policySetId) {
        policySetRepo.deleteById(policySetId);
    }

    // ---- Evaluation result operations ----

    @Override
    @Transactional(readOnly = true)
    public Optional<PolicyEvaluationResult> findEvaluationById(UUID evaluationId) {
        return evalRepo.findById(evaluationId).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PolicyEvaluationResult> findEvaluationsBySpecId(UUID specId, int limit) {
        return evalRepo.findBySpecIdOrderByEvaluatedAtDesc(
                        specId, org.springframework.data.domain.PageRequest.of(0, limit))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public PolicyEvaluationResult saveEvaluation(PolicyEvaluationResult result) {
        var entity = new EvaluationResultEntity(
                result.getId(), result.getSpecId(), result.getPolicySetId(),
                result.getRepository(), result.getSpecPath(), result.getCommitSha(),
                result.getVerdict().name(),
                serializeViolations(result.getViolations()),
                result.getTotalPoliciesChecked(), result.getPassedCount(),
                result.getFailedCount(), result.getEvaluatedAt());
        var saved = evalRepo.save(entity);
        return toDomain(saved);
    }

    // ---- Deactivate stale ----

    @Override
    public int deactivateStalePolicies(List<String> activePolicyNames) {
        return policyRepo.deactivateStalePolicies(activePolicyNames);
    }

    // ---- Mapping helpers ----

    private Policy toDomain(PolicyEntity e) {
        return new Policy(
                e.getId(), e.getName(), e.getDescription(),
                PolicySeverity.valueOf(e.getSeverity()),
                PolicyStatus.valueOf(e.getStatus()),
                deserializeScope(e),
                e.getDslExpression(), e.getSourceId(), e.getVersion(),
                e.getCreatedAt(), e.getUpdatedAt());
    }

    private PolicyEntity toEntity(Policy p) {
        return new PolicyEntity(
                p.getId(), p.getName(), p.getDescription(),
                p.getSeverity().name(), p.getStatus().name(),
                p.getDslExpression(), p.getSourceId(), p.getVersion(),
                serializeScopePaths(p.getScope()),
                serializeOperations(p.getScope()),
                serializeTags(p.getScope()),
                serializeExcludePaths(p.getScope()),
                p.getCreatedAt(), p.getUpdatedAt());
    }

    private PolicySet toDomain(PolicySetEntity e) {
        // Load policies belonging to this set (associated via sourceId matching set name)
        List<Policy> policies = policyRepo.findBySourceId(e.getName())
                .stream().map(this::toDomain).toList();
        return new PolicySet(e.getId(), e.getName(), e.getDescription(),
                policies, e.getVersion(), e.getCreatedAt(), e.getUpdatedAt());
    }

    private PolicyEvaluationResult toDomain(EvaluationResultEntity e) {
        List<Violation> violations = deserializeViolations(e.getViolationsJson());
        // Count passing/failing from violations
        long failed = violations.stream()
                .filter(v -> v.severity() == PolicySeverity.CRITICAL
                        || v.severity() == PolicySeverity.MAJOR)
                .count();
        return new PolicyEvaluationResult(
                e.getId(), e.getSpecId(), e.getPolicySetId(),
                e.getRepository(), e.getSpecPath(), e.getCommitSha(),
                PolicyEvaluationResult.Verdict.valueOf(e.getVerdict()),
                violations, e.getTotalPoliciesChecked(),
                e.getPassedCount(), e.getFailedCount(), e.getEvaluatedAt());
    }

    private PolicyScope deserializeScope(PolicyEntity e) {
        return new PolicyScope(
                parseSet(e.getScopePathPatterns()),
                parseOperations(e.getScopeOperations()),
                parseSet(e.getScopeTags()),
                parseSet(e.getScopeExcludePaths()));
    }

    private String serializeScopePaths(PolicyScope scope) {
        return scope.pathPatterns() == null ? null : String.join(",", scope.pathPatterns());
    }

    private String serializeOperations(PolicyScope scope) {
        if (scope.operations() == null || scope.operations().isEmpty()) return null;
        return scope.operations().stream().map(Enum::name).collect(Collectors.joining(","));
    }

    private String serializeTags(PolicyScope scope) {
        return scope.tags() == null ? null : String.join(",", scope.tags());
    }

    private String serializeExcludePaths(PolicyScope scope) {
        return scope.excludePaths() == null ? null : String.join(",", scope.excludePaths());
    }

    private Set<PolicyScope.HttpOperation> parseOperations(String value) {
        if (value == null || value.isBlank()) return Set.of();
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .map(PolicyScope.HttpOperation::valueOf)
                .collect(Collectors.toSet());
    }

    private Set<String> parseSet(String value) {
        if (value == null || value.isBlank()) return Set.of();
        return Set.of(value.split(","));
    }

    private String serializeViolations(List<Violation> violations) {
        if (violations == null || violations.isEmpty()) return "[]";
        return violations.stream()
                .map(v -> String.format(
                        "{\"policyId\":\"%s\",\"policyName\":\"%s\",\"severity\":\"%s\",\"message\":\"%s\",\"specPath\":\"%s\"}",
                        v.policyId(), escapeJson(v.policyName()), v.severity().name(),
                        escapeJson(v.message()), escapeJson(v.specPath())))
                .collect(Collectors.joining(",", "[", "]"));
    }

    private List<Violation> deserializeViolations(String json) {
        if (json == null || json.isBlank() || json.equals("[]")) return List.of();
        // Simple JSON array parsing for violation records
        return Arrays.stream(json.split("\\},\\{"))
                .map(this::parseViolation)
                .toList();
    }

    private Violation parseViolation(String jsonFragment) {
        String clean = jsonFragment.replaceAll("[\\[\\]{}]", "");
        String[] parts = clean.split(",");
        UUID policyId = null;
        String policyName = "", severity = "", message = "", specPath = "";
        for (String part : parts) {
            String[] kv = part.split(":", 2);
            if (kv.length < 2) continue;
            String key = kv[0].replace("\"", "").trim();
            String val = kv[1].replace("\"", "").trim();
            switch (key) {
                case "policyId" -> policyId = UUID.fromString(val);
                case "policyName" -> policyName = val;
                case "severity" -> severity = val;
                case "message" -> message = val;
                case "specPath" -> specPath = val;
            }
        }
        return new Violation(policyId, policyName, PolicySeverity.valueOf(severity),
                message, specPath, null);
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
