package com.transportlogistics.app.fuel.infrastructure.adapters.out.fleet;

import com.transportlogistics.app.fleet.VehicleReadingRecorder;
import com.transportlogistics.app.fuel.application.ports.out.FuelVehicleReadingPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class FleetFuelVehicleReadingAdapter implements FuelVehicleReadingPort {
    private final VehicleReadingRecorder recorder;

    @Override
    public void recordIssue(UUID vehicleId, UUID fuelIssueId, BigDecimal odometerKm, BigDecimal engineHours,
                            OffsetDateTime issueDateTime, UUID actorId) {
        if (odometerKm != null) {
            recorder.record(new VehicleReadingRecorder.Command(
                    vehicleId,
                    VehicleReadingRecorder.ReadingType.ODOMETER,
                    odometerKm,
                    VehicleReadingRecorder.SourceType.FUEL_ISSUE,
                    fuelIssueId,
                    issueDateTime,
                    actorId
            ));
        }
        if (engineHours != null) {
            recorder.record(new VehicleReadingRecorder.Command(
                    vehicleId,
                    VehicleReadingRecorder.ReadingType.ENGINE_HOURS,
                    engineHours,
                    VehicleReadingRecorder.SourceType.FUEL_ISSUE,
                    fuelIssueId,
                    issueDateTime,
                    actorId
            ));
        }
    }
}
