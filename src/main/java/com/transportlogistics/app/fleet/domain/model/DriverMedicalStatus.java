package com.transportlogistics.app.fleet.domain.model;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum DriverMedicalStatus {
    FIT,
    FIT_WITH_RESTRICTIONS,
    TEMPORARILY_UNFIT,
    UNFIT;

    @JsonCreator
    public static DriverMedicalStatus fromString(String value) {
        if (value == null || value.isBlank()) {
            return TEMPORARILY_UNFIT;
        }
        var normalized = value.trim().toUpperCase();
        try {
            return DriverMedicalStatus.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            return TEMPORARILY_UNFIT;
        }
    }
}
