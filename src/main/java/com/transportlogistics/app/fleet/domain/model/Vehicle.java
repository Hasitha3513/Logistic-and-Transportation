package com.transportlogistics.app.fleet.domain.model;

import java.util.UUID;

public record Vehicle(UUID id, String registrationNumber, String chassisNumber, String engineNumber, UUID categoryId,
                      UUID typeId, String manufacturer, String model, Integer manufactureYear, String ownershipType,
                      String operationalStatus, Double currentOdometerKm, Double engineHours, Double capacityKg,
                      boolean active) {
}
