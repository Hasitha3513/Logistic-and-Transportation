package com.transportlogistics.app.fleet.domain.model;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum DrugTestResult {
    PENDING,
    NEGATIVE,
    POSITIVE,
    INCONCLUSIVE;

    @JsonCreator
    public static DrugTestResult fromString(String value) {
        if (value == null || value.isBlank()) {
            return PENDING;
        }
        var normalized = value.trim().toUpperCase();
        try {
            return DrugTestResult.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            return PENDING;
        }
    }
}
