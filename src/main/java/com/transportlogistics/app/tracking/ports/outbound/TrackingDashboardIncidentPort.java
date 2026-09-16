package com.transportlogistics.app.tracking.ports.outbound;

import com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardModels.Incident;
import com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardModels.IncidentType;
import com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardModels.SourceStatus;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface TrackingDashboardIncidentPort {
    IncidentResult find(UUID tenantId, Set<UUID> vehicleIds, Set<IncidentType> types,
                        Instant fromInclusive, Instant toExclusive, int maximumPerProducer, int maximumTotal);

    record IncidentResult(List<Incident> items, java.util.Map<IncidentType, SourceStatus> sourceStatuses) {
        public IncidentResult {
            items = List.copyOf(items);
            sourceStatuses = java.util.Map.copyOf(sourceStatuses);
        }
    }
}
