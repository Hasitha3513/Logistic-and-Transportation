package com.transportlogistics.app.fleet.domain.model;

public enum VehicleReadingType {
    ODOMETER(VehicleReadingUnit.KILOMETER),
    ENGINE_HOURS(VehicleReadingUnit.HOUR);

    private final VehicleReadingUnit unit;

    VehicleReadingType(VehicleReadingUnit unit) {
        this.unit = unit;
    }

    public VehicleReadingUnit unit() {
        return unit;
    }
}
