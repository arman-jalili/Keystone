package com.keystone.ingestion.interfaces.http;

import com.keystone.ingestion.application.dto.ErrorResponse;
import com.keystone.ingestion.domain.exception.DuplicateSpecException;
import com.keystone.ingestion.domain.exception.SpecParseException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global exception handler for the ingestion HTTP layer.
 *
 * <p>Every error follows the {@link ErrorResponse} envelope format.
 */
@RestControllerAdvice(basePackageClasses = IngestionController.class)
public class IngestionExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationError(MethodArgumentNotValidException ex) {
        List<ErrorResponse.ErrorDetail> details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ErrorResponse.ErrorDetail(fe.getField(), fe.getDefaultMessage()))
                .toList();
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("VALIDATION_ERROR", "Invalid request payload", details));
    }

    @ExceptionHandler(SpecParseException.class)
    public ResponseEntity<ErrorResponse> handleSpecParseError(SpecParseException ex) {
        List<ErrorResponse.ErrorDetail> details = ex.getDetails().stream()
                .map(ve -> new ErrorResponse.ErrorDetail(ve.field(), ve.message()))
                .toList();
        return ResponseEntity.unprocessableEntity()
                .body(new ErrorResponse("SPEC_PARSE_ERROR", ex.getMessage(), details));
    }

    @ExceptionHandler(DuplicateSpecException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateSpec(DuplicateSpecException ex) {
        return ResponseEntity.status(HttpStatus.OK).body(new ErrorResponse("SPEC_DUPLICATE", ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedError(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred"));
    }
}
