package com.transportlogistics.app.trip.infrastructure.adapters.out.fleet;

import com.transportlogistics.app.fleet.VehicleReadingRecorder;
import com.transportlogistics.app.trip.application.ports.out.TripVehicleReadingPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class FleetVehicleReadingAdapter implements TripVehicleReadingPort {
    private final VehicleReadingRecorder recorder;

    @Override
    public void recordStart(UUID vehicleId, UUID tripId, Double odometerKm, Double engineHours,
                            OffsetDateTime actualStartTime, UUID actorId) {
        if (odometerKm != null) {
            recorder.record(new VehicleReadingRecorder.Command(
                    vehicleId,
                    VehicleReadingRecorder.ReadingType.ODOMETER,
                    BigDecimal.valueOf(odometerKm),
                    VehicleReadingRecorder.SourceType.TRIP_START,
                    tripId,
                    actualStartTime,
                    actorId
            ));
        }
        if (engineHours != null) {
            recorder.record(new VehicleReadingRecorder.Command(
                    vehicleId,
                    VehicleReadingRecorder.ReadingType.ENGINE_HOURS,
                    BigDecimal.valueOf(engineHours),
                    VehicleReadingRecorder.SourceType.TRIP_START,
                    tripId,
                    actualStartTime,
                    actorId
            ));
        }
    }

    @Override
    public void recordComplete(UUID vehicleId, UUID tripId, Double odometerKm, Double engineHours,
                               OffsetDateTime actualEndTime, UUID actorId) {
        if (odometerKm != null) {
            recorder.record(new VehicleReadingRecorder.Command(
                    vehicleId,
                    VehicleReadingRecorder.ReadingType.ODOMETER,
                    BigDecimal.valueOf(odometerKm),
                    VehicleReadingRecorder.SourceType.TRIP_END,
                    tripId,
                    actualEndTime,
                    actorId
            ));
        }
        if (engineHours != null) {
            recorder.record(new VehicleReadingRecorder.Command(
                    vehicleId,
                    VehicleReadingRecorder.ReadingType.ENGINE_HOURS,
                    BigDecimal.valueOf(engineHours),
                    VehicleReadingRecorder.SourceType.TRIP_END,
                    tripId,
                    actualEndTime,
                    actorId
            ));
        }
    }
}
