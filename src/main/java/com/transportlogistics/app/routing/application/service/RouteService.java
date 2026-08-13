package com.transportlogistics.app.routing.application.service;

import com.transportlogistics.app.routing.application.ports.in.RouteUseCase;
import com.transportlogistics.app.routing.application.ports.out.RouteRepository;
import com.transportlogistics.app.routing.domain.model.Route;
import com.transportlogistics.app.shared.domain.NotFoundException;

import java.util.List;
import java.util.UUID;

public final class RouteService implements RouteUseCase {
    private final RouteRepository repo;

    public RouteService(RouteRepository repo) {
        this.repo = repo;
    }

    public Route create(Route value) {
        return repo.save(value);
    }

    public Route get(UUID id) {
        return repo.findById(id).orElseThrow(() -> new NotFoundException("Route not found: " + id));
    }

    public List<Route> list() {
        return repo.findAll();
    }

    public List<Route> search(String query, UUID originLocationId, UUID destinationLocationId, Boolean active) {
        var normalizedQuery = query == null || query.isBlank() ? null : query.trim();
        if (normalizedQuery == null && originLocationId == null && destinationLocationId == null && active == null) {
            return list();
        }
        return repo.search(normalizedQuery, originLocationId, destinationLocationId, active);
    }

    public Route update(UUID id, Route value) {
        return repo.save(value);
    }

    public void deactivate(UUID id) {
        var v = get(id);
        repo.save(new Route(v.id(), v.code(), v.name(), v.originLocationId(), v.destinationLocationId(),
                v.plannedDistanceKm(), v.estimatedDurationMinutes(), false, v.stopLocationIds()));
    }
}
