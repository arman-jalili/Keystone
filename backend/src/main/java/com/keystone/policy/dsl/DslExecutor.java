package com.keystone.policy.dsl;

import com.keystone.policy.domain.model.Policy;
import com.keystone.policy.domain.model.PolicyScope;
import com.keystone.policy.domain.model.PolicySeverity;
import com.keystone.policy.domain.model.Violation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Executes parsed DSL expressions against spec data to produce violations.
 *
 * <p>For the initial implementation, the executor simulates spec data
 * based on known patterns. A full OpenAPI spec parser integration
 * will be added in a future iteration.
 */
public class DslExecutor {

    private static final Logger log = LoggerFactory.getLogger(DslExecutor.class);

    private final DslParser parser;

    public DslExecutor(DslParser parser) {
        this.parser = parser;
    }

    /**
     * Evaluates a policy against spec characteristics and returns violations.
     *
     * @param policy the policy to evaluate
     * @return list of violations (empty if policy passes)
     */
    public List<Violation> evaluate(Policy policy) {
        List<Violation> violations = new ArrayList<>();

        try {
            DslExpression expression = parser.parse(policy.getDslExpression());
            violations.addAll(execute(expression, policy));
        } catch (Exception e) {
            log.warn("Failed to parse/evaluate policy '{}': {}",
                    policy.getName(), e.getMessage());
        }

        return violations;
    }

    /**
     * Executes a parsed DSL expression and returns violations.
     */
    List<Violation> execute(DslExpression expression, Policy policy) {
        List<Violation> violations = new ArrayList<>();

        // Determine the source data to iterate over
        SpecData specData = getSpecData(expression.source());

        // Apply scope filtering
        List<SpecElement> matchingElements = filterByScope(specData.elements, policy.getScope());

        // Evaluate based on quantifier
        switch (expression.quantifier()) {
            case EACH -> {
                for (SpecElement element : matchingElements) {
                    boolean matches = evaluateCondition(expression.condition(), element);
                    if (matches) {
                        if (expression.action() == DslExpression.Action.VIOLATION) {
                            violations.add(createViolation(policy, element, expression.actionArg()));
                        }
                    }
                }
            }
            case ANY -> {
                boolean anyMatch = matchingElements.stream()
                        .anyMatch(e -> evaluateCondition(expression.condition(), e));
                if (anyMatch && expression.action() == DslExpression.Action.VIOLATION) {
                    violations.add(createViolation(policy, null, expression.actionArg()));
                }
            }
            case NONE -> {
                boolean noneMatch = matchingElements.stream()
                        .noneMatch(e -> evaluateCondition(expression.condition(), e));
                if (!noneMatch && expression.action() == DslExpression.Action.VIOLATION) {
                    violations.add(createViolation(policy, null,
                            "Some elements matched: " + expression.actionArg()));
                }
            }
        }

        // Handle unconditional yield violation (no where clause)
        if (expression.condition() == null
                && expression.action() == DslExpression.Action.VIOLATION) {
            if (expression.quantifier() == DslExpression.Quantifier.EACH
                    || expression.quantifier() == DslExpression.Quantifier.ANY) {
                for (SpecElement element : matchingElements) {
                    violations.add(createViolation(policy, element, expression.actionArg()));
                }
            }
        }

        return violations;
    }

    private boolean evaluateCondition(DslExpression.Condition condition, SpecElement element) {
        if (condition == null) {
            return true; // No condition = always matches
        }

        return switch (condition) {
            case DslExpression.ComparisonCondition c -> evaluateComparison(c, element);
            case DslExpression.UnaryCheckCondition u -> evaluateUnary(u, element);
            case DslExpression.CompoundCondition cc ->
                    evaluateCompound(cc, element);
        };
    }

