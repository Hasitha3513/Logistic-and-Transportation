package com.transportlogistics.app.fleet.application.ports.out;

import com.transportlogistics.app.fleet.domain.model.VehicleMeterReset;
import com.transportlogistics.app.fleet.domain.model.VehicleReadingType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VehicleMeterResetRepository {
    VehicleMeterReset save(VehicleMeterReset reset);

    Optional<VehicleMeterReset> findById(UUID resetId);

    List<VehicleMeterReset> findByVehicleId(UUID vehicleId);

    Optional<VehicleMeterReset> findLatestByVehicleIdAndReadingType(UUID vehicleId, VehicleReadingType readingType);
}
