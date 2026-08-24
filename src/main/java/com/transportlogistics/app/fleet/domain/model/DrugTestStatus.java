package com.transportlogistics.app.fleet.domain.model;
import com.fasterxml.jackson.annotation.JsonCreator;
public enum DrugTestStatus {
    SCHEDULED,
    SAMPLE_COLLECTED,
    COMPLETED,
    CANCELLED;

    @JsonCreator
    public static DrugTestStatus fromString(String value) {
        if (value == null || value.isBlank()) {
            return CANCELLED;
        }
        var normalized = value.trim().toUpperCase();
        try {
            return DrugTestStatus.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            return CANCELLED;
        }
    }
}
