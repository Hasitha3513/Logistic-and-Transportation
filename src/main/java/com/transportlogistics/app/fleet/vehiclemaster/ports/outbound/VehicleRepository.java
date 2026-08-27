package com.transportlogistics.app.fleet.vehiclemaster.ports.outbound;

import com.transportlogistics.app.fleet.vehiclemaster.domain.model.Vehicle;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VehicleRepository {
    Vehicle save(Vehicle value);

    Optional<Vehicle> findById(UUID id);

    Optional<Vehicle> findByIdForUpdate(UUID id);

    List<Vehicle> findAll();

    Optional<Vehicle> findByRegistrationNumber(String registrationNumber);

    Optional<Vehicle> findByChassisNumber(String chassisNumber);

    Optional<Vehicle> findByEngineNumber(String engineNumber);

    boolean existsByRegistrationNumberAndIdNot(String registrationNumber, UUID id);

    boolean existsByChassisNumberAndIdNot(String chassisNumber, UUID id);

    boolean existsByEngineNumberAndIdNot(String engineNumber, UUID id);
}
