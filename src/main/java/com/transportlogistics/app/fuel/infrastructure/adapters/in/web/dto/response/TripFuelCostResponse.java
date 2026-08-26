package com.transportlogistics.app.fuel.infrastructure.adapters.in.web.dto.response;

import com.transportlogistics.app.fleet.TripDistanceStatus;
import com.transportlogistics.app.fuel.TripFuelCostCalculationStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record TripFuelCostResponse(
        UUID tripId,
        UUID vehicleId,
        BigDecimal totalFuelQuantityLiters,
        String currencyCode,
        BigDecimal totalFuelCost,
        BigDecimal tripDistanceKm,
        BigDecimal costPerKm,
        BigDecimal litersPer100Km,
        int fuelIssueCount,
        int unpricedIssueCount,
        TripDistanceStatus distanceStatus,
        TripFuelCostCalculationStatus calculationStatus,
        List<TripFuelCostLineResponse> lines,
        OffsetDateTime calculatedAt
) {
}
