package com.transportlogistics.app.routing.application.ports.out;

import com.transportlogistics.app.routing.domain.model.DisruptionStatus;
import com.transportlogistics.app.routing.domain.model.RouteDisruption;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RouteDisruptionRepository {
    RouteDisruption save(RouteDisruption disruption);

    Optional<RouteDisruption> findById(UUID id);

    List<RouteDisruption> findByRouteId(UUID routeId);

    List<RouteDisruption> findByStatus(DisruptionStatus status);
}
