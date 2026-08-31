package com.transportlogistics.app.delivery.adapters.inbound.web.dto.response;

import com.transportlogistics.app.delivery.domain.model.DeliveryRider;
import com.transportlogistics.app.delivery.domain.model.DeliveryRiderStatus;
import com.transportlogistics.app.delivery.domain.model.DeliveryRiderType;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

public record DeliveryRiderResponse(
        UUID id,
        String riderCode,
        UUID driverId,
        DeliveryRiderType riderType,
        DeliveryRiderStatus status,
        UUID primaryZoneId,
        Set<UUID> secondaryZoneIds,
        int maxConcurrentDeliveries,
        long version,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        String createdBy,
        String updatedBy
) {
    public static DeliveryRiderResponse from(DeliveryRider domain) {
        return new DeliveryRiderResponse(
                domain.getId(),
                domain.getRiderCode(),
                domain.getDriverId(),
                domain.getRiderType(),
                domain.getStatus(),
                domain.getPrimaryZoneId(),
                domain.getSecondaryZoneIds(),
                domain.getMaxConcurrentDeliveries(),
                domain.getVersion(),
                domain.getCreatedAt(),
                domain.getUpdatedAt(),
                domain.getCreatedBy(),
                domain.getUpdatedBy()
        );
    }
}
