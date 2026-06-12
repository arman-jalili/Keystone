package com.keystone.ingestion.interfaces.http;

import com.keystone.ingestion.application.dto.ErrorResponse;
import com.keystone.ingestion.domain.exception.DuplicateSpecException;
import com.keystone.ingestion.domain.exception.SpecParseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.List;

/**
 * Global exception handler for the ingestion HTTP layer.
 *
 * <p>Defines the error response contract for all ingestion endpoints.
 * Every error follows the {@link ErrorResponse} envelope format.
 */
public class IngestionExceptionHandler {

    /**
     * Handles validation errors on the incoming spec payload.
     *
     * @param ex the validation exception
     * @return 400 Bad Request with field-level error details
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationError(MethodArgumentNotValidException ex) {
        List<ErrorResponse.ErrorDetail> details = ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> new ErrorResponse.ErrorDetail(fe.getField(), fe.getDefaultMessage()))
            .toList();
        return ResponseEntity.badRequest()
            .body(new ErrorResponse("VALIDATION_ERROR", "Invalid request payload", details));
    }

    /**
     * Handles spec parse/validation failures.
     *
     * @param ex the spec parse exception
     * @return 422 Unprocessable Entity with parse error details
     */
    @ExceptionHandler(SpecParseException.class)
    public ResponseEntity<ErrorResponse> handleSpecParseError(SpecParseException ex) {
        List<ErrorResponse.ErrorDetail> details = ex.getDetails().stream()
            .map(ve -> new ErrorResponse.ErrorDetail(ve.field(), ve.message()))
            .toList();
        return ResponseEntity.unprocessableEntity()
            .body(new ErrorResponse("SPEC_PARSE_ERROR", ex.getMessage(), details));
    }

    /**
     * Handles duplicate spec detection.
     *
     * @param ex the duplicate spec exception
     * @return 200 OK with the existing event ID (idempotent, not an error)
     */
    @ExceptionHandler(DuplicateSpecException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateSpec(DuplicateSpecException ex) {
        return ResponseEntity.status(HttpStatus.OK)
            .body(new ErrorResponse("SPEC_DUPLICATE", ex.getMessage()));
    }

    /**
     * Fallback handler for unexpected errors.
     *
     * @param ex the unexpected exception
     * @return 500 Internal Server Error
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedError(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred"));
    }
}
