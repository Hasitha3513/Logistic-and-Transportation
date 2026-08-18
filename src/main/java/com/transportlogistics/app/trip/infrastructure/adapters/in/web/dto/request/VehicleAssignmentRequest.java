package com.transportlogistics.app.trip.infrastructure.adapters.in.web.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record VehicleAssignmentRequest(@NotNull UUID vehicleId) {
}
