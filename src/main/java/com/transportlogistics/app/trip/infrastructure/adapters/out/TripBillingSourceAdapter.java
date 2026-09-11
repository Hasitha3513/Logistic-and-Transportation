package com.transportlogistics.app.trip.infrastructure.adapters.out;

import com.transportlogistics.app.trip.TripBillingSourceLookup;
import com.transportlogistics.app.trip.application.ports.out.TripRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class TripBillingSourceAdapter implements TripBillingSourceLookup {
    private final TripRepository trips;
    TripBillingSourceAdapter(TripRepository trips) { this.trips = trips; }

    @Override public Optional<TripBillingFact> findClosed(UUID tenantId, UUID tripId) {
        return trips.findById(tripId).filter(t -> "CLOSED".equals(t.status()) && t.actualEndTime() != null)
            .map(t -> new TripBillingFact(t.id(), t.tripNumber(), t.status(), t.actualEndTime(),
                t.customerId(), Math.max(0, t.updatedAt().toInstant().toEpochMilli())));
    }
}
