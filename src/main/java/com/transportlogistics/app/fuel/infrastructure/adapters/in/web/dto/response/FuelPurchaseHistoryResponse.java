package com.transportlogistics.app.fuel.infrastructure.adapters.in.web.dto.response;

import com.transportlogistics.app.fuel.domain.model.FuelPurchaseStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record FuelPurchaseHistoryResponse(UUID id,
                                          UUID fuelPurchaseId,
                                          FuelPurchaseStatus fromStatus,
                                          FuelPurchaseStatus toStatus,
                                          String action,
                                          UUID actorId,
                                          String actor,
                                          String comment,
                                          BigDecimal quantityVariance,
                                          BigDecimal priceVariance,
                                          OffsetDateTime occurredAt) {
}
