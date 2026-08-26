package com.transportlogistics.app.fleet.application.ports.out;

import com.transportlogistics.app.fleet.domain.model.VehicleCategory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VehicleCategoryRepository {
    VehicleCategory save(VehicleCategory value);

    Optional<VehicleCategory> findById(UUID id);

    List<VehicleCategory> findAll();
}
