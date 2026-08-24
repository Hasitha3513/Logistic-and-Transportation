package com.transportlogistics.app.fleet.domain.model;

import java.util.List;

public record VehicleAvailability(boolean available, List<Reason> reasons) {
    public VehicleAvailability {
        reasons = reasons == null ? List.of() : List.copyOf(reasons);
        if (available && !reasons.isEmpty()) {
            throw new IllegalArgumentException("An available vehicle cannot have rejection reasons");
        }
        if (!available && reasons.isEmpty()) {
            throw new IllegalArgumentException("An unavailable vehicle must have at least one rejection reason");
        }
    }

    public static VehicleAvailability eligible() {
        return new VehicleAvailability(true, List.of());
    }

    public static VehicleAvailability from(List<Reason> reasons) {
        return reasons.isEmpty() ? eligible() : new VehicleAvailability(false, reasons);
    }

    public boolean hasReason(Code code) {
        return reasons.stream().anyMatch(reason -> reason.code() == code);
    }

    public enum Code {
        INACTIVE,
        OPERATIONALLY_UNAVAILABLE,
        BROKEN_DOWN,
        OUT_OF_SERVICE,
        MAINTENANCE_BLOCKED,
        MANDATORY_DOCUMENT_INVALID,
        MANDATORY_DOCUMENT_EXPIRED,
        VEHICLE_TYPE_MISMATCH,
        INSUFFICIENT_CAPACITY,
        OVERLAPPING_ALLOCATION
    }

    public record Reason(Code code, String message) {
    }
}