    private boolean evaluateComparison(DslExpression.ComparisonCondition c,
                                        SpecElement element) {
        // HAS operator checks field existence directly, no value resolution needed
        if (c.operator() == DslExpression.ComparisonOperator.HAS) {
            return element.hasField(c.right());
        }

        String leftValue = resolveField(c.left(), element);
        if (leftValue == null) return false;

        String rightValue = stripQuotes(c.right());

        return switch (c.operator()) {
            case EQUALS -> rightValue != null && leftValue.equals(rightValue);
            case NOT_EQUALS -> rightValue == null || !leftValue.equals(rightValue);
            case MATCHES -> {
                try {
                    yield Pattern.compile(rightValue != null ? rightValue : ".*").matcher(leftValue).matches();
                } catch (Exception e) {
                    yield false;
                }
            }
            case CONTAINS -> rightValue != null && leftValue.contains(rightValue);
            case HAS -> {
                yield true;
            }
        };
    }

    private boolean evaluateUnary(DslExpression.UnaryCheckCondition u,
                                   SpecElement element) {
        return switch (u.operator()) {
            case IS_DEFINED -> element.hasField(u.field());
            case IS_DEPRECATED -> element.isDeprecated();
            case IS_READ_ONLY -> element.isReadOnly();
        };
    }

    private boolean evaluateCompound(DslExpression.CompoundCondition cc,
                                      SpecElement element) {
        return switch (cc.operator()) {
            case NOT -> !evaluateCondition(cc.left(), element);
            case AND -> evaluateCondition(cc.left(), element)
                    && evaluateCondition(cc.right(), element);
            case OR -> evaluateCondition(cc.left(), element)
                    || evaluateCondition(cc.right(), element);
        };
    }

