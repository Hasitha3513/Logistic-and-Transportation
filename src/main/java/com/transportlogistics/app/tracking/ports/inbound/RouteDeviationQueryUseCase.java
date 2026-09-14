package com.transportlogistics.app.tracking.ports.inbound;

import com.transportlogistics.app.tracking.domain.routedeviation.RouteDeviationEpisode;
import com.transportlogistics.app.tracking.domain.routedeviation.RouteDeviationReview;
import com.transportlogistics.app.tracking.domain.routedeviation.RouteDeviationRule;
import com.transportlogistics.app.tracking.domain.routedeviation.VehicleRouteDeviationState;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RouteDeviationQueryUseCase {
    Optional<RouteDeviationRule> rule(UUID tenantId, UUID ruleId);
    Page<RouteDeviationRule> rules(UUID tenantId, RouteDeviationRule.Lifecycle lifecycle,
                                   int page, int size);
    Optional<VehicleRouteDeviationState> state(UUID tenantId, UUID vehicleId);
    Page<VehicleRouteDeviationState> states(UUID tenantId,
                                            VehicleRouteDeviationState.State state,
                                            int page, int size);
    Optional<RouteDeviationEpisode> episode(UUID tenantId, UUID episodeId);
    CursorPage<RouteDeviationEpisode> episodes(UUID tenantId, UUID vehicleId, UUID tripId,
                                               UUID routeId, RouteDeviationEpisode.Severity severity,
                                               Boolean open, Instant from, Instant to,
                                               String cursor, int limit);
    List<RouteDeviationReview> reviews(UUID tenantId, UUID episodeId, int limit);
    record Page<T>(List<T> items, int page, int size, long total) { }
    record CursorPage<T>(List<T> items, String nextCursor) { }
}
