package com.transportlogistics.app.fleet;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Public read-only query contract published by the Fleet module.
 */
public interface FleetReportingQuery {

    Optional<FleetVehicleSummary> findVehicle(UUID vehicleId);

    List<FleetVehicleSummary> findAllVehicles();

    Optional<FleetDriverSummary> findDriver(UUID driverId);

    List<FleetDriverSummary> findAllDrivers();

    List<FleetDocumentAlert> findExpiringDocuments(java.time.LocalDate cutoff);

    List<FleetExceptionAlert> findActiveExceptions();
}
