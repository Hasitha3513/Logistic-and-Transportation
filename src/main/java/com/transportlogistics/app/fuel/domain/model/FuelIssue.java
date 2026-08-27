package com.transportlogistics.app.fuel.domain.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record FuelIssue(UUID id, String voucherNumber, UUID vehicleId, UUID tripId, UUID driverId, String fuelType,
                        BigDecimal quantity, BigDecimal unitPrice, BigDecimal totalAmount, UUID stationId,
                        BigDecimal odometer, BigDecimal engineHours, OffsetDateTime issueDateTime,
                        FuelIssueStatus status, UUID requestedBy, UUID authorizedBy,
                        OffsetDateTime authorizationDateTime, String notes, OffsetDateTime createdAt,
                        OffsetDateTime updatedAt) {
    public static BigDecimal total(BigDecimal quantity, BigDecimal unitPrice) {
        return unitPrice == null || quantity == null ? null : quantity.multiply(unitPrice);
    }
}
