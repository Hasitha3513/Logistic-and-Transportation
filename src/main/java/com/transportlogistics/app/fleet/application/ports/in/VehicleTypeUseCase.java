package com.transportlogistics.app.fleet.application.ports.in;

import com.transportlogistics.app.fleet.domain.model.VehicleType;

import java.util.List;
import java.util.UUID;

public interface VehicleTypeUseCase {
    VehicleType create(VehicleType value);

    VehicleType get(UUID id);

    List<VehicleType> list();

    VehicleType update(UUID id, VehicleType value);

    void deactivate(UUID id);
}
