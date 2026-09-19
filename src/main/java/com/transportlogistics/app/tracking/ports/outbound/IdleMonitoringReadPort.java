package com.transportlogistics.app.tracking.ports.outbound;

import com.transportlogistics.app.tracking.ports.inbound.IdleMonitoringQuery;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IdleMonitoringReadPort {
    List<IdleMonitoringQuery.State> states(UUID tenantId, IdleMonitoringQuery.StateFilter filter,
            Instant afterTimestamp, UUID afterVehicleId, int limit);
    List<IdleMonitoringQuery.Episode> episodes(UUID tenantId, IdleMonitoringQuery.EpisodeFilter filter,
            Instant afterTimestamp, UUID afterId, int limit);
    Optional<IdleMonitoringQuery.Episode> episode(UUID tenantId, UUID episodeId);
    List<IdleMonitoringQuery.Evidence> evidence(UUID tenantId, UUID episodeId,
            Instant afterTimestamp, UUID afterId, int limit);
}
