package com.transportlogistics.app.delivery.adapters.inbound.web.dto.response;

import com.transportlogistics.app.delivery.domain.model.DeliveryOrderRiderAssignment;
import com.transportlogistics.app.delivery.domain.model.DeliveryRiderAssignmentStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DeliveryOrderRiderAssignmentResponse(
        UUID id,
        UUID deliveryOrderId,
        UUID riderId,
        DeliveryRiderAssignmentStatus status,
        OffsetDateTime assignedAt,
        String assignedBy,
        OffsetDateTime unassignedAt,
        String unassignedBy,
        boolean isOverride,
        String overrideReason,
        long version
) {
    public static DeliveryOrderRiderAssignmentResponse from(DeliveryOrderRiderAssignment domain) {
        return new DeliveryOrderRiderAssignmentResponse(
                domain.getId(),
                domain.getDeliveryOrderId(),
                domain.getRiderId(),
                domain.getStatus(),
                domain.getAssignedAt(),
                domain.getAssignedBy(),
                domain.getUnassignedAt(),
                domain.getUnassignedBy(),
                domain.isOverride(),
                domain.getOverrideReason(),
                domain.getVersion()
        );
    }
}
