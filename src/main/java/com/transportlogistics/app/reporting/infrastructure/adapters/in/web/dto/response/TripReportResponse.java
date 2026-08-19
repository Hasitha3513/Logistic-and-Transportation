package com.transportlogistics.app.reporting.infrastructure.adapters.in.web.dto.response;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record TripReportResponse(
        UUID tripId,
        String tripNumber,
        String status,
        LocalDate scheduledDeparture,
        LocalDate scheduledArrival,
        LocalDate actualStart,
        LocalDate actualCompletion,
        UUID vehicleId,
        String vehicleRegistrationNumber,
        UUID driverId,
        String driverEmployeeNumber,
        String driverName,
        UUID routeId,
        Double distanceKm,
        UUID customerId,
        String completionRemarks,
        OffsetDateTime createdAt
) {
}
