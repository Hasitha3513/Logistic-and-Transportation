package com.transportlogistics.app.trip.infrastructure.adapters.in.web.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record TripResponse(UUID id,
                           String tripNumber,
                           UUID customerId,
                           UUID departmentId,
                           UUID projectId,
                           UUID routeId,
                           String priority,
                           String status,
                           UUID originLocationId,
                           UUID destinationLocationId,
                           OffsetDateTime requestedStartTime,
                           OffsetDateTime requestedEndTime,
                           UUID requiredVehicleTypeId,
                           Double requiredCapacityKg,
                           String cargoDescription,
                           Integer passengerCount,
                           String customerInstructions,
                           String notes,
                           UUID vehicleId,
                           UUID driverId,
                           OffsetDateTime actualStartTime,
                           OffsetDateTime actualEndTime,
                           Double startOdometerKm,
                           Double endOdometerKm,
                           String completionRemarks,
                           OffsetDateTime createdAt,
                           OffsetDateTime updatedAt) {
}
