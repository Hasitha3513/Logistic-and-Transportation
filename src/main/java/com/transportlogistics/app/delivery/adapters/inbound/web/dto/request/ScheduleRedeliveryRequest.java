package com.transportlogistics.app.delivery.adapters.inbound.web.dto.request;

import com.transportlogistics.app.delivery.domain.model.RedeliverySchedulingMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ScheduleRedeliveryRequest(
        @NotNull(message = "Expected version is required")
        Long expectedVersion,

        UUID failedAttemptId,

        @NotNull(message = "Scheduling method is required")
        RedeliverySchedulingMethod schedulingMethod,

        OffsetDateTime preferredStartTime,

        OffsetDateTime preferredEndTime,

        @Size(max = 500, message = "Customer preference notes must not exceed 500 characters")
        String customerPreferenceNotes,

        @NotNull(message = "Scheduled start time is required")
        OffsetDateTime scheduledStartTime,

        @NotNull(message = "Scheduled end time is required")
        OffsetDateTime scheduledEndTime
) {
}
