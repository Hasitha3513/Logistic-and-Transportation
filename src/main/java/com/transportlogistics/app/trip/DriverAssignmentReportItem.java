package com.transportlogistics.app.trip;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DriverAssignmentReportItem(
        UUID tripId,
        String tripNumber,
        String status,
        UUID driverId,
        UUID vehicleId,
        UUID routeId,
        OffsetDateTime requestedStartTime,
        OffsetDateTime requestedEndTime,
        OffsetDateTime actualStartTime,
        OffsetDateTime actualEndTime,
        OffsetDateTime createdAt
) {
}
