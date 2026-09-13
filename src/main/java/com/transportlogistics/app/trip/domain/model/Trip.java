package com.transportlogistics.app.trip.domain.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public record Trip(UUID id, String tripNumber, UUID customerId, UUID departmentId, UUID projectId, UUID routeId,
                   String routeVersion,
                   String priority, String status, UUID originLocationId, UUID destinationLocationId,
                   OffsetDateTime requestedStartTime, OffsetDateTime requestedEndTime, UUID requiredVehicleTypeId,
                   Double requiredCapacityKg, String cargoDescription, Integer passengerCount,
                   String customerInstructions, String notes, UUID vehicleId, UUID driverId,
                   OffsetDateTime actualStartTime, OffsetDateTime actualEndTime, Double startOdometerKm,
                   Double endOdometerKm, String completionRemarks, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
    public Trip {
        if (routeId == null && routeVersion != null) {
            throw new IllegalArgumentException("An unrouted trip cannot have a route version");
        }
        if (routeVersion != null && (routeVersion.length() > 120
                || !routeVersion.matches("REVISION:[1-9][0-9]*"))) {
            throw new IllegalArgumentException("Route version must use REVISION:<positive-integer>");
        }
    }

    /** Backward-compatible constructor for historical fixtures and null-revision rows. */
    public Trip(UUID id, String tripNumber, UUID customerId, UUID departmentId, UUID projectId, UUID routeId,
                String priority, String status, UUID originLocationId, UUID destinationLocationId,
                OffsetDateTime requestedStartTime, OffsetDateTime requestedEndTime, UUID requiredVehicleTypeId,
                Double requiredCapacityKg, String cargoDescription, Integer passengerCount,
                String customerInstructions, String notes, UUID vehicleId, UUID driverId,
                OffsetDateTime actualStartTime, OffsetDateTime actualEndTime, Double startOdometerKm,
                Double endOdometerKm, String completionRemarks, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this(id, tripNumber, customerId, departmentId, projectId, routeId, null, priority, status,
                originLocationId, destinationLocationId, requestedStartTime, requestedEndTime,
                requiredVehicleTypeId, requiredCapacityKg, cargoDescription, passengerCount,
                customerInstructions, notes, vehicleId, driverId, actualStartTime, actualEndTime,
                startOdometerKm, endOdometerKm, completionRemarks, createdAt, updatedAt);
    }
}
