package com.transportlogistics.app.fleet;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/**
 * Minimal fleet-owned view used by fuel operations.
 */
public interface VehicleFuelContextLookup {
    Optional<VehicleFuelContext> find(UUID vehicleId);

    record VehicleFuelContext(UUID vehicleId, String registrationNumber, boolean active, String operationalStatus,
                              BigDecimal currentOdometerKm, BigDecimal engineHours) {
    }
}
