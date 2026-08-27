package com.transportlogistics.app.fleet.domain.model;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum FluidType {
    ENGINE_OIL,
    TRANSMISSION_OIL,
    HYDRAULIC_OIL,
    GEAR_OIL,
    COOLANT,
    BRAKE_FLUID,
    GREASE,
    OTHER;

    @JsonCreator
    public static FluidType fromString(String value) {
        if (value == null || value.isBlank()) {
            return OTHER;
        }
        var normalized = value.trim().toUpperCase();
        if (normalized.contains("ENGINE_OIL") || normalized.contains("ENGINE")) {
            return ENGINE_OIL;
        }
        if (normalized.contains("TRANSMISSION")) {
            return TRANSMISSION_OIL;
        }
        if (normalized.contains("HYDRAULIC")) {
            return HYDRAULIC_OIL;
        }
        if (normalized.contains("GEAR")) {
            return GEAR_OIL;
        }
        if (normalized.contains("COOLANT") || normalized.contains("RADIATOR")) {
            return COOLANT;
        }
        if (normalized.contains("BRAKE")) {
            return BRAKE_FLUID;
        }
        if (normalized.contains("GREASE")) {
            return GREASE;
        }
        try {
            return FluidType.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            return OTHER;
        }
    }
}
