package com.transportlogistics.app.fleet.infrastructure.adapters.in.web.dto.response;

import com.transportlogistics.app.fleet.domain.model.VehicleReading;
import com.transportlogistics.app.fleet.domain.model.VehicleReadingSourceType;
import com.transportlogistics.app.fleet.domain.model.VehicleReadingType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record VehicleReadingResponse(
        UUID id,
        UUID vehicleId,
        VehicleReadingType readingType,
        BigDecimal value,
        String unit,
        int meterEpoch,
        VehicleReadingSourceType sourceType,
        UUID sourceReferenceId,
        OffsetDateTime recordedAt,
        OffsetDateTime receivedAt,
        UUID createdBy,
        UUID correctionOfReadingId,
        String correctionReason,
        String idempotencyKey,
        String notes,
        OffsetDateTime createdAt
) {
    public static VehicleReadingResponse from(VehicleReading r) {
        if (r == null) return null;
        return new VehicleReadingResponse(
                r.id(),
                r.vehicleId(),
                r.readingType(),
                r.value(),
                r.unit().name(),
                r.meterEpoch(),
                r.sourceType(),
                r.sourceReferenceId(),
                r.recordedAt(),
                r.receivedAt(),
                r.createdBy(),
                r.correctionOfReadingId(),
                r.correctionReason(),
                r.idempotencyKey(),
                r.notes(),
                r.createdAt()
        );
    }
}
