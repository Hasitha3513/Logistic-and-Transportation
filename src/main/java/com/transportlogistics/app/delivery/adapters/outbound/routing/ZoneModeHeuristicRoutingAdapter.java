package com.transportlogistics.app.delivery.adapters.outbound.routing;

import com.transportlogistics.app.delivery.domain.model.DeliveryTransportMode;
import com.transportlogistics.app.delivery.domain.model.DeliveryZoneType;
import com.transportlogistics.app.delivery.domain.model.EtaSource;
import com.transportlogistics.app.delivery.ports.outbound.LastMileRoutingPort;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Component
public class ZoneModeHeuristicRoutingAdapter implements LastMileRoutingPort {

    private static final double EARTH_RADIUS_METERS = 6371000.0;
    private static final double URBAN_CIRCUITY_FACTOR = 1.3;

    @Override
    public RouteEstimate estimate(
            Coordinate origin,
            Coordinate destination,
            DeliveryTransportMode mode,
            DeliveryZoneType zoneType,
            OffsetDateTime departureTime
    ) {
        if (origin == null || destination == null) {
            throw new IllegalArgumentException("Origin and destination coordinates are required");
        }

        double haversineDistanceMeters = calculateHaversineDistanceMeters(
                origin.latitude(), origin.longitude(),
                destination.latitude(), destination.longitude()
        );

        long roadDistanceMeters = Math.round(haversineDistanceMeters * URBAN_CIRCUITY_FACTOR);

        double speedKmPerHour = resolveSpeedKmPerHour(mode, zoneType);
        double speedMetersPerSecond = speedKmPerHour * 1000.0 / 3600.0;

        long travelDurationSeconds = speedMetersPerSecond > 0
                ? Math.round(roadDistanceMeters / speedMetersPerSecond)
                : 0L;

        return new RouteEstimate(roadDistanceMeters, travelDurationSeconds, EtaSource.HEURISTIC);
    }

    public static double calculateHaversineDistanceMeters(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double originLatRad = Math.toRadians(lat1);
        double destLatRad = Math.toRadians(lat2);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(originLatRad) * Math.cos(destLatRad)
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_METERS * c;
    }

    public static double resolveSpeedKmPerHour(DeliveryTransportMode mode, DeliveryZoneType zoneType) {
        DeliveryTransportMode effectiveMode = mode != null ? mode : DeliveryTransportMode.MOTORBIKE;
        DeliveryZoneType effectiveZone = zoneType != null ? zoneType : DeliveryZoneType.URBAN_DENSE;

        return switch (effectiveMode) {
            case BICYCLE -> switch (effectiveZone) {
                case URBAN_DENSE -> 15.0;
                case SUBURBAN -> 18.0;
                case RURAL -> 20.0;
                case SPECIAL_SECURITY -> 12.0;
            };
            case MOTORBIKE -> switch (effectiveZone) {
                case URBAN_DENSE -> 25.0;
                case SUBURBAN -> 40.0;
                case RURAL -> 50.0;
                case SPECIAL_SECURITY -> 20.0;
            };
            case VAN, CAR -> switch (effectiveZone) {
                case URBAN_DENSE -> 20.0;
                case SUBURBAN -> 45.0;
                case RURAL -> 60.0;
                case SPECIAL_SECURITY -> 15.0;
            };
            case WALKER -> switch (effectiveZone) {
                case URBAN_DENSE, SUBURBAN, RURAL, SPECIAL_SECURITY -> 5.0;
            };
        };
    }
}
