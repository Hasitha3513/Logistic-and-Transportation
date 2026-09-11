package com.transportlogistics.app.fuel.application.ports.out;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public interface FuelPerformanceContextPort {
    Map<UUID, VehicleContext> vehicles(Set<UUID> ids);
    Map<UUID, DriverContext> drivers(Set<UUID> ids);
    Map<UUID, TripContext> trips(Set<UUID> ids);

    record VehicleContext(UUID id, String label, UUID typeId, boolean active) {}
    record DriverContext(UUID id, String label, boolean active) {}
    record TripContext(UUID id, UUID vehicleId, UUID driverId) {}
}
