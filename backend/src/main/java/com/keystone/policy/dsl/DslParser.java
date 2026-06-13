// Canonical Reference: .pi/architecture/modules/policy-engine.md
// Module: policy-engine
package com.keystone.policy.dsl;

import com.keystone.policy.domain.exception.PolicyParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses Policy DSL expression strings into structured {@link DslExpression} objects.
 *
 * <p>Grammar (from {@code docs/policy-dsl.md}):
 * <pre>
 * rule     = quantifier "in" source ["where" condition] "yield" action
 * quantifier = "each" | "any" | "none"
 * source   = "spec.paths" | "spec.schemas" | "spec.operations" | "spec.endpoints"
 * condition = comparison | unary_expr | condition "and" condition
 * action   = "violation(" message ")" | "pass()"
 * </pre>
 */
public class DslParser {

    /**
     * Parses a full DSL expression string.
     *
     * @param expression the DSL expression to parse
     * @return the parsed expression
     * @throws PolicyParseException if the expression cannot be parsed
     */
    public DslExpression parse(String expression) throws PolicyParseException {
        if (expression == null || expression.isBlank()) {
            throw new PolicyParseException(
                    "Empty DSL expression", List.of(new PolicyParseException.ParseError(1, 1, "Expression is empty")));
        }

        String trimmed = expression.trim();
        List<PolicyParseException.ParseError> errors = new ArrayList<>();

        // Tokenize by whitespace, respecting quoted strings
        List<String> tokens = tokenize(trimmed);
        if (tokens.size() < 4) {
            throw new PolicyParseException(
                    "Invalid DSL expression",
                    List.of(new PolicyParseException.ParseError(
                            1, 1, "Expression too short. Expected: <quantifier> in <source> yield <action>")));
        }

        // 1. Parse quantifier
        DslExpression.Quantifier quantifier = parseQuantifier(tokens.get(0), errors);

        // 2. Skip optional loop variable (e.g. "endpoint" in "each endpoint in source")
        int idx = 1;
        if (idx < tokens.size() && !"in".equals(tokens.get(idx))) {
            // The next token is a loop variable name, skip it
            idx++;
        }

        // 3. Parse "in" keyword
        if (idx >= tokens.size() || !"in".equals(tokens.get(idx))) {
            errors.add(new PolicyParseException.ParseError(1, 1, "Expected 'in' after quantifier"));
        } else {
            idx++;
        }

        // 4. Parse source
        if (idx >= tokens.size()) {
            errors.add(new PolicyParseException.ParseError(1, 1, "Expected source after 'in'"));
            throw new PolicyParseException("Invalid DSL expression", errors);
        }
        String source = tokens.get(idx);
        idx++;

        // 5. Parse optional "where" + condition
        DslExpression.Condition condition = null;
        if (idx < tokens.size() && "where".equals(tokens.get(idx))) {
            idx++;
            if (idx >= tokens.size()) {
                errors.add(new PolicyParseException.ParseError(1, 1, "Expected condition after 'where'"));
            } else {
                condition = parseCondition(tokens.subList(idx, tokens.size()));
                // Find where condition ends (before "yield")
                int conditionEnd = findYieldIndex(tokens, idx);
                idx = conditionEnd;
            }
        }

        // 6. Parse "yield"
        if (idx >= tokens.size() || !"yield".equals(tokens.get(idx))) {
            errors.add(new PolicyParseException.ParseError(1, 1, "Expected 'yield' keyword"));
            throw new PolicyParseException("Invalid DSL expression", errors);
        }
        idx++;

        // 7. Parse action
        if (idx >= tokens.size()) {
            errors.add(new PolicyParseException.ParseError(1, 1, "Expected action after 'yield'"));
            throw new PolicyParseException("Invalid DSL expression", errors);
        }

        String actionToken = tokens.get(idx);
        DslExpression.Action action;
        String actionArg = null;

        if (actionToken.startsWith("violation(")) {
            action = DslExpression.Action.VIOLATION;
            actionArg = extractStringArg(actionToken);
        } else if (actionToken.startsWith("pass()") || "pass".equals(actionToken)) {
            action = DslExpression.Action.PASS;
        } else {
            errors.add(new PolicyParseException.ParseError(
                    1, 1, "Unknown action: " + actionToken + ". Expected 'violation(...)' or 'pass()'"));
            action = DslExpression.Action.PASS;
        }

        if (!errors.isEmpty()) {
            throw new PolicyParseException("DSL parsing failed", errors);
        }

        return new DslExpression(quantifier, source, condition, action, actionArg);
    }

    private DslExpression.Quantifier parseQuantifier(String token, List<PolicyParseException.ParseError> errors) {
        return switch (token.toLowerCase()) {
            case "each" -> DslExpression.Quantifier.EACH;
            case "any" -> DslExpression.Quantifier.ANY;
            case "none" -> DslExpression.Quantifier.NONE;
            default -> {
                errors.add(new PolicyParseException.ParseError(
                        1, 1, "Unknown quantifier: " + token + ". Expected one of: each, any, none"));
                yield DslExpression.Quantifier.EACH;
            }
        };
    }

