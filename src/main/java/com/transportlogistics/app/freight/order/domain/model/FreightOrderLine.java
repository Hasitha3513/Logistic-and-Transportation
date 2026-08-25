package com.transportlogistics.app.freight.order.domain.model;

import com.transportlogistics.app.shared.domain.BusinessRuleException;

import java.math.BigDecimal;
import java.util.UUID;

public record FreightOrderLine(UUID id, String description, BigDecimal quantity) {
    public FreightOrderLine {
        if (id == null) throw invalid("Shipment line id is required");
        description = normalize(description);
        if (description == null) throw invalid("Shipment line description is required");
        if (description.length() > 500) throw invalid("Shipment line description must not exceed 500 characters");
        if (quantity == null || quantity.signum() <= 0) throw invalid("Shipment line quantity must be greater than zero");
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static BusinessRuleException invalid(String message) {
        return new BusinessRuleException("INVALID_FREIGHT_ORDER_LINE", message);
    }
}
