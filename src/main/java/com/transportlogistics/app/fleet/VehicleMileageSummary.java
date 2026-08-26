package com.transportlogistics.app.fleet;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record VehicleMileageSummary(UUID vehicleId, OffsetDateTime from, OffsetDateTime to,
                                    BigDecimal openingOdometer, BigDecimal closingOdometer,
                                    BigDecimal distanceTravelledKm, BigDecimal openingEngineHours,
                                    BigDecimal closingEngineHours, BigDecimal engineHoursUsed,
                                    int meterResetCount, CoverageStatus coverageStatus,
                                    boolean abnormalDetected) {
}