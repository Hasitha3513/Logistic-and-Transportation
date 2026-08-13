package com.transportlogistics.app.fleet.domain.model;

public record VehicleAvailability(boolean available, String reason) {
    public static VehicleAvailability eligible() {
        return new VehicleAvailability(true, "AVAILABLE");
    }

    public static VehicleAvailability unavailable(String reason) {
        return new VehicleAvailability(false, reason);
    }
}
