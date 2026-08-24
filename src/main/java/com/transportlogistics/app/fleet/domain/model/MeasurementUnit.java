package com.transportlogistics.app.fleet.domain.model;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum MeasurementUnit {
    LITRE,
    MILLILITRE,
    KILOGRAM,
    GRAM;

    @JsonCreator
    public static MeasurementUnit fromString(String value) {
        if (value == null || value.isBlank()) {
            return LITRE;
        }
        var normalized = value.trim().toUpperCase();
        return switch (normalized) {
            case "LITRE", "LITRES", "LITER", "LITERS", "L" -> LITRE;
            case "MILLILITRE", "MILLILITRES", "MILLILITER", "MILLILITERS", "ML" -> MILLILITRE;
            case "KILOGRAM", "KILOGRAMS", "KG" -> KILOGRAM;
            case "GRAM", "GRAMS", "G" -> GRAM;
            default -> {
                try {
                    yield MeasurementUnit.valueOf(normalized);
                } catch (IllegalArgumentException ex) {
                    yield LITRE;
                }
            }
        };
    }
}
