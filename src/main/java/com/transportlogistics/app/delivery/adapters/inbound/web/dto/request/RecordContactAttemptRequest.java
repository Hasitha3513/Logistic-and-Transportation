package com.transportlogistics.app.delivery.adapters.inbound.web.dto.request;

import com.transportlogistics.app.delivery.domain.model.DeliveryContactChannel;
import com.transportlogistics.app.delivery.domain.model.DeliveryContactOutcome;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

public record RecordContactAttemptRequest(
        @NotNull(message = "channel is required")
        DeliveryContactChannel channel,

        OffsetDateTime contactTimestamp,

        @NotNull(message = "outcome is required")
        DeliveryContactOutcome outcome,

        @Size(max = 500, message = "notes cannot exceed 500 characters")
        String notes
) {}
