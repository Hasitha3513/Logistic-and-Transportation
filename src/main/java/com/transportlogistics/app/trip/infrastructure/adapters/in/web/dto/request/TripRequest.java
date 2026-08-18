package com.transportlogistics.app.trip.infrastructure.adapters.in.web.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.UUID;

public record TripRequest(UUID customerId,
                          UUID departmentId,
                          UUID projectId,
                          UUID routeId,
                          String priority,
                          @NotNull UUID originLocationId,
                          @NotNull UUID destinationLocationId,
                          @NotNull OffsetDateTime requestedStartTime,
                          @NotNull OffsetDateTime requestedEndTime,
                          UUID requiredVehicleTypeId,
                          Double requiredCapacityKg,
                          String cargoDescription,
                          Integer passengerCount,
                          String customerInstructions,
                          String notes) {
}
