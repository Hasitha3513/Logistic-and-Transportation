package com.transportlogistics.app.reporting.application.model;

import java.util.UUID;

public record VehicleUtilizationReportRecord(
        UUID vehicleId,
        String registrationNumber,
        String operationalStatus,
        long totalAssignedTrips,
        long completedTrips,
        double totalDistanceKm,
        double totalAllocatedHours
) {
}
