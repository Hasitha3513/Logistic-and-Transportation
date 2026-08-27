package com.transportlogistics.app.fleet.vehiclemaster.adapters.inbound.web.dto.response;

import java.util.UUID;

public record VehicleResponse(UUID id,
                              String registrationNumber,
                              String chassisNumber,
                              String engineNumber,
                              UUID categoryId,
                              UUID typeId,
                              String manufacturer,
                              String model,
                              Integer manufactureYear,
                              String ownershipType,
                              String operationalStatus,
                              Double currentOdometerKm,
                              Double engineHours,
                              Double capacityKg,
                              Double tareWeightKg,
                              Double grossVehicleWeightKg,
                              Double cargoVolumeCapacityM3,
                              Integer axleCount,
                              Double maxAxleLoadKg,
                              boolean active) {
}
