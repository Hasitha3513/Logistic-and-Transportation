package com.transportlogistics.app.fuel;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record FuelIssued(UUID fuelIssueId, String voucherNumber, UUID vehicleId, UUID tripId,
                         BigDecimal quantity, OffsetDateTime issuedAt) {
}
