package com.transportlogistics.app.identity.infrastructure.adapters.in.web;

import com.transportlogistics.app.identity.domain.AuthenticationFailedException;
import com.transportlogistics.app.shared.web.ApiError;
import com.transportlogistics.app.shared.web.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.util.List;

@RestControllerAdvice
class IdentityExceptionHandler {
    @ExceptionHandler(AuthenticationFailedException.class)
    ResponseEntity<ApiError> authentication(AuthenticationFailedException ex, HttpServletRequest request) {
        var status = HttpStatus.UNAUTHORIZED;
        var correlationId = (String) request.getAttribute(CorrelationIdFilter.ATTRIBUTE);
        return ResponseEntity.status(status).body(new ApiError(OffsetDateTime.now(), status.value(),
                status.getReasonPhrase(), "AUTHENTICATION_FAILED", ex.getMessage(), request.getRequestURI(),
                correlationId, List.of()));
    }
}
