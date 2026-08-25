package com.transportlogistics.app.routing.application.ports.in;

import com.transportlogistics.app.routing.domain.model.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface RouteUseCase {
    Route create(Route value, String actor);

    default Route create(Route value) {
        return create(value, "system");
    }

    Route get(UUID id);

    List<Route> list();

    List<Route> search(String query, UUID originLocationId, UUID destinationLocationId, Boolean active);

    Route update(UUID id, Route value, String actor);

    default Route update(UUID id, Route value) {
        return update(id, value, "system");
    }

    void deactivate(UUID id, String actor);

    default void deactivate(UUID id) {
        deactivate(id, "system");
    }

    // US-21: Route Revisions
    List<RouteRevision> getRevisions(UUID routeId);

    RouteRevision getRevision(UUID routeId, int revisionNumber);

    // US-23: Route Disruptions
    RouteDisruption createDisruption(UUID routeId, RouteDisruptionType type, DisruptionSeverity severity,
                                    String description, OffsetDateTime effectiveFrom, OffsetDateTime effectiveUntil,
                                    UUID detourRouteId, String actor);

    RouteDisruption resolveDisruption(UUID routeId, UUID disruptionId, String actor);

    List<RouteDisruption> getDisruptions(UUID routeId);

    List<RouteDisruption> getActiveDisruptions();

    // US-20: Route Optimization
    RouteOptimizationResult optimizeRoute(UUID routeId);

    Route applyOptimization(UUID routeId, List<UUID> optimizedStopLocationIds, String actor);

    default Route applyOptimization(UUID routeId, List<UUID> optimizedStopLocationIds) {
        return applyOptimization(routeId, optimizedStopLocationIds, "system");
    }

    // US-22: Route Performance Analytics
    RoutePerformanceAnalytics getRoutePerformance(UUID routeId, OffsetDateTime from, OffsetDateTime to);
}
