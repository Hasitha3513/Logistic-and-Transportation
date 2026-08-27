package com.transportlogistics.app.fleet.infrastructure.adapters.in.web.dto.response;

import com.transportlogistics.app.fleet.TripDistanceStatus;
import com.transportlogistics.app.fleet.TripDistanceSummary;

import java.math.BigDecimal;
import java.util.UUID;

public record TripDistanceSummaryResponse(
        UUID tripId,
        UUID vehicleId,
        BigDecimal startOdometerKm,
        BigDecimal endOdometerKm,
        BigDecimal distanceTravelledKm,
        TripDistanceStatus status
) {
    public static TripDistanceSummaryResponse from(TripDistanceSummary s) {
        if (s == null) return null;
        return new TripDistanceSummaryResponse(
                s.tripId(),
                s.vehicleId(),
                s.startOdometerKm(),
                s.endOdometerKm(),
                s.distanceTravelledKm(),
                s.status()
        );
    }
}
