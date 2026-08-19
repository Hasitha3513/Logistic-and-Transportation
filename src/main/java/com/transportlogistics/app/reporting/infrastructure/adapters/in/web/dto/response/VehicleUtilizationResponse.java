package com.transportlogistics.app.reporting.infrastructure.adapters.in.web.dto.response;

import java.util.UUID;

public record VehicleUtilizationResponse(
        UUID vehicleId,
        String registrationNumber,
        String operationalStatus,
        long totalAssignedTrips,
        long completedTrips,
        double totalDistanceKm,
        double totalAllocatedHours
) {
}
