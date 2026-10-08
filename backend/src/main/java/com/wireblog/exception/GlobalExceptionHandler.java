package com.wireblog.exception;

import com.wireblog.dto.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> handleApiException(ApiException ex, HttpServletRequest req) {
        ApiError body = new ApiError(Instant.now(), ex.getStatus().value(), ex.getStatus().getReasonPhrase(),
                ex.getMessage(), req.getRequestURI());
        return ResponseEntity.status(ex.getStatus()).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .collect(Collectors.joining("; "));
        ApiError body = new ApiError(Instant.now(), HttpStatus.BAD_REQUEST.value(), "Bad Request", message, req.getRequestURI());
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex, HttpServletRequest req) {
        log.error("Unhandled request failure for {} {}", req.getMethod(), req.getRequestURI(), ex);
        ApiError body = new ApiError(Instant.now(), HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Internal Server Error", "An unexpected error occurred.", req.getRequestURI());
        return ResponseEntity.internalServerError().body(body);
    }
}
