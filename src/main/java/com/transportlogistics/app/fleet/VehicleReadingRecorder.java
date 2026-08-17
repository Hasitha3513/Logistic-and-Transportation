package com.transportlogistics.app.fleet;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Fleet-owned module boundary for future operational reading sources.
 */
public interface VehicleReadingRecorder {
    Result record(Command command);

    enum ReadingType {ODOMETER, ENGINE_HOURS}

    enum SourceType {TRIP_START, TRIP_END, FUEL_ISSUE, BASELINE}

    record Command(UUID vehicleId, ReadingType readingType, BigDecimal value, SourceType sourceType,
                   UUID sourceReferenceId, OffsetDateTime recordedAt, UUID actorId) {
    }

    record Result(UUID readingId, UUID vehicleId, ReadingType readingType, BigDecimal value, String unit,
                  SourceType sourceType, UUID sourceReferenceId, OffsetDateTime recordedAt,
                  OffsetDateTime receivedAt) {
    }
}
