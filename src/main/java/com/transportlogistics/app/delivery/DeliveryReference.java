package com.transportlogistics.app.delivery;

import java.util.UUID;

/** Minimal public Delivery reference; does not expose persistence entities. */
public record DeliveryReference(UUID deliveryId, String deliveryNumber, String status) {
    public DeliveryReference {
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
