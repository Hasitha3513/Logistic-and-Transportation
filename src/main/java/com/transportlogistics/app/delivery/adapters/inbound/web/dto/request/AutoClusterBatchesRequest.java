package com.transportlogistics.app.delivery.adapters.inbound.web.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AutoClusterBatchesRequest(
        @NotNull(message = "Delivery zone ID is required")
        UUID deliveryZoneId,
        UUID deliverySlotId,
        Integer maxBatchSize
) {}
