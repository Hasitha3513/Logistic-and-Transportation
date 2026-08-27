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
            Double tareWeightKg,
            Double grossVehicleWeightKg,
            Double cargoVolumeCapacityM3,
            Integer axleCount,
            Double maxAxleLoadKg,
            String operationalStatus,
            boolean active
    ) {
        public VehiclePlanningView(UUID vehicleId, String registrationNumber, Double capacityKg, String operationalStatus, boolean active) {
            this(vehicleId, registrationNumber, capacityKg, null, null, null, null, null, operationalStatus, active);
        }
    }

    Optional<VehiclePlanningView> findVehicle(UUID vehicleId);
}
