package com.transportlogistics.app.fleet.application.ports.in;

import com.transportlogistics.app.fleet.domain.model.Vehicle;

import java.util.List;
import java.util.UUID;

public interface VehicleUseCase {
    Vehicle create(Vehicle value);

    Vehicle get(UUID id);

    List<Vehicle> list();

    Vehicle update(UUID id, Vehicle value);

    void deactivate(UUID id);

}
