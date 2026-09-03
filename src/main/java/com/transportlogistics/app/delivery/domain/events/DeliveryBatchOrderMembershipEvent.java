package com.transportlogistics.app.delivery.domain.events;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DeliveryBatchOrderMembershipEvent(
        UUID eventId,
        UUID tenantId,
        UUID batchId,
        UUID deliveryOrderId,
        String action, // ADDED, REMOVED, COMPLETED
        OffsetDateTime timestamp,
        String actor
) {}
