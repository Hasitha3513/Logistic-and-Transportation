package com.transportlogistics.app.fuel;

import java.time.OffsetDateTime;
import java.util.UUID;

public record FuelIssueCancelled(UUID fuelIssueId, String voucherNumber, UUID cancelledBy, String reason,
                                 OffsetDateTime cancelledAt) {
}
