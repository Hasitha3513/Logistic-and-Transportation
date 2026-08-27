package com.transportlogistics.app.trip.domain.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public record Trip(UUID id, String tripNumber, UUID customerId, UUID departmentId, UUID projectId, UUID routeId,
                   String priority, String status, UUID originLocationId, UUID destinationLocationId,
                   OffsetDateTime requestedStartTime, OffsetDateTime requestedEndTime, UUID requiredVehicleTypeId,
                   Double requiredCapacityKg, String cargoDescription, Integer passengerCount,
                   String customerInstructions, String notes, UUID vehicleId, UUID driverId,
                   OffsetDateTime actualStartTime, OffsetDateTime actualEndTime, Double startOdometerKm,
                   Double endOdometerKm, String completionRemarks, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
}