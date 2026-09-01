package com.transportlogistics.app.delivery.domain.events;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DeliveryBatchCreatedEvent(
        UUID eventId,
        UUID tenantId,
        UUID batchId,
        String batchCode,
        UUID deliveryZoneId,
        UUID deliverySlotId,
        OffsetDateTime timestamp,
        String actor
) {}
