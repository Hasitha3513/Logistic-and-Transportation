package com.transportlogistics.app.routing.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.routing.application.ports.out.RouteRepository;
import com.transportlogistics.app.routing.domain.model.Route;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class RoutePersistenceAdapter implements RouteRepository {
    private final RouteJpaRepository repo;

    public Route save(Route v) {
        var e = new RouteEntity();
        e.setId(v.id());
        e.setCode(v.code());
        e.setName(v.name());
        e.setOriginLocationId(v.originLocationId());
        e.setDestinationLocationId(v.destinationLocationId());
        e.setPlannedDistanceKm(v.plannedDistanceKm());
        e.setEstimatedDurationMinutes(v.estimatedDurationMinutes());
        e.setActive(v.active());
        e.setStopLocationIds(v.stopLocationIds());
        return map(repo.save(e));
    }

    public Optional<Route> findById(UUID id) {
        return repo.findById(id).map(this::map);
    }

    public List<Route> findAll() {
        return repo.findAll().stream().map(this::map).toList();
    }

    public List<Route> search(String query, UUID originLocationId, UUID destinationLocationId, Boolean active) {
        return repo.search(query, originLocationId, destinationLocationId, active).stream().map(this::map).toList();
    }

    private Route map(RouteEntity e) {
        return new Route(e.getId(), e.getCode(), e.getName(), e.getOriginLocationId(), e.getDestinationLocationId(),
                e.getPlannedDistanceKm(), e.getEstimatedDurationMinutes(), e.isActive(), e.getStopLocationIds());
    }
}
