package com.transportlogistics.app.fleet.domain.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record VehicleMeterReset(UUID id, UUID vehicleId, VehicleReadingType readingType,
                                int fromEpoch, int toEpoch, BigDecimal lastReadingValue,
                                BigDecimal newMeterValue, OffsetDateTime effectiveAt,
                                String reason, UUID createdBy, OffsetDateTime createdAt) {
}