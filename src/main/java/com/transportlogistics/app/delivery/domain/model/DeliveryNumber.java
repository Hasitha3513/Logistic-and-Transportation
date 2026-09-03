package com.transportlogistics.app.delivery.domain.model;

public record DeliveryNumber(String value) {
    public DeliveryNumber {
        value = value == null ? null : value.trim();
        if (value == null || !value.matches("DEL-[0-9]{4}-[0-9]{6}"))
            throw new IllegalArgumentException("Delivery number must match DEL-YYYY-NNNNNN");
    }
}
