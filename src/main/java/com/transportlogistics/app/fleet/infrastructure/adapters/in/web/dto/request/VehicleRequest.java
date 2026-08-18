package com.transportlogistics.app.fleet.infrastructure.adapters.in.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record VehicleRequest(@NotBlank String registrationNumber,
                             String chassisNumber,
                             String engineNumber,
                             @NotNull UUID categoryId,
                             @NotNull UUID typeId,
                             String manufacturer,
                             String model,
                             Integer manufactureYear,
                             String ownershipType,
                             String operationalStatus,
                             Double currentOdometerKm,
                             Double engineHours,
                             Double capacityKg,
                             Boolean active) {
}
