package com.transportlogistics.app.fleet.payroll.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

public record DriverPayrollInputLine(UUID id, UUID driverId, UUID tripId, String tripNumber, Category category,
                                     String reasonCode, String description, BigDecimal quantity, Unit unit,
                                     BigDecimal rate, BigDecimal amount, UUID originalLineId,
                                     String sourceSnapshotHash, String externalWorkerReference) {
    public DriverPayrollInputLine {
        if (id == null || driverId == null || tripId == null || category == null || unit == null) invalid();
        reasonCode = required(reasonCode, 80);
        description = required(description, 500);
        tripNumber = required(tripNumber, 80);
        quantity = nonNegative(quantity, "quantity");
        rate = nonNegative(rate, "rate").setScale(2, RoundingMode.HALF_UP);
        BigDecimal calculated = quantity.multiply(rate).setScale(2, RoundingMode.HALF_UP);
        if ("FIXED_AMOUNT".equals(reasonCode)) {
            amount = nonNegative(amount, "amount").setScale(2, RoundingMode.HALF_UP);
            if (unit != Unit.FIXED) invalid();
        } else {
            amount = calculated;
        }
    }

    public DriverPayrollInputLine snapshot(String hash, String workerReference) {
        return new DriverPayrollInputLine(id, driverId, tripId, tripNumber, category, reasonCode, description,
            quantity, unit, rate, amount, originalLineId, required(hash, 64), required(workerReference, 160));
    }

    private static String required(String value, int max) {
        if (value == null || value.isBlank() || value.trim().length() > max) invalid();
        return value.trim();
    }

    private static BigDecimal nonNegative(BigDecimal value, String field) {
        if (value == null || value.signum() < 0) throw new IllegalArgumentException(field + " must be non-negative");
        return value;
    }

    private static void invalid() { throw new IllegalArgumentException("DRIVER_PAYROLL_VALIDATION_FAILED"); }
    public enum Category { TRIP_EARNING, ALLOWANCE, OVERTIME, DEDUCTION }
    public enum Unit { TRIP, HOUR, FIXED }
}
