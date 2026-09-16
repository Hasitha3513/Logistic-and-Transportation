package com.transportlogistics.app.trip;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Published, Tenant-qualified active Trip facts for bounded operational dashboards. */
public interface TripDashboardQuery {
    int MAXIMUM_VEHICLES = 100;

    List<ActiveTripContext> findActiveContexts(
            UUID tenantId, Set<UUID> vehicleIds, Instant evaluatedAt);

    record ActiveTripContext(UUID vehicleId, UUID tripId, String lifecycle,
                             UUID routeId, String routeVersion) {
    }
}
