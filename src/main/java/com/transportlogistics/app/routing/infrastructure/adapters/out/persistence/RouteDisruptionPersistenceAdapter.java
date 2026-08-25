package com.transportlogistics.app.routing.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.routing.application.ports.out.RouteDisruptionRepository;
import com.transportlogistics.app.routing.domain.model.DisruptionStatus;
import com.transportlogistics.app.routing.domain.model.RouteDisruption;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class RouteDisruptionPersistenceAdapter implements RouteDisruptionRepository {
    private final RouteDisruptionJpaRepository repo;

    @Override
    public RouteDisruption save(RouteDisruption d) {
        var e = new RouteDisruptionEntity(
                d.id(),
                d.routeId(),
                d.disruptionType(),
                d.severity(),
                d.description(),
                d.effectiveFrom(),
                d.effectiveUntil(),
                d.detourRouteId(),
                d.status(),
                d.createdAt(),
                d.createdBy(),
                d.resolvedAt(),
                d.resolvedBy()
        );
        return map(repo.save(e));
    }

    @Override
    public Optional<RouteDisruption> findById(UUID id) {
        return repo.findById(id).map(this::map);
    }

    @Override
    public List<RouteDisruption> findByRouteId(UUID routeId) {
        return repo.findByRouteIdOrderByCreatedAtDesc(routeId).stream().map(this::map).toList();
    }

    @Override
    public List<RouteDisruption> findByStatus(DisruptionStatus status) {
        return repo.findByStatusOrderByCreatedAtDesc(status).stream().map(this::map).toList();
    }

    private RouteDisruption map(RouteDisruptionEntity e) {
        return new RouteDisruption(
                e.getId(),
                e.getRouteId(),
                e.getDisruptionType(),
                e.getSeverity(),
                e.getDescription(),
                e.getEffectiveFrom(),
                e.getEffectiveUntil(),
                e.getDetourRouteId(),
                e.getStatus(),
                e.getCreatedAt(),
                e.getCreatedBy(),
                e.getResolvedAt(),
                e.getResolvedBy()
        );
    }
}
