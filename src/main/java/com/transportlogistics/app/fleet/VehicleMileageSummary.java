package com.transportlogistics.app.fleet;

import com.transportlogistics.app.fleet.domain.model.VehicleReadingSourceType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Derived, read-side domain summary of vehicle distance, engine hours, and reading counts
 * over an operational period. Not stored as a separate source of truth.
 */
public record VehicleMileageSummary(
        UUID vehicleId,
        OffsetDateTime from,
        OffsetDateTime to,
        BigDecimal openingOdometer,
        BigDecimal closingOdometer,
        BigDecimal distanceKm,
        BigDecimal openingEngineHours,
        BigDecimal closingEngineHours,
        BigDecimal engineHoursUsed,
        int readingCount,
        int correctionCount,
        int meterResetCount,
        OffsetDateTime firstReadingAt,
        OffsetDateTime lastReadingAt,
        CoverageStatus coverageStatus,
        String coverageReason,
        Map<VehicleReadingSourceType, Integer> sourceCounts
) {
    public VehicleMileageSummary {
        Objects.requireNonNull(vehicleId, "vehicleId must not be null");
        Objects.requireNonNull(from, "from must not be null");
        Objects.requireNonNull(to, "to must not be null");
        Objects.requireNonNull(coverageStatus, "coverageStatus must not be null");
        sourceCounts = sourceCounts == null ? Map.of() : Map.copyOf(sourceCounts);
    }
}
