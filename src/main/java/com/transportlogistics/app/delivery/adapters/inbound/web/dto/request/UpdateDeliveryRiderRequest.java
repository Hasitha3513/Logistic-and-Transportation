package com.transportlogistics.app.delivery.adapters.inbound.web.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.Set;
import java.util.UUID;

public record UpdateDeliveryRiderRequest(
        @NotNull UUID primaryZoneId,
        Set<UUID> secondaryZoneIds,
        Integer maxConcurrentDeliveries
) {
}
