package com.transportlogistics.app.trip;

import java.time.OffsetDateTime;
import java.util.UUID;

public record VehicleTripReportItem(
        UUID tripId,
        String tripNumber,
        String status,
        UUID vehicleId,
        OffsetDateTime requestedStartTime,
        OffsetDateTime requestedEndTime,
        OffsetDateTime actualStartTime,
        OffsetDateTime actualEndTime,
        Double startOdometerKm,
        Double endOdometerKm
) {
}
