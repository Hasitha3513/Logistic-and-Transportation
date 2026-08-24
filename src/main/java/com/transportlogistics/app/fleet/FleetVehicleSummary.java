package com.transportlogistics.app.fleet;

import java.util.UUID;

public record FleetVehicleSummary(
        UUID id,
        String registrationNumber,
        String operationalStatus,
        Double currentOdometerKm,
        boolean active
) {
}
