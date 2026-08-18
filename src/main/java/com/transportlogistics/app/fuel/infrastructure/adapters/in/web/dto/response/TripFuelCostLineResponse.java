package com.transportlogistics.app.fuel.infrastructure.adapters.in.web.dto.response;

import com.transportlogistics.app.fuel.PricingSource;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record TripFuelCostLineResponse(
        UUID fuelIssueId,
        String voucherNumber,
        OffsetDateTime issuedAt,
        BigDecimal quantityLiters,
        BigDecimal unitPrice,
        BigDecimal lineCost,
        PricingSource pricingSource,
        String currencyCode,
        UUID stationId,
        String fuelType
) {
}
