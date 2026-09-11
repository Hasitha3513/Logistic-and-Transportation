package com.transportlogistics.app.tracking.domain.speed;

import java.util.UUID;

public record SpeedAttribution(UUID tripId, UUID driverId, UUID routeId, String routeVersion) {
    public static SpeedAttribution unknown() {
        return new SpeedAttribution(null, null, null, null);
    }

    public boolean matchesRoute(UUID expectedRouteId, String expectedRouteVersion) {
        return routeId != null && routeId.equals(expectedRouteId)
                && routeVersion != null && routeVersion.equals(expectedRouteVersion);
    }
}
