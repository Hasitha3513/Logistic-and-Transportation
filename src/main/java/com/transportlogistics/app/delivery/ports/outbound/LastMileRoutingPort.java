package com.transportlogistics.app.delivery.ports.outbound;

import com.transportlogistics.app.delivery.domain.model.DeliveryTransportMode;
import com.transportlogistics.app.delivery.domain.model.DeliveryZoneType;
import com.transportlogistics.app.delivery.domain.model.EtaSource;

import java.time.OffsetDateTime;

public interface LastMileRoutingPort {

    RouteEstimate estimate(
            Coordinate origin,
            Coordinate destination,
            DeliveryTransportMode mode,
            DeliveryZoneType zoneType,
            OffsetDateTime departureTime
    );

    record Coordinate(double latitude, double longitude) {
        public Coordinate {
            if (latitude < -90.0 || latitude > 90.0) {
                throw new IllegalArgumentException("Latitude must be between -90 and 90");
            }
            if (longitude < -180.0 || longitude > 180.0) {
                throw new IllegalArgumentException("Longitude must be between -180 and 180");
            }
        }
    }

    record RouteEstimate(
            long distanceMeters,
            long durationSeconds,
            EtaSource source
    ) {
    }
}
