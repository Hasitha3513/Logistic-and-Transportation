package com.transportlogistics.app.delivery.adapters.inbound.web.dto.request;

import com.transportlogistics.app.delivery.domain.model.DeliveryRiderType;
import jakarta.validation.constraints.NotNull;

import java.util.Set;
import java.util.UUID;

public record OnboardDeliveryRiderRequest(
        String riderCode,
        @NotNull UUID driverId,
        DeliveryRiderType riderType,
        @NotNull UUID primaryZoneId,
        Set<UUID> secondaryZoneIds,
        Integer maxConcurrentDeliveries
) {
}
