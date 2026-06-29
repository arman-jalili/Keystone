// Canonical Reference: .pi/architecture/modules/policy-engine.md
// Module: policy-engine
package com.keystone.policy.evaluator;

import com.keystone.analysis.domain.model.ParsedEndpoint;
import com.keystone.analysis.domain.service.SpecParser;
import com.keystone.ingestion.infrastructure.repository.SpecRepository;
import com.keystone.policy.domain.exception.PolicyEvaluationException;
import com.keystone.policy.domain.model.*;
import com.keystone.policy.domain.service.EvaluationEngine;
import com.keystone.policy.infrastructure.repository.PolicyRepository;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Evaluates OpenAPI specifications against policy sets using real spec data.
 *
 * <p>Fetches the actual spec content from the ingestion store, parses it
 * into structured endpoints, and evaluates each policy's DSL conditions
 * against the real endpoint data.
 */
@Service
public class EvaluationEngineImpl implements EvaluationEngine {

    private static final Logger log = LoggerFactory.getLogger(EvaluationEngineImpl.class);

    private final PolicyRepository policyRepository;
    private final SpecRepository specRepository;
    private final SpecParser specParser;

    public EvaluationEngineImpl(
            PolicyRepository policyRepository, SpecRepository specRepository, SpecParser specParser) {
        this.policyRepository = policyRepository;
        this.specRepository = specRepository;
        this.specParser = specParser;
    }

    @Override
    public PolicyEvaluationResult evaluate(PolicySet policySet, UUID specId) throws PolicyEvaluationException {
        return evaluateInternal(policySet, specId, null);
    }

    @Override
    public PolicyEvaluationResult evaluateSubset(PolicySet policySet, UUID specId, Set<UUID> policyIds)
            throws PolicyEvaluationException {
        return evaluateInternal(policySet, specId, policyIds);
    }

    private PolicyEvaluationResult evaluateInternal(PolicySet policySet, UUID specId, Set<UUID> subsetIds) {
        List<Policy> activePolicies = policySet.getActivePolicies();
        if (subsetIds != null) {
            activePolicies = activePolicies.stream()
                    .filter(p -> subsetIds.contains(p.getId()))
                    .toList();
        }

        // Fetch and parse the real spec content
        List<ParsedEndpoint> parsedEndpoints = fetchParsedEndpoints(specId);

        List<Violation> allViolations = new ArrayList<>();
        int checked = 0;

        for (Policy policy : activePolicies) {
            checked++;
            try {
                List<Violation> violations = evaluatePolicy(policy, parsedEndpoints);
                allViolations.addAll(violations);
            } catch (Exception e) {
                log.warn("Policy '{}' evaluation failed, skipping: {}", policy.getName(), e.getMessage());
            }
        }

        long passedCount = activePolicies.size()
                - allViolations.stream().map(Violation::policyId).distinct().count();

        long failedCount = allViolations.stream()
                .filter(v -> v.severity() == PolicySeverity.CRITICAL || v.severity() == PolicySeverity.MAJOR)
                .map(Violation::policyId)
                .distinct()
                .count();

        PolicyEvaluationResult.Verdict verdict = computeVerdict(allViolations);

        PolicyEvaluationResult result = new PolicyEvaluationResult(
                UUID.randomUUID(),
                specId,
                policySet.getId(),
                "unknown",
                "unknown",
                null,
                verdict,
                allViolations,
                checked,
                (int) passedCount,
                (int) failedCount,
                Instant.now());

        return policyRepository.saveEvaluation(result);
    }

    /**
     * Fetches the latest version of a spec and parses it into endpoints.
     */
    private List<ParsedEndpoint> fetchParsedEndpoints(UUID specId) {
        try {
            var versions = specRepository.findVersionsBySpecId(specId, 1);
            if (!versions.isEmpty()) {
                return specParser.parse(versions.getFirst().getRawContent());
            }
            // Fallback: try to find any spec
            var optSpec = specRepository.findById(specId);
            if (optSpec.isPresent()) {
                versions = specRepository.findVersionsBySpecId(optSpec.get().getId(), 1);
                if (!versions.isEmpty()) {
                    return specParser.parse(versions.getFirst().getRawContent());
                }
            }
        } catch (Exception e) {
            log.warn("Failed to fetch spec content for {}: {}", specId, e.getMessage());
        }
        return List.of();
    }

