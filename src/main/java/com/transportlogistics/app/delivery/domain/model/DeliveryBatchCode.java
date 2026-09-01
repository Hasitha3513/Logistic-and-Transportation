package com.transportlogistics.app.delivery.domain.model;

public record DeliveryBatchCode(String value) {
    public DeliveryBatchCode {
        value = value == null ? null : value.trim();
        if (value == null || !value.matches("BAT-[0-9]{4}-[0-9]{6}")) {
            throw new IllegalArgumentException("Batch code must match BAT-YYYY-NNNNNN");
        }
    }
}
