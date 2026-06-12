package com.keystone.policy.dsl;

import java.util.Objects;

/**
 * Parsed representation of a policy DSL expression.
 *
 * <p>A DSL expression has the structure:
 * {@code <quantifier> in <source> [where <condition>] yield <action>}
 *
 * @param quantifier  The quantifier: EACH, ANY, or NONE
 * @param source      The data source to iterate over (e.g. "spec.paths", "spec.endpoints")
 * @param condition   Optional condition expression (null if no where clause)
 * @param action      The action to take: "violation(...)" or "pass()"
 * @param actionArg   The argument to the action (violation message, or null for pass)
 */
public record DslExpression(
        Quantifier quantifier, String source, Condition condition, Action action, String actionArg) {
    public DslExpression {
        Objects.requireNonNull(quantifier, "quantifier must not be null");
        Objects.requireNonNull(source, "source must not be null");
        Objects.requireNonNull(action, "action must not be null");
    }

    /** The quantifier type. */
    public enum Quantifier {
        EACH,
        ANY,
        NONE
    }

    /** The action type. */
    public enum Action {
        VIOLATION,
        PASS
    }

    /**
     * A condition expression in the DSL.
     *
     * <p>Conditions can be simple comparisons, unary checks, or compound
     * expressions with AND/OR/NOT.
     */
    public sealed interface Condition permits ComparisonCondition, UnaryCheckCondition, CompoundCondition {}

    /** A comparison: {@code <left> <operator> <right>} */
    record ComparisonCondition(String left, ComparisonOperator operator, String right) implements Condition {
        public ComparisonCondition {
            Objects.requireNonNull(left, "left must not be null");
            Objects.requireNonNull(operator, "operator must not be null");
        }
    }

    /** A unary check: {@code is_defined}, {@code is_deprecated}, {@code is_read_only} */
    record UnaryCheckCondition(String field, UnaryOperator operator) implements Condition {
        public UnaryCheckCondition {
            Objects.requireNonNull(field, "field must not be null");
            Objects.requireNonNull(operator, "operator must not be null");
        }
    }

    /** A compound condition: {@code <left> AND|OR <right>} or {@code NOT <condition>} */
    record CompoundCondition(Condition left, LogicalOperator operator, Condition right) implements Condition {
        public CompoundCondition {
            Objects.requireNonNull(left, "left must not be null");
            Objects.requireNonNull(operator, "operator must not be null");
        }
    }

    /** Comparison operators. */
    public enum ComparisonOperator {
        EQUALS("=="),
        NOT_EQUALS("!="),
        MATCHES("matches"),
        CONTAINS("contains"),
        HAS("has");

        private final String symbol;

        ComparisonOperator(String symbol) {
            this.symbol = symbol;
        }

        public String getSymbol() {
            return symbol;
        }

        public static ComparisonOperator fromSymbol(String symbol) {
            for (ComparisonOperator op : values()) {
                if (op.symbol.equals(symbol)) return op;
            }
            throw new IllegalArgumentException("Unknown operator: " + symbol);
        }
    }

    /** Unary check operators. */
    public enum UnaryOperator {
        IS_DEFINED("is_defined"),
        IS_DEPRECATED("is_deprecated"),
        IS_READ_ONLY("is_read_only");

        private final String keyword;

        UnaryOperator(String keyword) {
            this.keyword = keyword;
        }

        public String getKeyword() {
            return keyword;
        }

        public static UnaryOperator fromKeyword(String keyword) {
            for (UnaryOperator op : values()) {
                if (op.keyword.equals(keyword)) return op;
            }
            throw new IllegalArgumentException("Unknown unary operator: " + keyword);
        }
    }

    /** Logical operators for compound conditions. */
    public enum LogicalOperator {
        AND("and"),
        OR("or"),
        NOT("not");

        private final String keyword;

        LogicalOperator(String keyword) {
            this.keyword = keyword;
        }

        public String getKeyword() {
            return keyword;
        }
    }
}
