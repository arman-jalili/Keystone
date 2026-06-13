// Canonical Reference: .pi/architecture/modules/breaking-change-analysis.md
// Module: breaking-change-analysis
package com.keystone.analysis.interfaces.http;

import com.keystone.analysis.application.dto.ErrorResponse;
import com.keystone.analysis.domain.exception.DiffAnalysisException;
import com.keystone.analysis.domain.exception.NoBaseVersionException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global exception handler for the breaking change analysis HTTP layer.
 */
@RestControllerAdvice(basePackageClasses = BreakingAnalysisController.class)
public class BreakingAnalysisExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationError(MethodArgumentNotValidException ex) {
        List<ErrorResponse.ErrorDetail> details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ErrorResponse.ErrorDetail(fe.getField(), fe.getDefaultMessage()))
                .toList();
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("VALIDATION_ERROR", "Invalid request payload", details));
    }

    @ExceptionHandler(DiffAnalysisException.class)
    public ResponseEntity<ErrorResponse> handleDiffAnalysisError(DiffAnalysisException ex) {
        ErrorResponse.ErrorDetail detail = new ErrorResponse.ErrorDetail(ex.getStage(), ex.getMessage());
        return ResponseEntity.unprocessableEntity()
                .body(new ErrorResponse(
                        "DIFF_ANALYSIS_ERROR",
                        "Diff analysis failed for " + ex.getRepository() + "/" + ex.getSpecPath(),
                        List.of(detail)));
    }

    @ExceptionHandler(NoBaseVersionException.class)
    public ResponseEntity<ErrorResponse> handleNoBaseVersion(NoBaseVersionException ex) {
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(
                        "NO_BASE_VERSION",
                        "Could not resolve base version for " + ex.getRepository() + "/" + ex.getSpecPath()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(new ErrorResponse("INVALID_INPUT", ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedError(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred"));
    }
}
