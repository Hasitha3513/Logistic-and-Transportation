package com.transportlogistics.app.reporting.application.ports.out;

import com.transportlogistics.app.fleet.FleetDriverSummary;
import com.transportlogistics.app.fleet.FleetVehicleSummary;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FleetReportReadPort {

    Optional<FleetVehicleSummary> findVehicle(UUID vehicleId);

    List<FleetVehicleSummary> findAllVehicles();

    Optional<FleetDriverSummary> findDriver(UUID driverId);

    List<FleetDriverSummary> findAllDrivers();
}
