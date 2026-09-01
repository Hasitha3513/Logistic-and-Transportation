package com.transportlogistics.app.delivery.domain.events;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DeliveryBatchStatusChangedEvent(
        UUID eventId,
        UUID tenantId,
        UUID batchId,
        String oldStatus,
        String newStatus,
        OffsetDateTime timestamp,
        String actor
) {}
