package com.transportlogistics.app.fleet;

import java.util.UUID;

public record FleetVehicleSummary(
        UUID id,
        String registrationNumber,
        String operationalStatus,
        Double currentOdometerKm,
        Double capacityKg,
        Double tareWeightKg,
        Double grossVehicleWeightKg,
        Double cargoVolumeCapacityM3,
        Integer axleCount,
        Double maxAxleLoadKg,
        boolean active
) {
    public FleetVehicleSummary(UUID id, String registrationNumber, String operationalStatus, Double currentOdometerKm, boolean active) {
        this(id, registrationNumber, operationalStatus, currentOdometerKm, null, null, null, null, null, null, active);
    }
}
