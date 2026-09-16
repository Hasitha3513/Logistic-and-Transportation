package com.transportlogistics.app.tracking.adapters.inbound.web.dto.request;

import com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardModels.Connectivity;
import com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardModels.Freshness;
import com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardModels.IncidentType;
import com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardModels.Motion;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.Set;
import java.util.UUID;

public final class TrackingDashboardRequests {
    private TrackingDashboardRequests() { }

    public record Query(
            @Size(max = 100) Set<UUID> vehicleIds,
            Set<Freshness> freshness,
            Set<Connectivity> connectivity,
            Set<Motion> motion,
            Set<IncidentType> incidentTypes,
            Boolean includeHeatMap,
            Boolean includeIncidents,
            String cursor,
            @Min(1) @Max(100) Integer pageSize) { }
}
