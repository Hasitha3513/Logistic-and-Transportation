package com.transportlogistics.app.routing;

import java.util.Objects;
import java.util.UUID;

/**
 * Public routing-module boundary used by trip assignment orchestration.
 */
public interface RouteAssignmentLookup {
    AssignmentRoute get(UUID routeId);

    record AssignmentRoute(UUID id, UUID originLocationId, UUID destinationLocationId, boolean active,
                           String routeVersion) {
        public AssignmentRoute {
            Objects.requireNonNull(id, "Route ID is required");
            Objects.requireNonNull(originLocationId, "Origin location ID is required");
            Objects.requireNonNull(destinationLocationId, "Destination location ID is required");
            if (routeVersion == null || routeVersion.length() > 120
                    || !routeVersion.matches("REVISION:[1-9][0-9]*")) {
                throw new IllegalArgumentException("Route version must use REVISION:<positive-integer>");
            }
        }
    }
}