    private DslExpression.Condition parseCondition(List<String> tokens) {
        // Find where condition ends (before "yield")
        int endIdx = findYieldIndex(tokens, 0);
        List<String> conditionTokens = tokens.subList(0, endIdx);

        if (conditionTokens.isEmpty()) {
            throw new IllegalArgumentException("Empty condition");
        }

        // Check for unary check (field path ends with unary operator keyword)
        String last = conditionTokens.get(conditionTokens.size() - 1);
        for (DslExpression.UnaryOperator op : DslExpression.UnaryOperator.values()) {
            if (last.endsWith(op.getKeyword())) {
                // Format: "field.field.is_deprecated" → field="field.field"
                String keyword = op.getKeyword();
                String field = last.substring(0, last.length() - keyword.length() - 1);
                return new DslExpression.UnaryCheckCondition(field, op);
            }
        }

        // Handle "not" prefix
        if (conditionTokens.size() >= 2 && "not".equals(conditionTokens.get(0))) {
            // not condition
            var inner = parseCondition(conditionTokens.subList(1, conditionTokens.size()));
            return new DslExpression.CompoundCondition(inner, DslExpression.LogicalOperator.NOT, null);
        }

        // Handle "not" inside: "where not endpoint.has(...)"
        if (conditionTokens.size() >= 3 && "not".equals(conditionTokens.get(0))) {
            String field = conditionTokens.get(1);
            String opStr = conditionTokens.get(2);
            String value = conditionTokens.size() > 3 ? conditionTokens.get(3) : null;

            var inner = new DslExpression.ComparisonCondition(
                    field, DslExpression.ComparisonOperator.fromSymbol(opStr), value);
            return new DslExpression.CompoundCondition(inner, DslExpression.LogicalOperator.NOT, null);
        }

        // Handle function call conditions: field.matches("pattern") / field.contains("value") / field.has("field")
        if (conditionTokens.size() == 1) {
            String full = conditionTokens.get(0);
            int parenIdx = full.indexOf('(');
            if (parenIdx > 0 && full.endsWith(")")) {
                // Extract: "endpoint.has(\"operationId\")" → field="endpoint", op="has", arg="operationId"
                String beforeParen = full.substring(0, parenIdx);
                String arg = full.substring(parenIdx + 1, full.length() - 1);
                // Strip surrounding quotes from arg
                if ((arg.startsWith("\"") && arg.endsWith("\"")) || (arg.startsWith("'") && arg.endsWith("'"))) {
                    arg = arg.substring(1, arg.length() - 1);
                }
                // Split "field.operator" into field and operator
                int dotIdx = beforeParen.lastIndexOf('.');
                if (dotIdx > 0) {
                    String field = beforeParen.substring(0, dotIdx);
                    String opStr = beforeParen.substring(dotIdx + 1);
                    if (opStr.equals("matches") || opStr.equals("contains") || opStr.equals("has")) {
                        DslExpression.ComparisonOperator op = DslExpression.ComparisonOperator.fromSymbol(opStr);
                        return new DslExpression.ComparisonCondition(field, op, arg);
                    }
                }
            }
        }

        // Handle comparisons: field operator value
        if (conditionTokens.size() >= 3) {
            String field = conditionTokens.get(0);
            String opStr = conditionTokens.get(1);
            String value = conditionTokens.size() > 2 ? conditionTokens.get(2) : null;

            try {
                DslExpression.ComparisonOperator op = DslExpression.ComparisonOperator.fromSymbol(opStr);
                return new DslExpression.ComparisonCondition(field, op, value);
            } catch (IllegalArgumentException e) {
                // Try binary operators: "matches", "contains", "has"
                if (opStr.equals("matches") || opStr.equals("contains") || opStr.equals("has")) {
                    DslExpression.ComparisonOperator op = DslExpression.ComparisonOperator.fromSymbol(opStr);
                    String rightValue = conditionTokens.size() > 2
                            ? String.join(" ", conditionTokens.subList(2, conditionTokens.size()))
                            : null;
                    return new DslExpression.ComparisonCondition(field, op, rightValue);
                }
                throw new IllegalArgumentException("Unknown operator: " + opStr);
            }
        }

        throw new IllegalArgumentException("Invalid condition format");
    }

    private int findYieldIndex(List<String> tokens, int startIdx) {
        for (int i = startIdx; i < tokens.size(); i++) {
            if ("yield".equals(tokens.get(i))) {
                return i;
            }
        }
        return tokens.size();
    }

    private String extractStringArg(String token) {
        // Parse: violation("message") or violation('message')
        int parenStart = token.indexOf('(');
        int parenEnd = token.lastIndexOf(')');
        if (parenStart < 0 || parenEnd <= parenStart) {
            return "";
        }
        String arg = token.substring(parenStart + 1, parenEnd);
        // Remove surrounding quotes
        if ((arg.startsWith("\"") && arg.endsWith("\"")) || (arg.startsWith("'") && arg.endsWith("'"))) {
            arg = arg.substring(1, arg.length() - 1);
        }
        return arg;
    }

    /**
     * Simple tokenizer that splits on whitespace while preserving
     * quoted strings and parenthesized expressions.
     */
    List<String> tokenize(String input) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        char quoteChar = '"';

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            if (inQuotes) {
                current.append(c);
                if (c == quoteChar) {
                    inQuotes = false;
                }
            } else if (c == '"' || c == '\'') {
                current.append(c);
                inQuotes = true;
                quoteChar = c;
            } else if (Character.isWhitespace(c)) {
                if (!current.isEmpty()) {
                    tokens.add(current.toString());
                    current = new StringBuilder();
                }
            } else {
                current.append(c);
            }
        }
        if (!current.isEmpty()) {
            tokens.add(current.toString());
        }
        return tokens;
    }
}
