package com.transportlogistics.app.fleet;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record VehicleReadingCorrected(UUID correctionReadingId, UUID originalReadingId,
                                      UUID vehicleId, String readingType, BigDecimal correctedValue,
                                      UUID actorId, OffsetDateTime correctedAt) {
}