package com.transportlogistics.app.fuel;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record FuelPurchaseReceived(UUID fuelPurchaseId, String purchaseNumber, UUID vendorId, UUID fuelStationId,
                                   String fuelType, BigDecimal receivedQuantity, BigDecimal quantityVariance,
                                   OffsetDateTime receivedAt) {
}