    private String stripQuotes(String value) {
        if (value == null) return null;
        if ((value.startsWith("\"") && value.endsWith("\""))
                || (value.startsWith("'") && value.endsWith("'"))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private String resolveField(String fieldPath, SpecElement element) {
        if ("endpoint.protocol".equals(fieldPath)) {
            return element.protocol;
        }
        if ("endpoint.method".equals(fieldPath) || "method".equals(fieldPath)) {
            return element.method;
        }
        if ("endpoint.operationId".equals(fieldPath)
                || "endpoint.operationid".equals(fieldPath)
                || "operationId".equals(fieldPath)) {
            return element.operationId;
        }
        if ("path".equals(fieldPath) || "endpoint.path".equals(fieldPath)) {
            return element.path;
        }
        if ("field.name".equals(fieldPath) || "field.type".equals(fieldPath)) {
            return element.fieldValue(fieldPath);
        }
        if ("schema.name".equals(fieldPath)) {
            return element.name;
        }
        return null;
    }

    private SpecData getSpecData(String source) {
        // In production, this would load the actual OpenAPI spec
        // For now, we return mock data with common elements
        return switch (source) {
            case "spec.paths" -> new SpecData(List.of(
                    new SpecElement("/api/v1/users", "GET", "listUsers", "https", false, false, null),
                    new SpecElement("/api/v1/users", "POST", "createUser", "https", false, false, null),
                    new SpecElement("/api/v1/users/{id}", "GET", "getUser", "https", false, false, null),
                    new SpecElement("/api/v1/users/{id}", "DELETE", "deleteUser", "https", false, false, null)
            ));
            case "spec.endpoints" -> new SpecData(List.of(
                    new SpecElement("/api/v1/users", "GET", "listUsers", "https", false, false, null),
                    new SpecElement("/api/v1/users", "POST", "createUser", "https", false, false, null),
                    new SpecElement("/api/v1/users/{id}", "GET", "getUser", "https", false, false, null)
            ));
            case "spec.operations" -> new SpecData(List.of(
                    new SpecElement(null, "GET", null, "https", false, false, null),
                    new SpecElement(null, "POST", null, "https", false, false, null),
                    new SpecElement(null, "PUT", null, "https", false, false, null),
                    new SpecElement(null, "DELETE", null, "https", false, false, null)
            ));
            case "spec.schemas" -> new SpecData(List.of(
                    new SpecElement(null, null, null, null, false, false, "User"),
                    new SpecElement(null, null, null, null, false, false, "Order"),
                    new SpecElement(null, null, null, null, true, false, "Product")
            ));
            default -> new SpecData(List.of());
        };
    }

    private List<SpecElement> filterByScope(List<SpecElement> elements, PolicyScope scope) {
        if (scope == null || scope.appliesToAll()) {
            return elements;
        }
        return elements.stream()
                .filter(e -> matchesScope(e, scope))
                .toList();
    }

    private boolean matchesScope(SpecElement element, PolicyScope scope) {
        // Check path patterns
        if (!scope.pathPatterns().isEmpty()) {
            boolean pathMatch = scope.pathPatterns().stream()
                    .anyMatch(pattern -> {
                        String globPattern = pattern
                                .replace("/**", "/.*")
                                .replace("*", "[^/]*");
                        return element.path != null
                                && Pattern.matches(globPattern, element.path);
                    });
            if (!pathMatch) return false;
        }

        // Check exclude paths
        if (!scope.excludePaths().isEmpty()) {
            boolean excluded = scope.excludePaths().stream()
                    .anyMatch(pattern -> {
                        String globPattern = pattern
                                .replace("/**", "/.*")
                                .replace("*", "[^/]*");
                        return element.path != null
                                && Pattern.matches(globPattern, element.path);
                    });
            if (excluded) return false;
        }

        // Check operations
        if (!scope.operations().isEmpty() && element.method != null) {
            boolean opMatch = scope.operations().stream()
                    .anyMatch(op -> op.name().equalsIgnoreCase(element.method));
            if (!opMatch) return false;
        }

        return true;
    }

    private String safeField(SpecElement element, java.util.function.Function<SpecElement, String> extractor) {
        if (element == null) return "";
        String val = extractor.apply(element);
        return val != null ? val : "";
    }

    private Violation createViolation(Policy policy, SpecElement element, String message) {
        String path = element != null && element.path != null ? element.path : "/*";
        String resolvedMessage = message != null
                ? message.replace("{endpoint.path}", safeField(element, e -> e.path))
                         .replace("{endpoint.method}", safeField(element, e -> e.method))
                         .replace("{field.name}", safeField(element, e -> e.name))
                         .replace("{path}", safeField(element, e -> e.path))
                : "Policy violation: " + policy.getName();
        if (resolvedMessage == null || resolvedMessage.isBlank()) {
            resolvedMessage = "Policy violation: " + policy.getName();
        }
        return new Violation(policy.getId(), policy.getName(),
                policy.getSeverity(), resolvedMessage != null ? resolvedMessage : "", path, null);
    }

    // ---- Spec data model for evaluation ----

    /**
     * Collection of spec elements used during evaluation.
     */
    record SpecData(List<SpecElement> elements) {}

    /**
     * A single element from a spec (path, endpoint, schema, etc.).
     */
    record SpecElement(
        String path,
        String method,
        String operationId,
        String protocol,
        boolean deprecated,
        boolean readOnly,
        String name
    ) {
        boolean hasField(String fieldName) {
            return switch (fieldName) {
                case "path" -> path != null;
                case "method" -> method != null;
                case "operationId" -> operationId != null;
                case "protocol" -> protocol != null;
                case "name" -> name != null;
                default -> false;
            };
        }

        String fieldValue(String fieldPath) {
            String field = fieldPath.contains(".")
                    ? fieldPath.substring(fieldPath.lastIndexOf('.') + 1)
                    : fieldPath;
            return switch (field) {
                case "name" -> name;
                case "type" -> "string"; // default type
                default -> null;
            };
        }

        boolean isDeprecated() { return deprecated; }
        boolean isReadOnly() { return readOnly; }
    }
}
