package com.transportlogistics.app.fleet.application.ports.out;

import com.transportlogistics.app.fleet.domain.model.DriverDrugTest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DriverDrugTestRepository {

    DriverDrugTest save(DriverDrugTest test);

    List<DriverDrugTest> findByDriverId(UUID driverId);

    Optional<DriverDrugTest> findById(UUID id);

    List<DriverDrugTest> findActiveByDriverId(UUID driverId);

    Optional<DriverDrugTest> findLatestByDriverId(UUID driverId);
}
