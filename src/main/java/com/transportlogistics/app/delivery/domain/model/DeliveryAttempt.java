package com.transportlogistics.app.delivery.domain.model;

import com.transportlogistics.app.shared.domain.BusinessRuleException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record DeliveryAttempt(
        UUID id,
        DeliveryId deliveryId,
        int attemptNumber,
        OffsetDateTime attemptTimestamp,
        DeliveryFailureReason failureReason,
        String notes,
        DeliveryFailureDisposition disposition,
        List<DeliveryContactAttempt> contactAttempts,
        String recordedBy,
        OffsetDateTime recordedAt
) {
    public DeliveryAttempt {
        if (id == null || deliveryId == null || attemptNumber <= 0 || attemptTimestamp == null
                || failureReason == null || disposition == null || recordedBy == null || recordedBy.isBlank()
                || recordedAt == null) {
            throw new BusinessRuleException("INVALID_DELIVERY_ATTEMPT", "Required delivery attempt data is missing");
        }
        notes = notes == null || notes.isBlank() ? null : notes.trim();
        failureReason.validateNotes(notes);
        recordedBy = recordedBy.trim();
        contactAttempts = contactAttempts == null ? List.of() : List.copyOf(contactAttempts);
    }

    public static DeliveryAttempt create(UUID id, DeliveryId deliveryId, int attemptNumber,
                                         OffsetDateTime attemptTimestamp, DeliveryFailureReason failureReason,
                                         String notes, DeliveryFailureDisposition requestedDisposition,
                                         List<DeliveryContactAttempt> contactAttempts, String actor, OffsetDateTime now) {
        DeliveryFailureDisposition resolvedDisposition = failureReason.resolveDisposition(requestedDisposition);
        return new DeliveryAttempt(id, deliveryId, attemptNumber,
                attemptTimestamp == null ? now : attemptTimestamp,
                failureReason, notes, resolvedDisposition, contactAttempts, actor, now);
    }
}
