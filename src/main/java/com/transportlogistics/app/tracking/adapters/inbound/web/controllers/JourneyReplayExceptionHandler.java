package com.transportlogistics.app.tracking.adapters.inbound.web.controllers;

import com.transportlogistics.app.shared.web.ApiError;
import com.transportlogistics.app.shared.web.CorrelationIdFilter;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayError;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = JourneyReplayController.class)
final class JourneyReplayExceptionHandler {
    @ExceptionHandler(JourneyReplayException.class)
    ResponseEntity<ApiError> handle(JourneyReplayException exception, HttpServletRequest request) {
        HttpStatus status = status(exception.error());
        ApiError body = new ApiError(OffsetDateTime.now(), status.value(), status.getReasonPhrase(),
                exception.error().name(), message(exception.error()), request.getRequestURI(),
                (String) request.getAttribute(CorrelationIdFilter.ATTRIBUTE), List.of());
        return ResponseEntity.status(status).cacheControl(org.springframework.http.CacheControl.noStore())
                .header("Referrer-Policy", "no-referrer").body(body);
    }

    private static HttpStatus status(JourneyReplayError error) {
        return switch (error) {
            case SAFE_ABSENCE, REPLAY_EVIDENCE_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case REQUIRED_CAPABILITY_UNAVAILABLE, RETENTION_UNAVAILABLE, ATTRIBUTION_UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
            default -> HttpStatus.BAD_REQUEST;
        };
    }

    private static String message(JourneyReplayError error) {
        return switch (error) {
            case SAFE_ABSENCE, REPLAY_EVIDENCE_NOT_FOUND -> "Journey replay evidence not found";
            case REQUIRED_CAPABILITY_UNAVAILABLE -> "Requested replay capability is unavailable";
            default -> "Journey replay request could not be completed";
        };
    }
}
