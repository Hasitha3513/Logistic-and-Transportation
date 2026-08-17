package com.transportlogistics.app.fleet.domain.model;

import com.transportlogistics.app.shared.domain.BusinessRuleException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.UUID;

public record VehicleMeterReset(UUID id, UUID vehicleId, VehicleReadingType readingType,
                                UUID previousReadingId, BigDecimal previousMeterValue,
                                UUID newReadingId, BigDecimal newMeterValue,
                                OffsetDateTime effectiveAt, String reason,
                                UUID createdBy, UUID approvedBy, String notes,
                                OffsetDateTime createdAt) {
    public VehicleMeterReset {
        required(id, "Reset id is required");
        required(vehicleId, "Vehicle id is required");
        required(readingType, "Reading type is required");
        required(previousMeterValue, "Previous meter value is required");
        required(newReadingId, "New reading id is required");
        required(newMeterValue, "New meter value is required");
        required(effectiveAt, "Effective time is required");
        required(createdBy, "Created-by user is required");
        required(createdAt, "Created time is required");
        if (previousMeterValue.signum() < 0) invalid("Previous meter value cannot be negative");
        if (newMeterValue.signum() < 0) invalid("New meter value cannot be negative");
        try {
            previousMeterValue = previousMeterValue.setScale(3, RoundingMode.UNNECESSARY);
            newMeterValue = newMeterValue.setScale(3, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            invalid("Meter values must have no more than three decimal places");
        }
        if (previousMeterValue.precision() > 19 || newMeterValue.precision() > 19) {
            invalid("Meter value exceeds NUMERIC(19,3)");
        }
        reason = trim(reason);
        if (reason == null) invalid("Reset reason is required");
        notes = trim(notes);
        if (notes != null && notes.length() > 1000) invalid("Notes cannot exceed 1000 characters");
    }

    private static void required(Object value, String message) {
        if (value == null) invalid(message);
    }

    private static String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static void invalid(String message) {
        throw new BusinessRuleException("INVALID_METER_RESET", message);
    }
}
