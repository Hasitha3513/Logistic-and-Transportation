package com.transportlogistics.app.trip.infrastructure.adapters.in.web.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.UUID;

public record TripDelayRequest(
        @NotNull(message = "delayMinutes is required")
        @Min(value = 1, message = "delayMinutes must be at least 1 minute")
        Integer delayMinutes,

        @NotBlank(message = "reason is required")
        @Size(max = 500, message = "reason cannot exceed 500 characters")
        String reason,

        OffsetDateTime occurredAt,

        UUID locationId,

        @Size(max = 255, message = "locationDescription cannot exceed 255 characters")
        String locationDescription,

        @Size(max = 2000, message = "remarks cannot exceed 2000 characters")
        String remarks
) {}
