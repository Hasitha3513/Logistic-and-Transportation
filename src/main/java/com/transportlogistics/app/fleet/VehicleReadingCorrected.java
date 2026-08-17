package com.transportlogistics.app.fleet;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record VehicleReadingCorrected(UUID readingId, UUID originalReadingId, UUID vehicleId,
                                      String readingType, BigDecimal value, BigDecimal originalValue,
                                      String unit, String correctionReason, OffsetDateTime recordedAt,
                                      OffsetDateTime receivedAt, UUID createdBy) {
}
