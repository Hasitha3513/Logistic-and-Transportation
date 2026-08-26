package com.transportlogistics.app.fleet.application.ports.in;

import com.transportlogistics.app.fleet.domain.model.VehicleCategory;

import java.util.List;
import java.util.UUID;

public interface VehicleCategoryUseCase {
    VehicleCategory create(VehicleCategory value);

    VehicleCategory get(UUID id);

    List<VehicleCategory> list();

    VehicleCategory update(UUID id, VehicleCategory value);

    void deactivate(UUID id);
}
