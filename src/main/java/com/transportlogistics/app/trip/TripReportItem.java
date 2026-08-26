package com.transportlogistics.app.trip;

import java.time.OffsetDateTime;
import java.util.UUID;

public record TripReportItem(
        UUID tripId,
        String tripNumber,
        String status,
        UUID customerId,
        UUID vehicleId,
        UUID driverId,
        UUID routeId,
        OffsetDateTime requestedStartTime,
        OffsetDateTime requestedEndTime,
        OffsetDateTime actualStartTime,
        OffsetDateTime actualEndTime,
        Double startOdometerKm,
        Double endOdometerKm,
        String completionRemarks,
        OffsetDateTime createdAt
) {
}
