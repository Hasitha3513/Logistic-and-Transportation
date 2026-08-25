package com.transportlogistics.app.routing.application.ports.out;

import com.transportlogistics.app.routing.domain.model.RouteRevision;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RouteRevisionRepository {
    RouteRevision save(RouteRevision revision);

    List<RouteRevision> findByRouteIdOrderByRevisionNumberDesc(UUID routeId);

    Optional<RouteRevision> findByRouteIdAndRevisionNumber(UUID routeId, int revisionNumber);

    int findLatestRevisionNumber(UUID routeId);
}
