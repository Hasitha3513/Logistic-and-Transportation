package com.transportlogistics.app.fleet.infrastructure.adapters.in.web.dto.response;

import com.transportlogistics.app.fleet.domain.model.VehicleMeterReset;
import com.transportlogistics.app.fleet.domain.model.VehicleReadingType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record VehicleMeterResetResponse(
        UUID id,
        UUID vehicleId,
        VehicleReadingType readingType,
        int fromEpoch,
        int toEpoch,
        BigDecimal lastReadingValue,
        BigDecimal newMeterValue,
        OffsetDateTime effectiveAt,
        String reason,
        UUID createdBy,
        OffsetDateTime createdAt
) {
    public static VehicleMeterResetResponse from(VehicleMeterReset r) {
        if (r == null) return null;
        return new VehicleMeterResetResponse(
                r.id(),
                r.vehicleId(),
                r.readingType(),
                r.fromEpoch(),
                r.toEpoch(),
                r.lastReadingValue(),
                r.newMeterValue(),
                r.effectiveAt(),
                r.reason(),
                r.createdBy(),
                r.createdAt()
        );
    }
}
