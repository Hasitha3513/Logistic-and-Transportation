package com.transportlogistics.app.fuel;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record FuelPurchaseReconciled(UUID fuelPurchaseId, String purchaseNumber, BigDecimal quantityVariance,
                                     BigDecimal priceVariance, UUID reconciledBy, OffsetDateTime reconciledAt) {
}
