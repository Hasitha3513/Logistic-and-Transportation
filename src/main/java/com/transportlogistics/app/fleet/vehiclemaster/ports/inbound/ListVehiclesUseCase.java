package com.transportlogistics.app.fleet.vehiclemaster.ports.inbound;

import com.transportlogistics.app.fleet.vehiclemaster.domain.model.Vehicle;

import java.util.List;

public interface ListVehiclesUseCase {
    List<Vehicle> list();
}
