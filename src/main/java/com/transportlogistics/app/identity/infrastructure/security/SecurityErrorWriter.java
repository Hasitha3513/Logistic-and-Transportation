package com.transportlogistics.app.identity.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transportlogistics.app.shared.web.ApiError;
import com.transportlogistics.app.shared.web.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
class SecurityErrorWriter {
    private final ObjectMapper objectMapper;

    void write(HttpServletRequest request, HttpServletResponse response, HttpStatus status, String code, String message)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        var correlationId = (String) request.getAttribute(CorrelationIdFilter.ATTRIBUTE);
        objectMapper.writeValue(response.getOutputStream(), new ApiError(OffsetDateTime.now(), status.value(),
                status.getReasonPhrase(), code, message, request.getRequestURI(), correlationId, List.of()));
    }
}
