package com.transportlogistics.app.fleet.vehiclemaster.adapters.inbound.web.dto.request;

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
                             @PositiveOrZero(message = "Tare weight cannot be negative") Double tareWeightKg,
                             @PositiveOrZero(message = "Gross vehicle weight cannot be negative") Double grossVehicleWeightKg,
                             @PositiveOrZero(message = "Cargo volume capacity cannot be negative") Double cargoVolumeCapacityM3,
                             @Min(value = 1, message = "Axle count must be at least 1") Integer axleCount,
                             @PositiveOrZero(message = "Max axle load cannot be negative") Double maxAxleLoadKg,
                             Boolean active) {

    public VehicleRequest(String registrationNumber, String chassisNumber, String engineNumber,
                          UUID categoryId, UUID typeId, String manufacturer, String model,
                          Integer manufactureYear, String ownershipType, String operationalStatus,
                          Double currentOdometerKm, Double engineHours, Double capacityKg, Boolean active) {
        this(registrationNumber, chassisNumber, engineNumber, categoryId, typeId, manufacturer, model,
                manufactureYear, ownershipType, operationalStatus, currentOdometerKm, engineHours, capacityKg,
                null, null, null, null, null, active);
    }
}
