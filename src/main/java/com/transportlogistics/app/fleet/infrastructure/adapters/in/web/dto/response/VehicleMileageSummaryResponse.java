package com.transportlogistics.app.fleet.infrastructure.adapters.in.web.dto.response;

import com.transportlogistics.app.fleet.CoverageStatus;
import com.transportlogistics.app.fleet.VehicleMileageSummary;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record VehicleMileageSummaryResponse(
        UUID vehicleId,
        OffsetDateTime from,
        OffsetDateTime to,
        BigDecimal openingOdometer,
        BigDecimal closingOdometer,
        BigDecimal distanceTravelledKm,
        BigDecimal openingEngineHours,
        BigDecimal closingEngineHours,
        BigDecimal engineHoursUsed,
        int meterResetCount,
        CoverageStatus coverageStatus,
        boolean abnormalDetected
) {
    public static VehicleMileageSummaryResponse from(VehicleMileageSummary s) {
        if (s == null) return null;
        return new VehicleMileageSummaryResponse(
                s.vehicleId(),
                s.from(),
                s.to(),
                s.openingOdometer(),
                s.closingOdometer(),
                s.distanceTravelledKm(),
                s.openingEngineHours(),
                s.closingEngineHours(),
                s.engineHoursUsed(),
                s.meterResetCount(),
                s.coverageStatus(),
                s.abnormalDetected()
        );
    }
}
