package com.transportlogistics.app.fleet.application.ports.out;

import com.transportlogistics.app.fleet.domain.model.VehicleType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VehicleTypeRepository {
    VehicleType save(VehicleType value);

    Optional<VehicleType> findById(UUID id);

    List<VehicleType> findAll();
}
