package com.transportlogistics.app.fleet;

import java.util.Optional;
import java.util.UUID;

/**
 * Public fleet contract for cross-module driver profile lookups.
 */
public interface DriverLookup {
    Optional<FleetDriverSummary> findDriver(UUID id);
}
