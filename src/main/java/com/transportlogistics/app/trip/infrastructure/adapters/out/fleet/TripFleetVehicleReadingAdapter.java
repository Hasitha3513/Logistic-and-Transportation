package com.transportlogistics.app.trip.infrastructure.adapters.out.fleet;

import com.transportlogistics.app.fleet.VehicleReadingRecorder;
import com.transportlogistics.app.trip.application.ports.out.TripVehicleReadingPort;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Component
class TripFleetVehicleReadingAdapter implements TripVehicleReadingPort {
    private final VehicleReadingRecorder recorder;

    TripFleetVehicleReadingAdapter(VehicleReadingRecorder recorder) {
        this.recorder = recorder;
    }

    @Override
    public void recordTripStart(UUID vehicleId, UUID tripId, Double odometerKm, OffsetDateTime occurredAt, UUID actorId) {
        if (vehicleId == null || odometerKm == null) return;
        recorder.record(new VehicleReadingRecorder.Command(
                vehicleId,
                VehicleReadingRecorder.ReadingType.ODOMETER,
                BigDecimal.valueOf(odometerKm),
                VehicleReadingRecorder.SourceType.TRIP_START,
                tripId,
                occurredAt,
                actorId
        ));
    }

    @Override
    public void recordTripEnd(UUID vehicleId, UUID tripId, Double odometerKm, OffsetDateTime occurredAt, UUID actorId) {
        if (vehicleId == null || odometerKm == null) return;
        recorder.record(new VehicleReadingRecorder.Command(
                vehicleId,
                VehicleReadingRecorder.ReadingType.ODOMETER,
                BigDecimal.valueOf(odometerKm),
                VehicleReadingRecorder.SourceType.TRIP_END,
                tripId,
                occurredAt,
                actorId
        ));
    }
}