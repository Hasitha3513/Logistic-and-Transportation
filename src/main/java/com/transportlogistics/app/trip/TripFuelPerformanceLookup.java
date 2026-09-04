package com.transportlogistics.app.trip;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Minimal bulk Trip attribution published for Fuel-owned US-37 analytics. */
public interface TripFuelPerformanceLookup {
    Map<UUID, Attribution> findAll(Set<UUID> tripIds);
    record Attribution(UUID tripId, UUID vehicleId, UUID driverId) {}
}
