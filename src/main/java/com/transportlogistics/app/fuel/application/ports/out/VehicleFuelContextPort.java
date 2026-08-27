package com.transportlogistics.app.fuel.application.ports.out;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface VehicleFuelContextPort {
    Optional<VehicleContext> find(UUID vehicleId);

    record VehicleContext(UUID id, String registrationNumber, boolean active, String operationalStatus,
                          BigDecimal currentOdometerKm, BigDecimal engineHours) {
    }
}
