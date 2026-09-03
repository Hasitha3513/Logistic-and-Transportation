package com.transportlogistics.app.delivery.adapters.inbound.web.dto.response;

import com.transportlogistics.app.delivery.domain.model.DeliveryEscalation;
import com.transportlogistics.app.delivery.domain.model.DeliveryEscalationStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DeliveryEscalationResponse(
        UUID id,
        UUID deliveryId,
        UUID deliveryAttemptId,
        String reason,
        DeliveryEscalationStatus status,
        String resolutionNotes,
        String escalatedBy,
        OffsetDateTime escalatedAt,
        String resolvedBy,
        OffsetDateTime resolvedAt
) {
    public static DeliveryEscalationResponse from(DeliveryEscalation domain) {
        return new DeliveryEscalationResponse(
                domain.id(),
                domain.deliveryId().value(),
                domain.deliveryAttemptId(),
                domain.reason(),
                domain.status(),
                domain.resolutionNotes(),
                domain.escalatedBy(),
                domain.escalatedAt(),
                domain.resolvedBy(),
                domain.resolvedAt()
        );
    }
}
