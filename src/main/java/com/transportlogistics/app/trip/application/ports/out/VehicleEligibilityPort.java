package com.transportlogistics.app.trip.application.ports.out;

import java.time.LocalDate;
import java.util.UUID;

public interface VehicleEligibilityPort {
    void assertEligible(UUID vehicleId, LocalDate onDate);
}
