package com.transportlogistics.app.tracking.ports.outbound;

import com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardModels.DashboardFilter;
import com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardModels.LiveVehicleState;
import com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardModels.SourceStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface TrackingDashboardLiveStatePort {
    LiveStatePage find(UUID tenantId, DashboardFilter filter, UUID afterVehicleId, int limit, Instant evaluatedAt);

    record LiveStatePage(List<LiveVehicleState> items, long matchingVehicleCount, boolean hasMore,
                         SourceStatus sourceStatus, Instant lastSuccessfulRefreshAt) {
        public LiveStatePage {
            items = List.copyOf(items);
        }
    }
}
