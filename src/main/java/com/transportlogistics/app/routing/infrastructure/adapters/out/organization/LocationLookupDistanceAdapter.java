package com.transportlogistics.app.routing.infrastructure.adapters.out.organization;

import com.transportlogistics.app.organization.LocationLookup;
import com.transportlogistics.app.routing.application.ports.out.RouteDistancePort;
import com.transportlogistics.app.routing.domain.model.RouteLocation;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
public class LocationLookupDistanceAdapter implements RouteDistancePort {
    private static final double EARTH_RADIUS_KM = 6371.0;

    private final LocationLookup locationLookup;

    public LocationLookupDistanceAdapter(LocationLookup locationLookup) {
        this.locationLookup = Objects.requireNonNull(locationLookup, "LocationLookup is required");
    }

    @Override
    public Optional<RouteLocation> getLocation(UUID locationId) {
        return locationLookup.find(locationId).map(loc -> new RouteLocation(
                loc.id(),
                loc.code(),
                loc.name(),
                loc.latitude(),
                loc.longitude()
        ));
    }

    @Override
    public double getDistanceKm(UUID fromLocationId, UUID toLocationId) {
        if (Objects.equals(fromLocationId, toLocationId)) {
            return 0.0;
        }

        var from = getLocation(fromLocationId).orElseThrow(() -> new BusinessRuleException(
                "ROUTE_OPTIMIZATION_DATA_UNAVAILABLE", "Location not found: " + fromLocationId));
        var to = getLocation(toLocationId).orElseThrow(() -> new BusinessRuleException(
                "ROUTE_OPTIMIZATION_DATA_UNAVAILABLE", "Location not found: " + toLocationId));
        if (!from.hasCoordinates() || !to.hasCoordinates()) {
            throw new BusinessRuleException("ROUTE_OPTIMIZATION_DATA_UNAVAILABLE",
                    "Coordinates are required for every route location");
        }
        return haversineKm(from.latitude(), from.longitude(), to.latitude(), to.longitude());
    }

    public static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2.0) * Math.sin(dLat / 2.0)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2.0) * Math.sin(dLon / 2.0);
        double c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));
        return Math.round(EARTH_RADIUS_KM * c * 100.0) / 100.0;
    }
}
