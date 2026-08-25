package com.transportlogistics.app.routing.application.ports.out;

import com.transportlogistics.app.routing.domain.model.RouteLocation;

import java.util.Optional;
import java.util.UUID;

public interface RouteDistancePort {

    Optional<RouteLocation> getLocation(UUID locationId);

    double getDistanceKm(UUID fromLocationId, UUID toLocationId);
}
