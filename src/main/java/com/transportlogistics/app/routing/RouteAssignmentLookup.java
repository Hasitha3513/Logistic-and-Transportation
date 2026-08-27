package com.transportlogistics.app.routing;

import java.util.UUID;

/**
 * Public routing-module boundary used by trip assignment orchestration.
 */
public interface RouteAssignmentLookup {
    AssignmentRoute get(UUID routeId);

    record AssignmentRoute(UUID id, UUID originLocationId, UUID destinationLocationId, boolean active) {
    }
}
