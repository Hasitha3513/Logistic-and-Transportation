package com.transportlogistics.app.delivery.domain.model;

import java.util.Objects;

public record DeliveryZoneCoordinate(double longitude, double latitude) {
    public DeliveryZoneCoordinate {
        if (Double.isNaN(longitude) || Double.isInfinite(longitude) || longitude < -180.0 || longitude > 180.0) {
            throw new IllegalArgumentException("Longitude must be between -180 and 180");
        }
        if (Double.isNaN(latitude) || Double.isInfinite(latitude) || latitude < -90.0 || latitude > 90.0) {
            throw new IllegalArgumentException("Latitude must be between -90 and 90");
        }
    }
}
