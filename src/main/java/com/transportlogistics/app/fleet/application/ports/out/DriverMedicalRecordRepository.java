package com.transportlogistics.app.fleet.application.ports.out;

import com.transportlogistics.app.fleet.domain.model.DriverMedicalRecord;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DriverMedicalRecordRepository {

    DriverMedicalRecord save(DriverMedicalRecord record);

    List<DriverMedicalRecord> findByDriverId(UUID driverId);

    Optional<DriverMedicalRecord> findById(UUID id);

    Optional<DriverMedicalRecord> findLatestByDriverId(UUID driverId);
}
