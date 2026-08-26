package com.transportlogistics.app.fleet.domain.model;

import java.util.List;

public record DriverAvailability(boolean available, List<Reason> reasons) {
    public DriverAvailability {
        reasons = reasons == null ? List.of() : List.copyOf(reasons);
        if (available && !reasons.isEmpty()) {
            throw new IllegalArgumentException("An available driver cannot have rejection reasons");
        }
        if (!available && reasons.isEmpty()) {
            throw new IllegalArgumentException("An unavailable driver must have at least one rejection reason");
        }
    }

    public static DriverAvailability eligible() {
        return new DriverAvailability(true, List.of());
    }

    public static DriverAvailability from(List<Reason> reasons) {
        return reasons.isEmpty() ? eligible() : new DriverAvailability(false, reasons);
    }

    public boolean hasReason(Code code) {
        return reasons.stream().anyMatch(reason -> reason.code() == code);
    }

    public enum Code {
        INACTIVE,
        OPERATIONALLY_UNAVAILABLE,
        LICENSE_MISSING,
        LICENSE_NOT_YET_VALID,
        LICENSE_EXPIRED,
        REQUIRED_LICENSE_CLASS_MISSING,
        OVERLAPPING_ASSIGNMENT,
        DRIVER_EXCEPTION_BLOCKED,
        MEDICAL_CLEARANCE_MISSING,
        MEDICAL_FITNESS_EXPIRED,
        MEDICALLY_UNFIT,
        DRUG_TEST_FAILED,
        RETURN_TO_DUTY_CLEARANCE_REQUIRED
    }

    public record Reason(Code code, String message) {
    }
}
