package com.transportlogistics.app.fleet.domain.model;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum VisionTestStatus {
    PASSED,
    PASSED_WITH_CORRECTIVE_LENSES,
    PASSED_WITH_GLASSES,
    FAILED,
    NOT_TESTED,
    PENDING;

    @JsonCreator
    public static VisionTestStatus fromString(String value) {
        if (value == null || value.isBlank()) {
            return NOT_TESTED;
        }
        var normalized = value.trim().toUpperCase();
        return switch (normalized) {
            case "PASSED" -> PASSED;
            case "PASSED_WITH_CORRECTIVE_LENSES", "PASSED_WITH_GLASSES", "PASSED_WITH_LENSES" -> PASSED_WITH_CORRECTIVE_LENSES;
            case "FAILED" -> FAILED;
            case "PENDING" -> PENDING;
            case "NOT_TESTED" -> NOT_TESTED;
            default -> {
                try {
                    yield VisionTestStatus.valueOf(normalized);
                } catch (IllegalArgumentException ex) {
                    yield NOT_TESTED;
                }
            }
        };
    }
}
