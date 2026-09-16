package com.transportlogistics.app.tracking.ports.outbound;

import com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardModels.TripContext;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface TrackingDashboardTripContextPort {
    List<TripContext> findActiveContexts(UUID tenantId, Set<UUID> vehicleIds, Instant evaluatedAt);
}
