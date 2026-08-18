package com.transportlogistics.app.fleet.infrastructure.adapters.in.web.dto.response;

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
                              boolean active) {
}
