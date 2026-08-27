package com.transportlogistics.app.fleet;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record VehicleMeterResetRecorded(UUID resetId, UUID vehicleId, String readingType,
                                        int fromEpoch, int toEpoch, BigDecimal lastReadingValue,
                                        BigDecimal newMeterValue, OffsetDateTime effectiveAt) {
}