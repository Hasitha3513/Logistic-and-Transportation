package com.transportlogistics.app.freight.order.domain.model;

import com.transportlogistics.app.shared.domain.BusinessRuleException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public record FreightOrder(
        UUID id,
        String orderNumber,
        UUID customerId,
        UUID originLocationId,
        UUID destinationLocationId,
        OffsetDateTime requestedPickupAt,
        OffsetDateTime requestedDeliveryAt,
        String serviceLevel,
        String priority,
        String specialHandlingInstructions,
        List<FreightOrderLine> lines,
        long version,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        String createdBy,
        String updatedBy
) {
    public FreightOrder {
        if (id == null) throw invalid("Freight order id is required");
        orderNumber = requiredCode(orderNumber, "Order number", 60);
        if (customerId == null) throw invalid("Customer is required");
        if (originLocationId == null) throw invalid("Origin location is required");
        if (destinationLocationId == null) throw invalid("Destination location is required");
        if (originLocationId.equals(destinationLocationId)) throw invalid("Origin and destination must be different");
        if (requestedPickupAt == null) throw invalid("Requested pickup time is required");
        if (requestedDeliveryAt == null) throw invalid("Requested delivery time is required");
        if (requestedDeliveryAt.isBefore(requestedPickupAt)) throw invalid("Requested delivery time must not precede pickup time");
        serviceLevel = requiredCode(serviceLevel, "Service level", 60);
        priority = requiredCode(priority, "Priority", 40);
        specialHandlingInstructions = optional(specialHandlingInstructions, 2000, "Special handling instructions");
        lines = lines == null ? List.of() : List.copyOf(lines);
        if (lines.isEmpty()) throw invalid("At least one shipment line is required");
        if (version < 0) throw invalid("Version must not be negative");
        if (createdAt == null || updatedAt == null) throw invalid("Audit timestamps are required");
        createdBy = requiredText(createdBy, "Created by", 128);
        updatedBy = requiredText(updatedBy, "Updated by", 128);
    }

    private static String requiredCode(String value, String field, int max) {
        String normalized = requiredText(value, field, max).toUpperCase(Locale.ROOT);
        if (!normalized.matches("[A-Z0-9][A-Z0-9_-]*")) throw invalid(field + " must be an alphanumeric code");
        return normalized;
    }

    private static String requiredText(String value, String field, int max) {
        String normalized = value == null || value.isBlank() ? null : value.trim();
        if (normalized == null) throw invalid(field + " is required");
        if (normalized.length() > max) throw invalid(field + " must not exceed " + max + " characters");
        return normalized;
    }

    private static String optional(String value, int max, String field) {
        String normalized = value == null || value.isBlank() ? null : value.trim();
        if (normalized != null && normalized.length() > max) throw invalid(field + " must not exceed " + max + " characters");
        return normalized;
    }

    private static BusinessRuleException invalid(String message) {
        return new BusinessRuleException("INVALID_FREIGHT_ORDER", message);
    }
}
