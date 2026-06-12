package com.keystone.policy.interfaces.http;

import com.keystone.policy.application.dto.ErrorResponse;
import com.keystone.policy.domain.exception.PolicyEvaluationException;
import com.keystone.policy.domain.exception.PolicyNotFoundException;
import com.keystone.policy.domain.exception.PolicyParseException;
import com.keystone.policy.domain.exception.PolicySyncException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Exception handler for the Policy Engine bounded context.
 *
 * <p>Translates domain exceptions into consistent HTTP error responses
 * following the standard error envelope from {@link ErrorResponse}.
 */
@RestControllerAdvice(basePackageClasses = PolicyController.class)
public class PolicyExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(PolicyExceptionHandler.class);

    /**
     * Handles validation errors from {@code @Valid} annotated request bodies.
     *
     * @param ex the validation exception
     * @return 400 Bad Request with field-level error details
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidationError(MethodArgumentNotValidException ex) {
        List<ErrorResponse.ErrorDetail> details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ErrorResponse.ErrorDetail(fe.getField(), fe.getDefaultMessage()))
                .toList();

        return new ErrorResponse("VALIDATION_ERROR", "Request validation failed", details);
    }

    /**
     * Handles policy parse exceptions (invalid DSL expressions).
     *
     * @param ex the parse exception
     * @return 422 Unprocessable Entity with parse error details
     */
    @ExceptionHandler(PolicyParseException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ErrorResponse handlePolicyParseError(PolicyParseException ex) {
        List<ErrorResponse.ErrorDetail> details = ex.getErrors().stream()
                .map(e -> new ErrorResponse.ErrorDetail("line " + e.line() + ":" + e.column(), e.message()))
                .toList();

        return new ErrorResponse("POLICY_PARSE_ERROR", ex.getMessage(), details);
    }

    /**
     * Handles policy evaluation exceptions.
     *
     * @param ex the evaluation exception
     * @return 422 Unprocessable Entity
     */
    @ExceptionHandler(PolicyEvaluationException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ErrorResponse handleEvaluationError(PolicyEvaluationException ex) {
        log.error("Policy evaluation error: {}", ex.getMessage(), ex);
        return new ErrorResponse("EVALUATION_ERROR", ex.getMessage());
    }

    /**
     * Handles policy sync exceptions.
     *
     * @param ex the sync exception
     * @return 500 Internal Server Error
     */
    @ExceptionHandler(PolicySyncException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleSyncError(PolicySyncException ex) {
        log.error("Policy sync error: {}", ex.getMessage(), ex);
        return new ErrorResponse("SYNC_ERROR", ex.getMessage());
    }

    /**
     * Handles policy not found exceptions.
     *
     * @param ex the not-found exception
     * @return 404 Not Found
     */
    @ExceptionHandler(PolicyNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(PolicyNotFoundException ex) {
        return new ErrorResponse("NOT_FOUND", ex.getMessage());
    }

    /**
     * Catch-all for unexpected errors.
     *
     * @param ex the unexpected exception
     * @return 500 Internal Server Error
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleUnexpectedError(Exception ex) {
        log.error("Unexpected error in policy engine: {}", ex.getMessage(), ex);
        return new ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred");
    }
}
