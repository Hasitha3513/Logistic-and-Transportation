package com.transportlogistics.app.fleet.infrastructure.adapters.in.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

public record DriverExceptionRequest(
        @NotBlank(message = "Exception type is required")
        String exceptionType,

        @NotNull(message = "Start time is required")
        OffsetDateTime startTime,

        @NotNull(message = "End time is required")
        OffsetDateTime endTime,

        String reason,

        String remarks
) {
}
