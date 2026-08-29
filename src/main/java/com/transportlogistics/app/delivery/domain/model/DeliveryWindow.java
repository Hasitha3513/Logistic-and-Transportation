package com.transportlogistics.app.delivery.domain.model;

import com.transportlogistics.app.shared.domain.BusinessRuleException;

import java.time.OffsetDateTime;

public record DeliveryWindow(OffsetDateTime start, OffsetDateTime end) {
    public DeliveryWindow {
        if (start == null || end == null) {
            throw new BusinessRuleException("DELIVERY_WINDOW_REQUIRED", "Delivery window start and end are required");
        }
        if (end.isBefore(start)) {
            throw new BusinessRuleException("INVALID_DELIVERY_WINDOW", "Delivery window end must not precede its start");
        }
    }
}
