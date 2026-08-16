package com.transportlogistics.app.fleet.domain.service;

import com.transportlogistics.app.fleet.domain.model.VehicleReading;
import com.transportlogistics.app.shared.domain.ConflictException;

import java.util.List;

public final class VehicleReadingChronologyPolicy {
    public void validate(VehicleReading candidate, VehicleReading previous, VehicleReading next,
                         List<VehicleReading> atSameTimestamp) {
        var sameTime = atSameTimestamp == null ? List.<VehicleReading>of() : atSameTimestamp;
        if (sameTime.stream().anyMatch(reading -> reading.value().compareTo(candidate.value()) != 0)) {
            throw new ConflictException("VEHICLE_READING_CHRONOLOGY_CONFLICT",
                    "A different effective reading already exists at the same recorded time");
        }
        if (previous != null && candidate.value().compareTo(previous.value()) < 0) {
            if (next == null) {
                throw new ConflictException("VEHICLE_READING_DECREASE",
                        "Reading is below the previous value; use the approved meter-reset workflow if the meter changed");
            }
            throw conflict("Reading is below the previous chronological value");
        }
        if (next != null && candidate.value().compareTo(next.value()) > 0) {
            throw conflict("Reading is above the next chronological value");
        }
    }

    private ConflictException conflict(String message) {
        return new ConflictException("VEHICLE_READING_CHRONOLOGY_CONFLICT", message);
    }
}
