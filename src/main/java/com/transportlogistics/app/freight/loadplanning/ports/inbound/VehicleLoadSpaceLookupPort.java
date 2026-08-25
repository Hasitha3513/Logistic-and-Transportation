package com.transportlogistics.app.freight.loadplanning.ports.inbound;

import java.util.Optional;
import java.util.UUID;

/**
 * Port for looking up vehicle / load-space data needed for load planning.
 */
public interface VehicleLoadSpaceLookupPort {

    record VehiclePlanningView(
            UUID vehicleId,
            String registrationNumber,
            Double capacityKg,
            String operationalStatus,
            boolean active
    ) {}

    Optional<VehiclePlanningView> findVehicle(UUID vehicleId);
}
