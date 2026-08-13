package com.transportlogistics.app.fleet;

import java.time.LocalDate;
import java.util.UUID;

/** Public fleet contract used by trip allocation and dispatch. */
public interface VehicleDispatchEligibility {
    void assertEligible(UUID vehicleId, LocalDate onDate);
}
