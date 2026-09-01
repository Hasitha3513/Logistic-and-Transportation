package com.transportlogistics.app.delivery.domain.events;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DeliveryBatchRiderAssignedEvent(
        UUID eventId,
        UUID tenantId,
        UUID batchId,
        UUID riderId,
        int orderCount,
        OffsetDateTime timestamp,
        String actor
) {}
