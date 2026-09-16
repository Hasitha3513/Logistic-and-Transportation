package com.transportlogistics.app.tracking.ports.outbound;

import com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardModels.DashboardFilter;
import java.time.Instant;
import java.util.UUID;

public interface TrackingDashboardCursorPort {
    String encode(CursorState state);

    CursorState decode(UUID tenantId, DashboardFilter filter, String cursor, Instant now);

    record CursorState(UUID tenantId, DashboardFilter filter, UUID afterVehicleId,
                       Instant evaluatedAt, Instant expiresAt) {
    }
}
