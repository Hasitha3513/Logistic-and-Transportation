package com.transportlogistics.app.delivery.adapters.inbound.web.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

public record RescheduleRedeliveryRequest(
        @NotNull(message = "Expected version is required")
        Long expectedVersion,

        @Size(max = 500, message = "Supersede reason must not exceed 500 characters")
        String supersedeReason,

        @NotNull(message = "Scheduled start time is required")
        OffsetDateTime scheduledStartTime,

        @NotNull(message = "Scheduled end time is required")
        OffsetDateTime scheduledEndTime
) {
}
