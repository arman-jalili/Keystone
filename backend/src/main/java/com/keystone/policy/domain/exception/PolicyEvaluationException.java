package com.keystone.policy.domain.exception;

import java.util.Objects;

/**
 * Exception thrown when policy evaluation fails unexpectedly.
 *
 * <p>This covers internal failures in the evaluation pipeline (e.g.
 * expression parsing errors, rule engine failures, resource exhaustion).
 * It does NOT cover policy violations — those are captured as
 * {@link com.keystone.policy.domain.model.Violation} instances in the
 * evaluation result.
 */
public class PolicyEvaluationException extends RuntimeException {

    public PolicyEvaluationException(String message) {
        super(Objects.requireNonNull(message, "message must not be null"));
    }

    public PolicyEvaluationException(String message, Throwable cause) {
        super(Objects.requireNonNull(message, "message must not be null"), cause);
    }
}
