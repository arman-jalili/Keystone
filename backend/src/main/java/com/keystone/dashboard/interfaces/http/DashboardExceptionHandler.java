// Canonical Reference: .pi/architecture/modules/dashboard.md#error-handling
// Implements: @RestControllerAdvice for dashboard error responses
package com.keystone.dashboard.interfaces.http;

import com.keystone.dashboard.application.dto.ErrorResponse;
import com.keystone.dashboard.domain.exception.DashboardDataNotFoundException;
import com.keystone.dashboard.domain.exception.HealthScoreComputationException;
import com.keystone.dashboard.domain.exception.InvalidTimeRangeException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global exception handler for the dashboard HTTP layer.
 *
 * <p>Every error follows the {@link ErrorResponse} envelope format.
 */
@RestControllerAdvice(basePackageClasses = DashboardController.class)
public class DashboardExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationError(MethodArgumentNotValidException ex) {
        List<ErrorResponse.ErrorDetail> details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ErrorResponse.ErrorDetail(fe.getField(), fe.getDefaultMessage()))
                .toList();
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("VALIDATION_ERROR", "Invalid request payload", details));
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(SecurityException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse("FORBIDDEN", "Insufficient permissions"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(new ErrorResponse("BAD_REQUEST", ex.getMessage()));
    }

    @ExceptionHandler(DashboardDataNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleDataNotFound(DashboardDataNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("DATA_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(InvalidTimeRangeException.class)
    public ResponseEntity<ErrorResponse> handleInvalidTimeRange(InvalidTimeRangeException ex) {
        return ResponseEntity.badRequest().body(new ErrorResponse("INVALID_TIME_RANGE", ex.getMessage()));
    }

    @ExceptionHandler(HealthScoreComputationException.class)
    public ResponseEntity<ErrorResponse> handleHealthScoreComputation(HealthScoreComputationException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ErrorResponse("HEALTH_SCORE_ERROR", ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedError(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred"));
    }
}
