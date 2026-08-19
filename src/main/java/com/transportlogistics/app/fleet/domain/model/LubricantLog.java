package com.transportlogistics.app.fleet.domain.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

public record LubricantLog(
        UUID id,
        UUID vehicleId,
        FluidType fluidType,
        BigDecimal quantity,
        MeasurementUnit unit,
        OffsetDateTime recordedAt,
        Double odometerKm,
        Double engineHours,
        UUID vendorId,
        String supplierName,
        String referenceNumber,
        String remarks,
        boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        String createdBy,
        String updatedBy
) {
    public LubricantLog {
        Objects.requireNonNull(id, "Lubricant log ID cannot be null");
        Objects.requireNonNull(vehicleId, "Vehicle ID cannot be null");
        Objects.requireNonNull(fluidType, "Fluid type cannot be null");
        Objects.requireNonNull(quantity, "Quantity cannot be null");
        Objects.requireNonNull(unit, "Measurement unit cannot be null");
        Objects.requireNonNull(recordedAt, "Recorded date/time cannot be null");

        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }
        if (odometerKm != null && odometerKm < 0) {
            throw new IllegalArgumentException("Odometer reading cannot be negative");
        }
        if (engineHours != null && engineHours < 0) {
            throw new IllegalArgumentException("Engine hours cannot be negative");
        }
        if (recordedAt.isAfter(OffsetDateTime.now().plusHours(24))) {
            throw new IllegalArgumentException("Recorded date/time cannot be in the future");
        }
    }
}
