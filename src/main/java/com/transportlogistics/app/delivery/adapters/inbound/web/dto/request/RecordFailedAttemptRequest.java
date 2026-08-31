package com.transportlogistics.app.delivery.adapters.inbound.web.dto.request;

import com.transportlogistics.app.delivery.domain.model.DeliveryContactChannel;
import com.transportlogistics.app.delivery.domain.model.DeliveryContactOutcome;
import com.transportlogistics.app.delivery.domain.model.DeliveryFailureDisposition;
import com.transportlogistics.app.delivery.domain.model.DeliveryFailureReason;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.List;

public record RecordFailedAttemptRequest(
        @NotNull(message = "expectedVersion is required")
        Long expectedVersion,

        @NotNull(message = "failureReason is required")
        DeliveryFailureReason failureReason,

        String notes,

        DeliveryFailureDisposition requestedDisposition,

        OffsetDateTime attemptTimestamp,

        @Valid
        List<ContactAttemptDto> contactAttempts
) {
    public record ContactAttemptDto(
            @NotNull(message = "channel is required")
            DeliveryContactChannel channel,

            OffsetDateTime contactTimestamp,

            @NotNull(message = "outcome is required")
            DeliveryContactOutcome outcome,

            String notes
    ) {}
}
