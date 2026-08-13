package com.transportlogistics.app.routing.application.ports.in;

import com.transportlogistics.app.routing.domain.model.Route;

import java.util.List;
import java.util.UUID;

public interface RouteUseCase {
    Route create(Route value);

    Route get(UUID id);

    List<Route> list();

    Route update(UUID id, Route value);

    void deactivate(UUID id);
}
