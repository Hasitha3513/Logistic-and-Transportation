package com.transportlogistics.app.fleet.infrastructure.adapters.in.web.dto.response;

import com.transportlogistics.app.fleet.domain.model.VehicleReading;
import com.transportlogistics.app.fleet.domain.model.VehicleReadingSourceType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record LatestVehicleReadingsResponse(
        UUID vehicleId,
        ReadingSnapshot odometer,
        ReadingSnapshot engineHours
) {
    public record ReadingSnapshot(
            UUID readingId,
            BigDecimal value,
            String unit,
            int meterEpoch,
            VehicleReadingSourceType sourceType,
            UUID sourceReferenceId,
            OffsetDateTime recordedAt,
            OffsetDateTime receivedAt
    ) {
        public static ReadingSnapshot from(VehicleReading r) {
            if (r == null) return null;
            return new ReadingSnapshot(
                    r.id(),
                    r.value(),
                    r.unit().name(),
                    r.meterEpoch(),
                    r.sourceType(),
                    r.sourceReferenceId(),
                    r.recordedAt(),
                    r.receivedAt()
            );
        }
    }
}
