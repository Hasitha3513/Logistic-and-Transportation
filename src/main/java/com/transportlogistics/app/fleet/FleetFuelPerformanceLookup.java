package com.transportlogistics.app.fleet;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Minimal bulk Fleet dimensions published for Fuel-owned US-37 analytics. */
public interface FleetFuelPerformanceLookup {
    Map<UUID, VehicleContext> vehicles(Set<UUID> vehicleIds);
    Map<UUID, DriverContext> drivers(Set<UUID> driverIds);

    record VehicleContext(UUID vehicleId, String registrationNumber, UUID vehicleTypeId, boolean active) {}
    record DriverContext(UUID driverId, String displayLabel, boolean active) {}
}
