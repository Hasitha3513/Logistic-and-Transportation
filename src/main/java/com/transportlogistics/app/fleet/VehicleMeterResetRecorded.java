package com.transportlogistics.app.fleet;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record VehicleMeterResetRecorded(UUID resetId, UUID vehicleId, String readingType,
                                        UUID previousReadingId, BigDecimal previousMeterValue,
                                        UUID newReadingId, BigDecimal newMeterValue,
                                        OffsetDateTime effectiveAt, String reason,
                                        UUID createdBy, UUID approvedBy, OffsetDateTime createdAt) {
}
