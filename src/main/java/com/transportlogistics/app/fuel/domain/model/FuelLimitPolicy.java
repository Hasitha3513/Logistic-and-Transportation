package com.transportlogistics.app.fuel.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

/** MVP limit policy. A null vehicle applies globally; a vehicle policy takes precedence. */
public record FuelLimitPolicy(UUID id, UUID vehicleId, BigDecimal maximumQuantityPerIssue, boolean active) {
}
