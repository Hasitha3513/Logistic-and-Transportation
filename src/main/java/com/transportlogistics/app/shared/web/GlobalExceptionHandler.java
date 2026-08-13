package com.transportlogistics.app.shared.web;

import com.transportlogistics.app.shared.domain.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(NotFoundException.class)
    ResponseEntity<ApiError> notFound(NotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiError("NOT_FOUND", ex.getMessage(), OffsetDateTime.now(), List.of()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ApiError> badRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(new ApiError("BAD_REQUEST", ex.getMessage(), OffsetDateTime.now(), List.of()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> validation(MethodArgumentNotValidException ex) {
        var v = ex.getBindingResult().getFieldErrors().stream().map(e -> new ApiError.FieldViolation(e.getField(), e.getDefaultMessage())).toList();
        return ResponseEntity.badRequest().body(new ApiError("VALIDATION_ERROR", "Request validation failed", OffsetDateTime.now(), v));
    }
}
