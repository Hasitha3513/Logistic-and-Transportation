package com.transportlogistics.app.fuel;

import com.transportlogistics.app.fleet.TripDistanceStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record TripFuelCost(
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
        List<TripFuelCostLine> lines,
        OffsetDateTime calculatedAt
) {
}