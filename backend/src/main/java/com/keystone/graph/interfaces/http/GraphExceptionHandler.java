package com.keystone.graph.interfaces.http;

import com.keystone.graph.application.dto.ErrorResponse;
import com.keystone.graph.domain.exception.DuplicateDependencyException;
import com.keystone.graph.domain.exception.UnknownServiceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

/**
 * Global exception handler for the Dependency Graph HTTP layer.
 *
 * <p>Every error follows the {@link ErrorResponse} envelope format.
 */
@RestControllerAdvice(basePackageClasses = GraphController.class)
public class GraphExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationError(MethodArgumentNotValidException ex) {
        List<ErrorResponse.ErrorDetail> details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ErrorResponse.ErrorDetail(fe.getField(), fe.getDefaultMessage()))
                .toList();
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("VALIDATION_ERROR", "Invalid request payload", details));
    }

    @ExceptionHandler(UnknownServiceException.class)
    public ResponseEntity<ErrorResponse> handleUnknownService(UnknownServiceException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ErrorResponse("UNKNOWN_SERVICE", ex.getMessage()));
    }

    @ExceptionHandler(DuplicateDependencyException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateDependency(DuplicateDependencyException ex) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(new ErrorResponse("DEPENDENCY_DUPLICATE", ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedError(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred"));
    }
}
