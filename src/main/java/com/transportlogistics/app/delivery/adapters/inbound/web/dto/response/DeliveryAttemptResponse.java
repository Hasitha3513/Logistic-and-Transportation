package com.transportlogistics.app.delivery.adapters.inbound.web.dto.response;

import com.transportlogistics.app.delivery.domain.model.DeliveryAttempt;
import com.transportlogistics.app.delivery.domain.model.DeliveryFailureDisposition;
import com.transportlogistics.app.delivery.domain.model.DeliveryFailureReason;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record DeliveryAttemptResponse(
        UUID id,
        UUID deliveryId,
        int attemptNumber,
        OffsetDateTime attemptTimestamp,
        DeliveryFailureReason failureReason,
        String notes,
        DeliveryFailureDisposition disposition,
        List<DeliveryContactAttemptResponse> contactAttempts,
        String recordedBy,
        OffsetDateTime recordedAt
) {
    public static DeliveryAttemptResponse from(DeliveryAttempt domain) {
        List<DeliveryContactAttemptResponse> contacts = domain.contactAttempts() == null
                ? List.of()
                : domain.contactAttempts().stream().map(DeliveryContactAttemptResponse::from).toList();
        return new DeliveryAttemptResponse(
                domain.id(),
                domain.deliveryId().value(),
                domain.attemptNumber(),
                domain.attemptTimestamp(),
                domain.failureReason(),
                domain.notes(),
                domain.disposition(),
                contacts,
                domain.recordedBy(),
                domain.recordedAt()
        );
    }
}
