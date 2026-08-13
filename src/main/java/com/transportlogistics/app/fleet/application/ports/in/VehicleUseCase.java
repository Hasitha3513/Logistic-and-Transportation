package com.transportlogistics.app.fleet.application.ports.in;

import com.transportlogistics.app.fleet.domain.model.Vehicle;
import com.transportlogistics.app.fleet.domain.model.VehicleAvailability;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface VehicleUseCase {
    Vehicle create(Vehicle value);

    Vehicle get(UUID id);

    List<Vehicle> list();

    Vehicle update(UUID id, Vehicle value);

    void deactivate(UUID id);

    VehicleAvailability availability(UUID id, LocalDate onDate);

    void assertAvailableForDispatch(UUID id, LocalDate onDate);
}
