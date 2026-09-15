package com.transportlogistics.app.tracking.ports.outbound;

import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.Coordinate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JourneyReplayRouteContextPort {
    Optional<List<Coordinate>> findExact(UUID tenantId, UUID routeId, String routeVersion);
}
