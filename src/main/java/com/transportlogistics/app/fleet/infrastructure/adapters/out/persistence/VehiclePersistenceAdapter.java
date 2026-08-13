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

    public List<Vehicle> findAll() {
        return repo.findAll().stream().map(this::map).toList();
    }

    private Vehicle map(VehicleEntity e) {
        return new Vehicle(e.getId(), e.getRegistrationNumber(), e.getChassisNumber(), e.getEngineNumber(), e.getCategoryId(), e.getTypeId(), e.getManufacturer(), e.getModel(), e.getManufactureYear(), e.getOwnershipType(), e.getOperationalStatus(), e.getCurrentOdometerKm(), e.getEngineHours(), e.getCapacityKg(), e.isActive());
    }
}
