package com.transportlogistics.app.fleet.domain.model;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum DrugTestType {
    RANDOM,
    SCHEDULED,
    PRE_EMPLOYMENT,
    POST_INCIDENT,
    POST_ACCIDENT,
    PERIODIC,
    REASONABLE_SUSPICION,
    RETURN_TO_DUTY,
    FOLLOW_UP,
    OTHER;

    @JsonCreator
    public static DrugTestType fromString(String value) {
        if (value == null || value.isBlank()) {
            return RANDOM;
        }
        var normalized = value.trim().toUpperCase();
        return switch (normalized) {
            case "RANDOM" -> RANDOM;
            case "SCHEDULED", "PERIODIC" -> SCHEDULED;
            case "PRE_EMPLOYMENT" -> PRE_EMPLOYMENT;
            case "POST_INCIDENT", "POST_ACCIDENT" -> POST_INCIDENT;
            case "REASONABLE_SUSPICION" -> REASONABLE_SUSPICION;
            case "RETURN_TO_DUTY" -> RETURN_TO_DUTY;
            case "FOLLOW_UP" -> FOLLOW_UP;
            default -> {
                try {
                    yield DrugTestType.valueOf(normalized);
                } catch (IllegalArgumentException ex) {
                    yield OTHER;
                }
            }
        };
    }
}
