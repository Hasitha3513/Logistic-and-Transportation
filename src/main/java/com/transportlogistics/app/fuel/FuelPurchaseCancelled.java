package com.transportlogistics.app.fuel;

import java.time.OffsetDateTime;
import java.util.UUID;

public record FuelPurchaseCancelled(UUID fuelPurchaseId, String purchaseNumber, String reason,
                                    UUID cancelledBy, OffsetDateTime cancelledAt) {
}
