package com.transportlogistics.app.tracking.adapters.outbound.trip;

import com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardModels.TripContext;
import com.transportlogistics.app.tracking.ports.outbound.TrackingDashboardTripContextPort;
import com.transportlogistics.app.trip.TripDashboardQuery;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
final class TripTrackingDashboardAdapter implements TrackingDashboardTripContextPort {
    private final TripDashboardQuery trips;

    TripTrackingDashboardAdapter(TripDashboardQuery trips) {
        this.trips = trips;
    }

    @Override
    public List<TripContext> findActiveContexts(
            UUID tenantId, Set<UUID> vehicleIds, Instant evaluatedAt) {
        return trips.findActiveContexts(tenantId, vehicleIds, evaluatedAt).stream()
                .map(value -> new TripContext(value.vehicleId(), value.tripId(), value.lifecycle(),
                        value.routeId(), parseVersion(value.routeVersion())))
                .toList();
    }

    private static Long parseVersion(String value) {
        if (value == null) {
            return null;
        }
        String numeric = value.startsWith("REVISION:") ? value.substring("REVISION:".length()) : value;
        try {
            return Long.valueOf(numeric);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
