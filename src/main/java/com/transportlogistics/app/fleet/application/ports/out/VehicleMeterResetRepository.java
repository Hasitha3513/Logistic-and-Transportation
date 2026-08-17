package com.transportlogistics.app.fleet.application.ports.out;

import com.transportlogistics.app.fleet.domain.model.VehicleMeterReset;
import com.transportlogistics.app.fleet.domain.model.VehicleReadingType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VehicleMeterResetRepository {
    VehicleMeterReset save(VehicleMeterReset reset);
    List<VehicleMeterReset> findByVehicleId(UUID vehicleId);
    List<VehicleMeterReset> findByVehicleIdAndType(UUID vehicleId, VehicleReadingType readingType);
    Optional<VehicleMeterReset> findLatestByVehicleIdAndType(UUID vehicleId, VehicleReadingType readingType);
}