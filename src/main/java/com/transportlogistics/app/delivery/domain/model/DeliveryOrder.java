package com.transportlogistics.app.delivery.domain.model;

import com.transportlogistics.app.shared.domain.BusinessRuleException;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DeliveryOrder(
        DeliveryId id,
        DeliveryNumber deliveryNumber,
        UUID customerId,
        UUID originLocationId,
        UUID destinationLocationId,
        DeliveryPriority priority,
        DeliveryServiceType serviceType,
        DeliveryWindow window,
        String instructions,
        DeliveryStatus status,
        long version,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        String createdBy,
        String updatedBy
) {
    public DeliveryOrder {
        if (id == null || deliveryNumber == null || customerId == null || originLocationId == null
                || destinationLocationId == null || priority == null || serviceType == null || window == null
                || status == null || createdAt == null || updatedAt == null) {
            throw invalid("Required Delivery Order data is missing");
        }
        if (originLocationId.equals(destinationLocationId)) throw invalid("Origin and destination must be different");
        if (version < 0) throw invalid("Version must not be negative");
        instructions = instructions == null || instructions.isBlank() ? null : instructions.trim();
        createdBy = actor(createdBy);
        updatedBy = actor(updatedBy);
    }

    public static DeliveryOrder create(DeliveryId id, DeliveryNumber number, UUID customerId, UUID originId,
                                       UUID destinationId, DeliveryPriority priority, DeliveryServiceType serviceType,
                                       DeliveryWindow window, String instructions, OffsetDateTime now, String actor) {
        return new DeliveryOrder(id, number, customerId, originId, destinationId,
                priority == null ? DeliveryPriority.NORMAL : priority,
                serviceType == null ? DeliveryServiceType.STANDARD : serviceType,
                window, instructions, DeliveryStatus.DRAFT, 0, now, now, actor, actor);
    }

    public DeliveryOrder updateRequirements(UUID customerId, UUID originId, UUID destinationId,
                                            DeliveryPriority priority, DeliveryServiceType serviceType,
                                            DeliveryWindow window, String instructions, OffsetDateTime now, String actor) {
        boolean material = !this.customerId.equals(customerId) || !this.originLocationId.equals(originId)
                || !this.destinationLocationId.equals(destinationId) || this.priority != priority
                || this.serviceType != serviceType || !this.window.equals(window)
                || !java.util.Objects.equals(this.instructions, normalize(instructions));
        DeliveryStatus next = material && status == DeliveryStatus.READY_FOR_ASSIGNMENT ? DeliveryStatus.DRAFT : status;
        return new DeliveryOrder(id, deliveryNumber, customerId, originId, destinationId, priority, serviceType,
                window, instructions, next, version, createdAt, now, createdBy, actor);
    }

    public DeliveryOrder markReadyForAssignment(OffsetDateTime now, String actor) {
        if (status != DeliveryStatus.DRAFT) {
            throw new BusinessRuleException("INVALID_DELIVERY_TRANSITION", "Only a DRAFT Delivery Order can be marked ready");
        }
        return new DeliveryOrder(id, deliveryNumber, customerId, originLocationId, destinationLocationId, priority,
                serviceType, window, instructions, DeliveryStatus.READY_FOR_ASSIGNMENT, version, createdAt, now,
                createdBy, actor);
    }

    public DeliveryOrder markDelivered(OffsetDateTime now, String actor) {
        if (status != DeliveryStatus.READY_FOR_ASSIGNMENT) {
            throw new BusinessRuleException("INVALID_DELIVERY_TRANSITION", "Only a READY_FOR_ASSIGNMENT Delivery Order can be completed");
        }
        return new DeliveryOrder(id, deliveryNumber, customerId, originLocationId, destinationLocationId, priority,
                serviceType, window, instructions, DeliveryStatus.DELIVERED, version, createdAt, now, createdBy, actor);
    }

    public DeliveryOrder recordFailedAttempt(DeliveryFailureDisposition disposition, OffsetDateTime now, String actor) {
        if (status != DeliveryStatus.READY_FOR_ASSIGNMENT && status != DeliveryStatus.FAILED_ATTEMPT) {
            throw new BusinessRuleException("INVALID_DELIVERY_TRANSITION",
                    "A failed attempt can only be recorded when the delivery is in READY_FOR_ASSIGNMENT or FAILED_ATTEMPT status");
        }
        DeliveryStatus nextStatus;
        if (disposition == DeliveryFailureDisposition.RETURN_TO_BASE_REQUIRED) {
            nextStatus = DeliveryStatus.RETURN_TO_BASE;
        } else if (disposition == DeliveryFailureDisposition.ESCALATED) {
            nextStatus = DeliveryStatus.ESCALATED;
        } else {
            nextStatus = DeliveryStatus.FAILED_ATTEMPT;
        }
        return new DeliveryOrder(id, deliveryNumber, customerId, originLocationId, destinationLocationId, priority,
                serviceType, window, instructions, nextStatus, version, createdAt, now, createdBy, actor);
    }

    public DeliveryOrder initiateReturnToBase(OffsetDateTime now, String actor) {
        if (status == DeliveryStatus.DELIVERED || status == DeliveryStatus.DRAFT) {
            throw new BusinessRuleException("INVALID_DELIVERY_TRANSITION",
                    "Return to base cannot be initiated for delivery in status " + status);
        }
        return new DeliveryOrder(id, deliveryNumber, customerId, originLocationId, destinationLocationId, priority,
                serviceType, window, instructions, DeliveryStatus.RETURN_TO_BASE, version, createdAt, now, createdBy, actor);
    }

    public DeliveryOrder escalate(OffsetDateTime now, String actor) {
        if (status == DeliveryStatus.DELIVERED || status == DeliveryStatus.DRAFT) {
            throw new BusinessRuleException("INVALID_DELIVERY_TRANSITION",
                    "Escalation cannot be initiated for delivery in status " + status);
        }
        return new DeliveryOrder(id, deliveryNumber, customerId, originLocationId, destinationLocationId, priority,
                serviceType, window, instructions, DeliveryStatus.ESCALATED, version, createdAt, now, createdBy, actor);
    }

    public DeliveryOrder resolveEscalation(DeliveryFailureDisposition nextDisposition, OffsetDateTime now, String actor) {
        if (status != DeliveryStatus.ESCALATED) {
            throw new BusinessRuleException("INVALID_DELIVERY_TRANSITION", "Only an ESCALATED delivery can be resolved");
        }
        DeliveryStatus nextStatus = (nextDisposition == DeliveryFailureDisposition.RETURN_TO_BASE_REQUIRED)
                ? DeliveryStatus.RETURN_TO_BASE
                : DeliveryStatus.FAILED_ATTEMPT;
        return new DeliveryOrder(id, deliveryNumber, customerId, originLocationId, destinationLocationId, priority,
                serviceType, window, instructions, nextStatus, version, createdAt, now, createdBy, actor);
    }

    private static String normalize(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private static String actor(String value) {
        if (value == null || value.isBlank()) throw invalid("Authenticated actor is required");
        return value.trim();
    }
    private static BusinessRuleException invalid(String message) {
        return new BusinessRuleException("INVALID_DELIVERY_ORDER", message);
    }
}
