package com.transportlogistics.app.fuel.infrastructure.adapters.out.fleet;

import com.transportlogistics.app.fleet.VehicleReadingRecorder;
import com.transportlogistics.app.fuel.application.ports.out.FuelVehicleReadingPort;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Component
class FleetFuelVehicleReadingAdapter implements FuelVehicleReadingPort {
    private final VehicleReadingRecorder recorder;

    FleetFuelVehicleReadingAdapter(VehicleReadingRecorder recorder) {
        this.recorder = recorder;
    }

    @Override
    public void record(UUID vehicleId, UUID fuelIssueId, BigDecimal odometer, BigDecimal engineHours,
                       OffsetDateTime recordedAt, UUID actorId) {
        if (vehicleId == null) return;
        if (odometer != null) {
            recorder.record(new VehicleReadingRecorder.Command(
                    vehicleId,
                    VehicleReadingRecorder.ReadingType.ODOMETER,
                    odometer,
                    VehicleReadingRecorder.SourceType.FUEL_ISSUE,
                    fuelIssueId,
                    recordedAt,
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
                    recordedAt,
                    actorId
            ));
        }
    }
}