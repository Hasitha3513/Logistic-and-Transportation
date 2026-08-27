package com.transportlogistics.app.fleet.vehiclemaster.ports.inbound;

import com.transportlogistics.app.fleet.vehiclemaster.domain.model.Vehicle;

public interface CreateVehicleUseCase {
    Vehicle create(Vehicle value);
}
