package com.transportlogistics.app.fleet.infrastructure.adapters.in.web.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.UUID;

public record VehicleRequest(@NotBlank(message = "Registration number is required") String registrationNumber,
                             String chassisNumber,
                             String engineNumber,
                             @NotNull(message = "Category ID is required") UUID categoryId,
                             @NotNull(message = "Vehicle type ID is required") UUID typeId,
                             String manufacturer,
                             String model,
                             @Min(value = 1900, message = "Manufacture year must be at least 1900") Integer manufactureYear,
                             String ownershipType,
                             String operationalStatus,
                             @PositiveOrZero(message = "Current odometer cannot be negative") Double currentOdometerKm,
                             @PositiveOrZero(message = "Engine hours cannot be negative") Double engineHours,
                             @PositiveOrZero(message = "Capacity cannot be negative") Double capacityKg,
                             Boolean active) {
}
