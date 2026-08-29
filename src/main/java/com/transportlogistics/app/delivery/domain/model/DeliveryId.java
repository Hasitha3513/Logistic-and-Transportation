package com.transportlogistics.app.delivery.domain.model;

import java.util.UUID;

public record DeliveryId(UUID value) {
    public DeliveryId {
        if (value == null) {
            throw new IllegalArgumentException("Delivery id is required");
        }
    }
}
