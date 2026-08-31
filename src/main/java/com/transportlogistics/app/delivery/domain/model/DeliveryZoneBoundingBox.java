package com.transportlogistics.app.delivery.domain.model;

public record DeliveryZoneBoundingBox(
        double minLatitude,
        double maxLatitude,
        double minLongitude,
        double maxLongitude
) {
    public boolean contains(double longitude, double latitude) {
        return latitude >= minLatitude && latitude <= maxLatitude
                && longitude >= minLongitude && longitude <= maxLongitude;
    }
}
