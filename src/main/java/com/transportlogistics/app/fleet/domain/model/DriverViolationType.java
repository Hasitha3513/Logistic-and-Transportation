package com.transportlogistics.app.fleet.domain.model;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum DriverViolationType {
    SPEEDING,
    RED_LIGHT,
    RECKLESS_DRIVING,
    UNAUTHORIZED_STOP,
    LOGBOOK_VIOLATION,
    ACCIDENT_FAULT,
    OVERLOADING,
    LANE_DISCIPLINE,
    LANE_VIOLATION,
    ILLEGAL_PARKING,
    DOCUMENT_VIOLATION,
    OTHER;

    @JsonCreator
    public static DriverViolationType fromString(String value) {
        if (value == null || value.isBlank()) {
            return OTHER;
        }
        var normalized = value.trim().toUpperCase();
        if (normalized.contains("SPEED")) return SPEEDING;
        if (normalized.contains("RED_LIGHT") || normalized.contains("SIGNAL")) return RED_LIGHT;
        if (normalized.contains("RECKLESS")) return RECKLESS_DRIVING;
        if (normalized.contains("LANE")) return LANE_DISCIPLINE;
        if (normalized.contains("PARK")) return ILLEGAL_PARKING;
        if (normalized.contains("DOCUMENT")) return DOCUMENT_VIOLATION;
        if (normalized.contains("OVERLOAD")) return OVERLOADING;
        if (normalized.contains("UNAUTHORIZED")) return UNAUTHORIZED_STOP;
        if (normalized.contains("LOGBOOK") || normalized.contains("HOS")) return LOGBOOK_VIOLATION;
        if (normalized.contains("ACCIDENT") || normalized.contains("CRASH")) return ACCIDENT_FAULT;
        try {
            return DriverViolationType.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            return OTHER;
        }
    }
}
