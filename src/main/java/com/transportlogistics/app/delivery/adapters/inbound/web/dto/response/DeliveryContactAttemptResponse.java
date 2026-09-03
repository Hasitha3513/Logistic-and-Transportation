package com.transportlogistics.app.delivery.adapters.inbound.web.dto.response;

import com.transportlogistics.app.delivery.domain.model.DeliveryContactAttempt;
import com.transportlogistics.app.delivery.domain.model.DeliveryContactChannel;
import com.transportlogistics.app.delivery.domain.model.DeliveryContactOutcome;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DeliveryContactAttemptResponse(
        UUID id,
        UUID deliveryAttemptId,
        DeliveryContactChannel channel,
        OffsetDateTime contactTimestamp,
        DeliveryContactOutcome outcome,
        String notes,
        String recordedBy,
        OffsetDateTime recordedAt
) {
    public static DeliveryContactAttemptResponse from(DeliveryContactAttempt domain) {
        return new DeliveryContactAttemptResponse(
                domain.id(),
                domain.deliveryAttemptId(),
                domain.channel(),
                domain.contactTimestamp(),
                domain.outcome(),
                domain.notes(),
                domain.recordedBy(),
                domain.recordedAt()
        );
    }
}
