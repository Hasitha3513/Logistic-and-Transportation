package com.transportlogistics.app.fleet.domain.model;

public record DriverAvailability(boolean available, String reason) {
    public static DriverAvailability eligible() {
        return new DriverAvailability(true, "AVAILABLE");
    }

    public static DriverAvailability unavailable(String reason) {
        return new DriverAvailability(false, reason);
    }
}
