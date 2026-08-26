package com.transportlogistics.app.fuel.application.ports.out;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public interface FuelVehicleReadingPort {
    void record(UUID vehicleId, UUID fuelIssueId, BigDecimal odometer, BigDecimal engineHours,
                OffsetDateTime recordedAt, UUID actorId);
}