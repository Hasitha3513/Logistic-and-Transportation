package com.transportlogistics.app.routing.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.routing.application.ports.out.RouteRevisionRepository;
import com.transportlogistics.app.routing.domain.model.RouteRevision;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class RouteRevisionPersistenceAdapter implements RouteRevisionRepository {
    private final RouteRevisionJpaRepository repo;

    @Override
    public RouteRevision save(RouteRevision r) {
        var e = new RouteRevisionEntity(
                r.id(),
                r.routeId(),
                r.revisionNumber(),
                r.code(),
                r.name(),
                r.originLocationId(),
                r.destinationLocationId(),
                r.plannedDistanceKm(),
                r.estimatedDurationMinutes(),
                r.active(),
                r.stopLocationIds(),
                r.changedAt(),
                r.changedBy()
        );
        return map(repo.save(e));
    }

    @Override
    public List<RouteRevision> findByRouteIdOrderByRevisionNumberDesc(UUID routeId) {
        return repo.findByRouteIdOrderByRevisionNumberDesc(routeId).stream().map(this::map).toList();
    }

    @Override
    public Optional<RouteRevision> findByRouteIdAndRevisionNumber(UUID routeId, int revisionNumber) {
        return repo.findByRouteIdAndRevisionNumber(routeId, revisionNumber).map(this::map);
    }

    @Override
    public int findLatestRevisionNumber(UUID routeId) {
        return repo.findMaxRevisionNumber(routeId);
    }

    private RouteRevision map(RouteRevisionEntity e) {
        return new RouteRevision(
                e.getId(),
                e.getRouteId(),
                e.getRevisionNumber(),
                e.getCode(),
                e.getName(),
                e.getOriginLocationId(),
                e.getDestinationLocationId(),
                e.getPlannedDistanceKm(),
                e.getEstimatedDurationMinutes(),
                e.isActive(),
                e.getStopLocationIds(),
                e.getChangedAt(),
                e.getChangedBy()
        );
    }
}
