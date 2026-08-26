package com.transportlogistics.app.fleet.domain.model;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum DriverExceptionType {
    LEAVE,
    DISCIPLINARY_SUSPENSION,
    MEDICAL_EMERGENCY,
    MEDICAL_LEAVE,
    ANNUAL_LEAVE,
    SICK_LEAVE,
    CASUAL_LEAVE,
    MATERNITY_LEAVE,
    SUSPENSION,
    OTHER;

    @JsonCreator
    public static DriverExceptionType fromString(String value) {
        if (value == null || value.isBlank()) {
            return OTHER;
        }
        var normalized = value.trim().toUpperCase();
        return switch (normalized) {
            case "MEDICAL_LEAVE", "MEDICAL" -> MEDICAL_LEAVE;
            case "ANNUAL_LEAVE", "ANNUAL" -> ANNUAL_LEAVE;
            case "SICK_LEAVE", "SICK" -> SICK_LEAVE;
            case "CASUAL_LEAVE", "CASUAL" -> CASUAL_LEAVE;
            case "MATERNITY_LEAVE" -> MATERNITY_LEAVE;
            case "LEAVE" -> LEAVE;
            case "DISCIPLINARY_SUSPENSION", "SUSPENSION", "DISCIPLINARY" -> DISCIPLINARY_SUSPENSION;
            case "MEDICAL_EMERGENCY" -> MEDICAL_EMERGENCY;
            default -> {
                try {
                    yield DriverExceptionType.valueOf(normalized);
                } catch (IllegalArgumentException ex) {
                    yield OTHER;
                }
            }
        };
    }
}
