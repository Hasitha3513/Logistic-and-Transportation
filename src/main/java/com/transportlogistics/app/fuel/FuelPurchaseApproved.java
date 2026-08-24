package com.transportlogistics.app.fuel;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record FuelPurchaseApproved(UUID fuelPurchaseId, String purchaseNumber, UUID vendorId,
                                   BigDecimal totalAmount, String currencyCode, UUID approvedBy,
                                   OffsetDateTime approvedAt) {
}
