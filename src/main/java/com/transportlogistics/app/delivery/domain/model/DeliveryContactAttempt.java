package com.transportlogistics.app.delivery.domain.model;

import com.transportlogistics.app.shared.domain.BusinessRuleException;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DeliveryContactAttempt(
        UUID id,
        UUID deliveryAttemptId,
        DeliveryContactChannel channel,
        OffsetDateTime contactTimestamp,
        DeliveryContactOutcome outcome,
        String notes,
        String recordedBy,
        OffsetDateTime recordedAt
) {
    public DeliveryContactAttempt {
        if (id == null || channel == null || contactTimestamp == null || outcome == null
                || recordedBy == null || recordedBy.isBlank() || recordedAt == null) {
            throw new BusinessRuleException("INVALID_CONTACT_ATTEMPT", "Required contact attempt data is missing");
        }
        notes = notes == null || notes.isBlank() ? null : notes.trim();
        if (notes != null && notes.length() > 500) {
            throw new BusinessRuleException("INVALID_CONTACT_ATTEMPT", "Contact attempt notes cannot exceed 500 characters");
        }
        recordedBy = recordedBy.trim();
    }

    public static DeliveryContactAttempt create(UUID id, UUID deliveryAttemptId, DeliveryContactChannel channel,
                                               OffsetDateTime contactTimestamp, DeliveryContactOutcome outcome,
                                               String notes, String actor, OffsetDateTime now) {
        return new DeliveryContactAttempt(id, deliveryAttemptId, channel,
                contactTimestamp == null ? now : contactTimestamp, outcome, notes, actor, now);
    }
}
