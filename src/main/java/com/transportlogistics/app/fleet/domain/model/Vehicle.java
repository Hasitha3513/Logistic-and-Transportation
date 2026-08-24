package com.transportlogistics.app.fleet.domain.model;

import com.transportlogistics.app.shared.domain.ConflictException;

import java.time.Year;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record Vehicle(UUID id, String registrationNumber, String chassisNumber, String engineNumber, UUID categoryId,
                      UUID typeId, String manufacturer, String model, Integer manufactureYear, String ownershipType,
                      String operationalStatus, Double currentOdometerKm, Double engineHours, Double capacityKg,
                      boolean active) {

    public Vehicle {
        id = id != null ? id : UUID.randomUUID();
        registrationNumber = normalizeRequired(registrationNumber, "Registration number is required");
        chassisNumber = normalizeOptional(chassisNumber);
        engineNumber = normalizeOptional(engineNumber);
        manufacturer = normalizeOptional(manufacturer);
        model = normalizeOptional(model);
        categoryId = categoryId != null ? categoryId : UUID.randomUUID();
        typeId = typeId != null ? typeId : UUID.randomUUID();
        ownershipType = normalizeOwnership(ownershipType);
        operationalStatus = normalizeStatus(operationalStatus);

        if (capacityKg != null && capacityKg < 0) {
            throw new IllegalArgumentException("Capacity cannot be negative");
        }
        if (currentOdometerKm != null && currentOdometerKm < 0) {
            throw new IllegalArgumentException("Current odometer cannot be negative");
        }
        if (engineHours != null && engineHours < 0) {
            throw new IllegalArgumentException("Engine hours cannot be negative");
        }
        if (manufactureYear != null) {
            int maxYear = Year.now().getValue() + 1;
            if (manufactureYear < 1900 || manufactureYear > maxYear) {
                throw new IllegalArgumentException("Manufacture year must be between 1900 and " + maxYear);
            }
        }
    }

    public static void validateStatusTransition(String currentStatus, String nextStatus) {
        var from = currentStatus != null ? currentStatus.trim().toUpperCase() : "AVAILABLE";
        var to = nextStatus != null ? nextStatus.trim().toUpperCase() : "AVAILABLE";
        if (from.equals(to)) {
            return;
        }
        boolean allowed = switch (from) {
            case "AVAILABLE" -> Set.of("ALLOCATED", "MAINTENANCE", "UNDER_MAINTENANCE", "OUT_OF_SERVICE", "BROKEN_DOWN").contains(to);
            case "ALLOCATED" -> Set.of("AVAILABLE", "MAINTENANCE", "UNDER_MAINTENANCE", "OUT_OF_SERVICE", "BROKEN_DOWN").contains(to);
            case "MAINTENANCE", "UNDER_MAINTENANCE" -> Set.of("AVAILABLE", "OUT_OF_SERVICE").contains(to);
            case "OUT_OF_SERVICE", "BROKEN_DOWN" -> Set.of("AVAILABLE", "MAINTENANCE", "UNDER_MAINTENANCE").contains(to);
            default -> false;
        };
        if (!allowed) {
            throw new ConflictException("VEHICLE_STATUS_TRANSITION_INVALID",
                    "Invalid vehicle status transition from " + from + " to " + to);
        }
    }

    private static String normalizeRequired(String value, String message) {
        if (value == null || value.trim().isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim().toUpperCase();
    }

    private static String normalizeOptional(String value) {
        if (value == null || value.trim().isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static String normalizeOwnership(String value) {
        if (value == null || value.trim().isBlank()) {
            return "COMPANY_OWNED";
        }
        return value.trim().toUpperCase();
    }

    private static String normalizeStatus(String value) {
        if (value == null || value.trim().isBlank()) {
            return "AVAILABLE";
        }
        return value.trim().toUpperCase();
    }
}
