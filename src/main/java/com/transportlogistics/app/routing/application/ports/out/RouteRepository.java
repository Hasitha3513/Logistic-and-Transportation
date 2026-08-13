package com.transportlogistics.app.routing.application.ports.out;

import com.transportlogistics.app.routing.domain.model.Route;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RouteRepository {
    Route save(Route value);

    Optional<Route> findById(UUID id);

    List<Route> findAll();

    List<Route> search(String query, UUID originLocationId, UUID destinationLocationId, Boolean active);
}
