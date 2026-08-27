package com.transportlogistics.app.fleet.vehiclemaster.ports.inbound;

import com.transportlogistics.app.fleet.vehiclemaster.domain.model.Vehicle;

import java.util.UUID;

public interface GetVehicleUseCase {
    Vehicle get(UUID id);
}
