package com.transportlogistics.app.delivery.domain.events;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DeliveryOrderDestinationChangedEvent(
        UUID tenantId,
        UUID deliveryOrderId,
        UUID previousDestinationLocationId,
        UUID newDestinationLocationId,
        OffsetDateTime changedAt,
        String actor
) {
}
