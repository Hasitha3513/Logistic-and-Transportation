package com.transportlogistics.app.fuel;

import java.time.OffsetDateTime;
import java.util.UUID;

public record FuelIssueAuthorized(UUID fuelIssueId, String voucherNumber, UUID authorizedBy,
                                  OffsetDateTime authorizedAt) {
}
