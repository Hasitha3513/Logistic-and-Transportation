package com.transportlogistics.app.fuel.domain.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record FuelPurchaseHistory(UUID id, UUID fuelPurchaseId, FuelPurchaseStatus fromStatus,
                                  FuelPurchaseStatus toStatus, String action, UUID actorId, String actor,
                                  String comment, BigDecimal quantityVariance, BigDecimal priceVariance,
                                  OffsetDateTime occurredAt) {
}
