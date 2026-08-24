package com.transportlogistics.app.fleet.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.fleet.application.ports.out.VehicleRepository;
import com.transportlogistics.app.fleet.domain.model.Vehicle;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
class VehiclePersistenceAdapter implements VehicleRepository {
    private final VehicleJpaRepository repo;

    VehiclePersistenceAdapter(VehicleJpaRepository repo) {
        this.repo = repo;
    }

    public Vehicle save(Vehicle v) {
        var e = new VehicleEntity();
        e.setId(v.id());
        e.setRegistrationNumber(v.registrationNumber());
        e.setChassisNumber(v.chassisNumber());
        e.setEngineNumber(v.engineNumber());
        e.setCategoryId(v.categoryId());
        e.setTypeId(v.typeId());
        e.setManufacturer(v.manufacturer());
        e.setModel(v.model());
        e.setManufactureYear(v.manufactureYear());
        e.setOwnershipType(v.ownershipType());
        e.setOperationalStatus(v.operationalStatus());
        e.setCurrentOdometerKm(v.currentOdometerKm());
        e.setEngineHours(v.engineHours());
        e.setCapacityKg(v.capacityKg());
        e.setActive(v.active());
        return map(repo.save(e));
    }

    public Optional<Vehicle> findById(UUID id) {
        return repo.findById(id).map(this::map);
    }

    public Optional<Vehicle> findByIdForUpdate(UUID id) {
        return repo.findByIdForUpdate(id).map(this::map);
    }

    public List<Vehicle> findAll() {
        return repo.findAll().stream().map(this::map).toList();
    }

    public Optional<Vehicle> findByRegistrationNumber(String registrationNumber) {
        return repo.findByRegistrationNumberIgnoreCase(registrationNumber).map(this::map);
    }

    public Optional<Vehicle> findByChassisNumber(String chassisNumber) {
        return repo.findByChassisNumberIgnoreCase(chassisNumber).map(this::map);
    }

    public Optional<Vehicle> findByEngineNumber(String engineNumber) {
        return repo.findByEngineNumberIgnoreCase(engineNumber).map(this::map);
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

    private Vehicle map(VehicleEntity e) {
        return new Vehicle(e.getId(), e.getRegistrationNumber(), e.getChassisNumber(), e.getEngineNumber(), e.getCategoryId(), e.getTypeId(), e.getManufacturer(), e.getModel(), e.getManufactureYear(), e.getOwnershipType(), e.getOperationalStatus(), e.getCurrentOdometerKm(), e.getEngineHours(), e.getCapacityKg(), e.isActive());
    }
}
