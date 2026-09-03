package com.transportlogistics.app.delivery.adapters.inbound.web.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DeliveryBatchResponse(
        UUID id,
        String batchCode,
        UUID deliveryZoneId,
        UUID deliverySlotId,
        UUID riderId,
        String status,
        int maxBatchSize,
        long version,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        String createdBy,
        String updatedBy
) {}
