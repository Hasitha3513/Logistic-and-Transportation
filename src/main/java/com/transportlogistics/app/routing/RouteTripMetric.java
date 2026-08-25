package com.transportlogistics.app.routing;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Provider-neutral trip measurement contract exposed by the routing module.
 */
public record RouteTripMetric(
        UUID tripId,
        String tripNumber,
        String status,
        OffsetDateTime requestedStartTime,
        OffsetDateTime requestedEndTime,
        OffsetDateTime actualStartTime,
        OffsetDateTime actualEndTime,
        Double startOdometerKm,
        Double endOdometerKm,
        int totalDelayMinutes
) {
}
