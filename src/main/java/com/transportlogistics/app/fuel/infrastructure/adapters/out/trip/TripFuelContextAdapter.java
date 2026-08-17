package com.transportlogistics.app.fuel.infrastructure.adapters.out.trip;

import com.transportlogistics.app.fuel.application.ports.out.TripFuelContextPort;
import com.transportlogistics.app.trip.TripFuelContextLookup;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class TripFuelContextAdapter implements TripFuelContextPort {
    private final TripFuelContextLookup trips;

    @Override
    public Optional<TripContext> find(UUID tripId) {
        return trips.find(tripId).map(trip -> new TripContext(trip.tripId(), trip.tripNumber(), trip.status(),
                trip.vehicleId(), trip.driverId(), trip.requestedStartTime(), trip.requestedEndTime()));
    }
}
