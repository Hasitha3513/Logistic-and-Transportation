package com.transportlogistics.app.fleet.domain.model;

import java.util.UUID;

public record VehicleCategory(UUID id, String code, String name, String description, boolean active) {
}
