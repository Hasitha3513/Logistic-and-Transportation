package com.transportlogistics.app.delivery.domain.model;

import com.transportlogistics.app.shared.domain.BusinessRuleException;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DeliveryEscalation(
        UUID id,
        DeliveryId deliveryId,
        UUID deliveryAttemptId,
        String reason,
        DeliveryEscalationStatus status,
        String resolutionNotes,
        String escalatedBy,
        OffsetDateTime escalatedAt,
        String resolvedBy,
        OffsetDateTime resolvedAt
) {
    public DeliveryEscalation {
        if (id == null || deliveryId == null || reason == null || reason.isBlank()
                || status == null || escalatedBy == null || escalatedBy.isBlank() || escalatedAt == null) {
            throw new BusinessRuleException("INVALID_DELIVERY_ESCALATION", "Required escalation data is missing");
        }
        reason = reason.trim();
        if (reason.length() > 500) {
            throw new BusinessRuleException("INVALID_DELIVERY_ESCALATION", "Escalation reason cannot exceed 500 characters");
        }
        resolutionNotes = resolutionNotes == null || resolutionNotes.isBlank() ? null : resolutionNotes.trim();
        escalatedBy = escalatedBy.trim();
        resolvedBy = resolvedBy == null || resolvedBy.isBlank() ? null : resolvedBy.trim();
    }

    public static DeliveryEscalation create(UUID id, DeliveryId deliveryId, UUID deliveryAttemptId,
                                            String reason, String actor, OffsetDateTime now) {
        return new DeliveryEscalation(id, deliveryId, deliveryAttemptId, reason,
                DeliveryEscalationStatus.OPEN, null, actor, now, null, null);
    }

    public DeliveryEscalation resolve(String resolutionNotes, String actor, OffsetDateTime now) {
        if (this.status == DeliveryEscalationStatus.RESOLVED) {
            throw new BusinessRuleException("ALREADY_RESOLVED", "Escalation is already resolved");
        }
        return new DeliveryEscalation(id, deliveryId, deliveryAttemptId, reason,
                DeliveryEscalationStatus.RESOLVED, resolutionNotes, escalatedBy, escalatedAt, actor, now);
    }

    public DeliveryEscalation underReview(String actor, OffsetDateTime now) {
        return new DeliveryEscalation(id, deliveryId, deliveryAttemptId, reason,
                DeliveryEscalationStatus.UNDER_REVIEW, resolutionNotes, escalatedBy, escalatedAt, resolvedBy, resolvedAt);
    }
}
