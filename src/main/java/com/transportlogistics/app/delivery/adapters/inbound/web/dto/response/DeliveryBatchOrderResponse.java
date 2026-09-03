package com.transportlogistics.app.delivery.adapters.inbound.web.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DeliveryBatchOrderResponse(
        UUID id,
        UUID batchId,
        UUID deliveryOrderId,
        Integer sequenceHint,
        String status,
        OffsetDateTime addedAt,
        String addedBy,
        OffsetDateTime removedAt,
        String removedBy
) {}
