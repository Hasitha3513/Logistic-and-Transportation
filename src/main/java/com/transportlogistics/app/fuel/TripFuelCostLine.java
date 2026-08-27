package com.transportlogistics.app.fuel;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record TripFuelCostLine(
        UUID fuelIssueId,
        String voucherNumber,
        OffsetDateTime issuedAt,
        BigDecimal quantityLiters,
        BigDecimal unitPrice,
        BigDecimal lineCost,
        com.transportlogistics.app.fuel.domain.model.PricingSource pricingSource,
        String currencyCode,
        UUID stationId,
        String fuelType
) {
}