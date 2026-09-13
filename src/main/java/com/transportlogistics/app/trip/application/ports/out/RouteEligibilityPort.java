package com.transportlogistics.app.trip.application.ports.out;

import java.util.UUID;

public interface RouteEligibilityPort {
    String requireAssignableRouteVersion(UUID routeId, UUID originLocationId, UUID destinationLocationId);
}
