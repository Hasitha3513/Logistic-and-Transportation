package com.transportlogistics.app.fleet;

import java.math.BigDecimal;
import java.util.UUID;

public record TripDistanceSummary(UUID tripId, UUID vehicleId, BigDecimal startOdometerKm,
                                  BigDecimal endOdometerKm, BigDecimal distanceTravelledKm,
                                  TripDistanceStatus status) {
}