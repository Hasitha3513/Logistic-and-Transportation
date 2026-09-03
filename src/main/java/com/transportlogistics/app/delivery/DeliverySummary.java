package com.transportlogistics.app.delivery;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Public summary shape reserved for Delivery-owned read models. */
public record DeliverySummary(UUID deliveryId, String deliveryNumber, String status,
                              UUID customerId, UUID destinationLocationId,
                              OffsetDateTime plannedDeliveryFrom, OffsetDateTime plannedDeliveryTo) {
    public DeliverySummary {
        if (deliveryId == null) {
            throw new IllegalArgumentException("deliveryId is required");
        }
        if (deliveryNumber == null || deliveryNumber.isBlank()) {
            throw new IllegalArgumentException("deliveryNumber is required");
        }
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("status is required");
        }
    }
}
