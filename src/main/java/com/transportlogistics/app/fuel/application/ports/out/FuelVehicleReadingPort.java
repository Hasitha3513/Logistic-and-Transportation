package com.transportlogistics.app.fuel.application.ports.out;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Output port for recording authoritative operational vehicle readings during Fuel Issue operations.
 */
public interface FuelVehicleReadingPort {
    void recordIssue(UUID vehicleId, UUID fuelIssueId, BigDecimal odometerKm, BigDecimal engineHours,
                     OffsetDateTime issueDateTime, UUID actorId);
}
