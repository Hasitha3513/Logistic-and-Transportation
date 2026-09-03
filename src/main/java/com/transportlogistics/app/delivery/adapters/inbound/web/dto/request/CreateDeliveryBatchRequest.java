package com.transportlogistics.app.delivery.adapters.inbound.web.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record CreateDeliveryBatchRequest(
        @NotNull(message = "Delivery zone ID is required")
        UUID deliveryZoneId,
        UUID deliverySlotId,
        Integer maxBatchSize,
        List<UUID> deliveryOrderIds,
        UUID riderId
) {}