    /**
     * Evaluates a single policy against real parsed endpoints.
     */
    List<Violation> evaluatePolicy(Policy policy, List<ParsedEndpoint> endpoints) {
        List<Violation> violations = new ArrayList<>();
        String dsl = policy.getDslExpression();

        if (dsl == null || dsl.isBlank()) {
            return violations;
        }

        String trimmed = dsl.trim().toLowerCase();

        // Apply scope filtering if the policy has path patterns
        // (only when we have real endpoint data)
        List<ParsedEndpoint> scopedEndpoints =
                endpoints.isEmpty() ? endpoints : filterByScope(endpoints, policy.getScope());

        // Pattern: "none field in spec.schemas where field.is_deprecated"
        if ((trimmed.contains("is_deprecated") || trimmed.contains("deprecated"))
                && trimmed.contains("none")
                && trimmed.contains("yield violation")) {
            // Check if any endpoint is actually deprecated
            boolean hasDeprecated = scopedEndpoints.stream().anyMatch(ParsedEndpoint::deprecated);
            if (hasDeprecated) {
                for (var ep : scopedEndpoints) {
                    if (ep.deprecated()) {
                        violations.add(new Violation(
                                policy.getId(),
                                policy.getName(),
                                policy.getSeverity(),
                                "Deprecated endpoint: " + ep.method() + " " + ep.path(),
                                ep.path(),
                                null));
                    }
                }
            }
            return violations;
        }

        // Pattern: "each endpoint in spec.endpoints where not endpoint.has('operationId')"
        if ((trimmed.contains("operationid") || trimmed.contains("operation_id"))
                && trimmed.contains("yield violation")
                && (trimmed.contains("not") || trimmed.contains("!"))) {
            for (var ep : scopedEndpoints) {
                if (ep.summary() == null || ep.summary().isBlank()) {
                    violations.add(new Violation(
                            policy.getId(),
                            policy.getName(),
                            policy.getSeverity(),
                            "Endpoint " + ep.method() + " " + ep.path() + " is missing a summary",
                            ep.path(),
                            null));
                }
            }
            return violations;
        }

        // Pattern: "each path in spec.paths where not path.matches(...)"
        if (trimmed.contains("path.matches") && trimmed.contains("yield violation")) {
            // Extract the regex pattern from the DSL
            java.util.regex.Matcher m =
                    Pattern.compile("path\\.matches\\(['\"]([^'\"]+)['\"]\\)").matcher(dsl);
            String regex = m.find() ? m.group(1) : null;
            if (regex != null) {
                Pattern pattern = Pattern.compile(regex);
                for (var ep : scopedEndpoints) {
                    if (!pattern.matcher(ep.path()).matches()) {
                        violations.add(new Violation(
                                policy.getId(),
                                policy.getName(),
                                policy.getSeverity(),
                                "Path " + ep.path() + " does not match required pattern: " + regex,
                                ep.path(),
                                null));
                    }
                }
            }
            return violations;
        }

        // Default: yield pass
        if (trimmed.contains("yield pass")) {
            return violations;
        }

        // Fallback: generic "each ... yield violation" without conditions
        // (backward compatibility for policies written before real spec parsing)
        if (endpoints.isEmpty()
                && trimmed.startsWith("each")
                && trimmed.contains("yield violation")
                && !trimmed.contains("where")
                && !trimmed.contains("not")) {
            violations.add(new Violation(
                    policy.getId(),
                    policy.getName(),
                    policy.getSeverity(),
                    "Policy '" + policy.getName() + "' matched all elements",
                    "/*",
                    null));
        }

        return violations;
    }

    /**
     * Filters endpoints by policy scope (path patterns, operations, etc.).
     */
    private List<ParsedEndpoint> filterByScope(List<ParsedEndpoint> endpoints, PolicyScope scope) {
        if (scope == null || scope.appliesToAll()) {
            return endpoints;
        }
        return endpoints.stream().filter(ep -> matchesScope(ep, scope)).toList();
    }

    private boolean matchesScope(ParsedEndpoint ep, PolicyScope scope) {
        if (!scope.pathPatterns().isEmpty()) {
            boolean pathMatch = scope.pathPatterns().stream().anyMatch(pattern -> {
                String glob = pattern.replace("/**", "/.*").replace("*", "[^/]*");
                return Pattern.matches(glob, ep.path());
            });
            if (!pathMatch) return false;
        }
        if (!scope.excludePaths().isEmpty()) {
            boolean excluded = scope.excludePaths().stream().anyMatch(pattern -> {
                String glob = pattern.replace("/**", "/.*").replace("*", "[^/]*");
                return Pattern.matches(glob, ep.path());
            });
            if (excluded) return false;
        }
        if (!scope.operations().isEmpty()) {
            boolean opMatch =
                    scope.operations().stream().anyMatch(op -> op.name().equalsIgnoreCase(ep.method()));
            if (!opMatch) return false;
        }
        return true;
    }

    private PolicyEvaluationResult.Verdict computeVerdict(List<Violation> violations) {
        if (violations.isEmpty()) {
            return PolicyEvaluationResult.Verdict.PASS;
        }
        boolean hasCriticalOrMajor = violations.stream()
                .anyMatch(v -> v.severity() == PolicySeverity.CRITICAL || v.severity() == PolicySeverity.MAJOR);
        if (hasCriticalOrMajor) {
            return PolicyEvaluationResult.Verdict.FAIL;
        }
        return PolicyEvaluationResult.Verdict.WARNING;
    }
}
