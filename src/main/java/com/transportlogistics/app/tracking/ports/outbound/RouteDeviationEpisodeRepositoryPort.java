package com.transportlogistics.app.tracking.ports.outbound;

import com.transportlogistics.app.tracking.domain.routedeviation.RouteDeviationEpisode;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RouteDeviationEpisodeRepositoryPort {
    RouteDeviationEpisode save(RouteDeviationEpisode episode);
    Optional<RouteDeviationEpisode> find(UUID tenantId, UUID episodeId);
    default Optional<RouteDeviationEpisode> lockAndFind(UUID tenantId, UUID episodeId) {
        return find(tenantId, episodeId);
    }
    Optional<RouteDeviationEpisode> findOpen(UUID tenantId, UUID vehicleId);
    List<RouteDeviationEpisode> history(UUID tenantId, UUID vehicleId, Instant from,
                                        Instant to, String cursor, int limit);
    default List<RouteDeviationEpisode> search(UUID tenantId, UUID vehicleId, UUID tripId,
            UUID routeId, RouteDeviationEpisode.Severity severity, Boolean open,
            Instant from, Instant to, Instant cursorTime, UUID cursorId, int limit) {
        return vehicleId == null ? List.of() : history(tenantId, vehicleId, from, to, null, limit);
    }
}
