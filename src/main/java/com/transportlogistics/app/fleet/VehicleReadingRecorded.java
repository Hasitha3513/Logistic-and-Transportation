package com.transportlogistics.app.fleet;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record VehicleReadingRecorded(UUID readingId, UUID vehicleId, String readingType, BigDecimal value,
                                     String unit, String sourceType, UUID sourceReferenceId,
                                     OffsetDateTime recordedAt, OffsetDateTime receivedAt) {
}
