package com.transportlogistics.app.fleet.application.ports.out;

import com.transportlogistics.app.fleet.domain.model.Vehicle;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VehicleRepository {
    Vehicle save(Vehicle value);

    Optional<Vehicle> findById(UUID id);

    Optional<Vehicle> findByIdForUpdate(UUID id);

    List<Vehicle> findAll();
}
