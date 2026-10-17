package com.vn.otech.error;

import jakarta.validation.ConstraintViolationException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleStatus(ResponseStatusException exception) {
        int statusCode = exception.getStatusCode().value();
        if (statusCode >= 500) {
            logger.error("Request failed with status {}", statusCode);
        } else {
            logger.warn("Request rejected with status {}", statusCode);
        }
        HttpStatus status = HttpStatus.valueOf(statusCode);
        String message = exception.getReason() == null ? status.getReasonPhrase() : exception.getReason();
        return ResponseEntity.status(status).body(error(status, message, List.of()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleBodyValidation(MethodArgumentNotValidException exception) {
        List<ApiErrorResponse.FieldError> details = exception.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> new ApiErrorResponse.FieldError(
                        fieldError.getField(), fieldError.getDefaultMessage() == null
                                ? "Invalid value" : fieldError.getDefaultMessage()))
                .toList();
        return ResponseEntity.badRequest().body(error(
                HttpStatus.BAD_REQUEST, "Request validation failed", details));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException exception) {
        List<ApiErrorResponse.FieldError> details = exception.getConstraintViolations().stream()
                .map(violation -> new ApiErrorResponse.FieldError(
                        violation.getPropertyPath().toString(), violation.getMessage()))
                .toList();
        return ResponseEntity.badRequest().body(error(
                HttpStatus.BAD_REQUEST, "Request validation failed", details));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadableBody() {
        return ResponseEntity.badRequest().body(error(
                HttpStatus.BAD_REQUEST, "Request body is missing or malformed", List.of()));
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    public ResponseEntity<ApiErrorResponse> handleParameterBinding(Exception exception) {
        String field = exception instanceof MethodArgumentTypeMismatchException mismatch
                ? mismatch.getName()
                : ((MissingServletRequestParameterException) exception).getParameterName();
        return ResponseEntity.badRequest().body(error(
                HttpStatus.BAD_REQUEST, "Request parameter is missing or invalid",
                List.of(new ApiErrorResponse.FieldError(field, "Provide a valid value"))));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleForbidden() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error(
                HttpStatus.FORBIDDEN, "You are not allowed to perform this action", List.of()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataConflict() {
        logger.warn("Request conflicted with a database constraint");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error(
                HttpStatus.CONFLICT, "Resource conflicts with existing data", List.of()));
    }

    private ApiErrorResponse error(HttpStatus status, String message,
                                   List<ApiErrorResponse.FieldError> details) {
        return new ApiErrorResponse(new ApiErrorResponse.ErrorBody(
                status.name().toLowerCase(), message, details));
    }
}
