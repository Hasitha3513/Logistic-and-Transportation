package com.transportlogistics.app.delivery.domain.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public final class DeliveryRiderEvents {

    private DeliveryRiderEvents() {}

    public record DeliveryRiderCreatedEvent(
            UUID tenantId,
            UUID riderId,
            UUID driverId,
            String riderCode,
            UUID primaryZoneId,
            DeliveryRiderType riderType,
            OffsetDateTime createdAt,
            String actor
    ) {}

    public record DeliveryRiderStatusChangedEvent(
            UUID tenantId,
            UUID riderId,
            DeliveryRiderStatus previousStatus,
            DeliveryRiderStatus newStatus,
            OffsetDateTime updatedAt,
            String actor
    ) {}

    public record DeliveryRiderAssignedEvent(
            UUID tenantId,
            UUID deliveryOrderId,
            UUID riderId,
            UUID assignmentId,
            boolean isOverride,
            OffsetDateTime assignedAt,
            String actor
    ) {}

    public record DeliveryRiderReassignedEvent(
            UUID tenantId,
            UUID deliveryOrderId,
            UUID previousRiderId,
            UUID newRiderId,
            UUID assignmentId,
            OffsetDateTime reassignedAt,
            String actor
    ) {}

    public record DeliveryRiderUnassignedEvent(
            UUID tenantId,
            UUID deliveryOrderId,
            UUID riderId,
            OffsetDateTime unassignedAt,
            String actor
    ) {}
}
