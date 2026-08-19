package com.transportlogistics.app.reporting.application.model;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record DriverAssignmentReportRecord(
        UUID driverId,
        String employeeNumber,
        String driverName,
        String driverStatus,
        UUID tripId,
        String tripNumber,
        String tripStatus,
        LocalDate scheduledDeparture,
        LocalDate scheduledArrival,
        LocalDate actualStart,
        LocalDate actualCompletion,
        UUID vehicleId,
        String vehicleRegistrationNumber,
        UUID routeId,
        OffsetDateTime assignmentCreatedAt
) {
}
