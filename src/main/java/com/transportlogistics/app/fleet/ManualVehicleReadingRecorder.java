package com.transportlogistics.app.fleet;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Narrow Fleet module boundary for recording a manual vehicle reading. */
public interface ManualVehicleReadingRecorder {
    Result recordManual(Command command);

    record Command(UUID vehicleId, ReadingType readingType, BigDecimal value, OffsetDateTime recordedAt,
                   UUID actorId, String idempotencyKey, String notes) {
    }

    record Result(UUID readingId, UUID vehicleId, ReadingType readingType, BigDecimal value,
                  OffsetDateTime recordedAt) {
    }

    enum ReadingType { ODOMETER, ENGINE_HOURS }
}
