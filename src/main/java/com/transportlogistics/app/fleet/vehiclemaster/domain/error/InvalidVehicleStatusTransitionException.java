package com.transportlogistics.app.fleet.vehiclemaster.domain.error;

public final class InvalidVehicleStatusTransitionException extends RuntimeException {

    public InvalidVehicleStatusTransitionException(String currentStatus, String nextStatus) {
        super("Invalid vehicle status transition from " + currentStatus + " to " + nextStatus);
    }

    public String code() {
        return "VEHICLE_STATUS_TRANSITION_INVALID";
    }
}
