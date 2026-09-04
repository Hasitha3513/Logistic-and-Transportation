package com.transportlogistics.app.fleet.vehiclemaster.adapters.outbound.persistence;

import com.transportlogistics.app.fleet.vehiclemaster.ports.outbound.VehicleRepository;
import com.transportlogistics.app.fleet.vehiclemaster.domain.model.Vehicle;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Set;

@Component
class VehiclePersistenceAdapter implements VehicleRepository {
    private final VehicleJpaRepository repo;
    private final VehiclePersistenceMapper mapper;

    VehiclePersistenceAdapter(VehicleJpaRepository repo, VehiclePersistenceMapper mapper) {
        this.repo = repo;
        this.mapper = mapper;
    }

    public Vehicle save(Vehicle v) {
        return mapper.toDomain(repo.save(mapper.toEntity(v)));
    }

    public Optional<Vehicle> findById(UUID id) {
        return repo.findById(id).map(mapper::toDomain);
    }

    public Optional<Vehicle> findByIdForUpdate(UUID id) {
        return repo.findByIdForUpdate(id).map(mapper::toDomain);
    }

    public List<Vehicle> findAll() {
        return repo.findAll().stream().map(mapper::toDomain).toList();
    }

    public List<Vehicle> findAllByIds(Set<UUID> ids) {
        return repo.findAllById(ids).stream().map(mapper::toDomain).toList();
    }

    public Optional<Vehicle> findByRegistrationNumber(String registrationNumber) {
        return repo.findByRegistrationNumberIgnoreCase(registrationNumber).map(mapper::toDomain);
    }

    public Optional<Vehicle> findByChassisNumber(String chassisNumber) {
        return repo.findByChassisNumberIgnoreCase(chassisNumber).map(mapper::toDomain);
    }

    public Optional<Vehicle> findByEngineNumber(String engineNumber) {
        return repo.findByEngineNumberIgnoreCase(engineNumber).map(mapper::toDomain);
    }

    public boolean existsByRegistrationNumberAndIdNot(String registrationNumber, UUID id) {
        return repo.existsByRegistrationNumberIgnoreCaseAndIdNot(registrationNumber, id);
    }

    public boolean existsByChassisNumberAndIdNot(String chassisNumber, UUID id) {
        return repo.existsByChassisNumberIgnoreCaseAndIdNot(chassisNumber, id);
    }

    public boolean existsByEngineNumberAndIdNot(String engineNumber, UUID id) {
        return repo.existsByEngineNumberIgnoreCaseAndIdNot(engineNumber, id);
    }
}
