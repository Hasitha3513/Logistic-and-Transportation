package com.transportlogistics.app.trip.application.ports.out;

import com.transportlogistics.app.trip.TripDashboardQuery.ActiveTripContext;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface TripDashboardRepository {
    List<ActiveTripContext> findActiveContexts(
            UUID tenantId, Set<UUID> vehicleIds, Instant evaluatedAt);
}
