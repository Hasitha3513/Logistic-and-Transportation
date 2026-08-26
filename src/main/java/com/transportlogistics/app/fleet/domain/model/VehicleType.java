package com.transportlogistics.app.fleet.domain.model;

import java.util.UUID;

public record VehicleType(UUID id, UUID categoryId, String code, String name, String description, boolean active) {
}
