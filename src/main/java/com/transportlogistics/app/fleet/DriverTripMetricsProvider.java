package com.transportlogistics.app.fleet;

import java.util.UUID;

/**
 * Fleet-side trip metrics lookup port implemented by cross-module composition.
 */
public interface DriverTripMetricsProvider {
    DriverTripSummary getTripSummary(UUID driverId);

    record DriverTripSummary(
            int totalTripsAssigned,
            int totalTripsCompleted,
            int totalTripsCancelled
    ) {
    }
}
