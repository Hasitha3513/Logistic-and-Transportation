package com.transportlogistics.app.fleet.domain.model;

import com.transportlogistics.app.shared.domain.BusinessRuleException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.UUID;

public record VehicleReading(UUID id, UUID vehicleId, VehicleReadingType readingType, BigDecimal value,
                             VehicleReadingUnit unit, int meterEpoch, VehicleReadingSourceType sourceType,
                             UUID sourceReferenceId, OffsetDateTime recordedAt, OffsetDateTime receivedAt,
                             UUID createdBy, UUID correctionOfReadingId, String correctionReason,
                             String idempotencyKey, String notes, OffsetDateTime createdAt) {
    public VehicleReading {
        required(id, "Reading id is required");
        required(vehicleId, "Vehicle id is required");
        required(readingType, "Reading type is required");
        required(value, "Reading value is required");
        required(unit, "Reading unit is required");
        required(sourceType, "Reading source type is required");
        required(recordedAt, "Recorded time is required");
        required(receivedAt, "Received time is required");
        required(createdBy, "Created-by user is required");
        required(createdAt, "Created time is required");
        if (value.signum() < 0) invalid("Reading value cannot be negative");
        try {
            value = value.setScale(3, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            invalid("Reading value must have no more than three decimal places");
        }
        if (value.precision() > 19) invalid("Reading value exceeds NUMERIC(19,3)");
        if (unit != readingType.unit()) invalid("Reading unit must match reading type");
        if (meterEpoch < 0) invalid("Meter epoch cannot be negative");
        notes = trim(notes);
        if (notes != null && notes.length() > 1000) invalid("Reading notes cannot exceed 1000 characters");
        idempotencyKey = trim(idempotencyKey);
        correctionReason = trim(correctionReason);
        if ((correctionOfReadingId == null) != (correctionReason == null)) {
            invalid("Correction reference and reason must be supplied together");
        }
    }

    private static void required(Object value, String message) {
        if (value == null) invalid(message);
    }

    private static String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static void invalid(String message) {
        throw new BusinessRuleException("INVALID_VEHICLE_READING", message);
    }
}
