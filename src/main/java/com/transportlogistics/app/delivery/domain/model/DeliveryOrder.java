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

    private static String normalize(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private static String actor(String value) {
        if (value == null || value.isBlank()) throw invalid("Authenticated actor is required");
        return value.trim();
    }
    private static BusinessRuleException invalid(String message) {
        return new BusinessRuleException("INVALID_DELIVERY_ORDER", message);
    }
}
