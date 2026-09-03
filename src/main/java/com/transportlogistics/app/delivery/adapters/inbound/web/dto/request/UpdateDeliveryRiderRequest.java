package com.transportlogistics.app.delivery.adapters.inbound.web.dto.request;

import com.transportlogistics.app.delivery.domain.model.DeliveryTransportMode;
import jakarta.validation.constraints.NotNull;

import java.util.Set;
import java.util.UUID;

public record UpdateDeliveryRiderRequest(
        @NotNull UUID primaryZoneId,
        @NotNull DeliveryTransportMode transportMode,
        Set<UUID> secondaryZoneIds,
        Integer maxConcurrentDeliveries
) {
}
