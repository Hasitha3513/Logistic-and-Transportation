package com.transportlogistics.app.routing.domain.model;

import java.util.Objects;
import java.util.UUID;

public record RouteLocation(
        UUID id,
        String code,
        String name,
        Double latitude,
        Double longitude
) {
    public RouteLocation {
        Objects.requireNonNull(id, "Location id is required");
    }

    public boolean hasCoordinates() {
        return latitude != null && longitude != null;
    }
}
