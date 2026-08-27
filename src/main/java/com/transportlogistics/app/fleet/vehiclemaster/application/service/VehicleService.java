package com.transportlogistics.app.fleet.vehiclemaster.application.service;

import com.transportlogistics.app.fleet.VehicleAllocationAvailability;
import com.transportlogistics.app.fleet.vehiclemaster.domain.error.InvalidVehicleStatusTransitionException;
import com.transportlogistics.app.fleet.vehiclemaster.ports.inbound.VehicleUseCase;
import com.transportlogistics.app.fleet.application.ports.out.VehicleCategoryRepository;
import com.transportlogistics.app.fleet.vehiclemaster.ports.outbound.VehicleRepository;
import com.transportlogistics.app.fleet.application.ports.out.VehicleTypeRepository;
import com.transportlogistics.app.fleet.vehiclemaster.domain.model.Vehicle;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.shared.domain.ConflictException;
import com.transportlogistics.app.shared.domain.NotFoundException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class VehicleService implements VehicleUseCase {
    private final VehicleRepository repo;
    private final VehicleCategoryRepository categories;
    private final VehicleTypeRepository types;
    private final VehicleAllocationAvailability allocations;

    public VehicleService(VehicleRepository repo, VehicleCategoryRepository categories, VehicleTypeRepository types,
                          VehicleAllocationAvailability allocations) {
        this.repo = repo;
        this.categories = categories;
        this.types = types;
        this.allocations = allocations;
    }

    public VehicleService(VehicleRepository repo, VehicleCategoryRepository categories, VehicleTypeRepository types) {
        this(repo, categories, types, null);
    }

    public VehicleService(VehicleRepository repo) {
        this(repo, null, null, null);
    }

    public Vehicle create(Vehicle value) {
        validateMasterDataReferences(value.categoryId(), value.typeId());
        checkDuplicateOnCreate(value);
        return repo.save(value);
    }

    public Vehicle get(UUID id) {
        return repo.findById(id).orElseThrow(() -> new NotFoundException("Vehicle not found: " + id));
    }

    public List<Vehicle> list() {
        return repo.findAll();
    }

    public Vehicle update(UUID id, Vehicle value) {
        var existing = get(id);
        validateMasterDataReferences(value.categoryId(), value.typeId());
        checkDuplicateOnUpdate(id, value);
        try {
            Vehicle.validateStatusTransition(existing.operationalStatus(), value.operationalStatus());
        } catch (InvalidVehicleStatusTransitionException exception) {
            throw new ConflictException(exception.code(), exception.getMessage());
        }

        if (existing.currentOdometerKm() != null && value.currentOdometerKm() != null
                && value.currentOdometerKm() < existing.currentOdometerKm()) {
            throw new BusinessRuleException("VEHICLE_DATA_INVALID",
                    "Odometer reading cannot be decreased through vehicle update");
        }
        if (existing.engineHours() != null && value.engineHours() != null
                && value.engineHours() < existing.engineHours()) {
            throw new BusinessRuleException("VEHICLE_DATA_INVALID",
                    "Engine hours cannot be decreased through vehicle update");
        }

        var vehicleToSave = new Vehicle(id, value.registrationNumber(), value.chassisNumber(), value.engineNumber(),
                value.categoryId(), value.typeId(), value.manufacturer(), value.model(), value.manufactureYear(),
                value.ownershipType(), value.operationalStatus(), value.currentOdometerKm(), value.engineHours(),
                value.capacityKg(), value.tareWeightKg(), value.grossVehicleWeightKg(),
                value.cargoVolumeCapacityM3(), value.axleCount(), value.maxAxleLoadKg(), value.active());
        return repo.save(vehicleToSave);
    }

    public void deactivate(UUID id) {
        var v = get(id);
        if (allocations != null && allocations.hasOverlap(id, OffsetDateTime.now(), OffsetDateTime.now().plusHours(1), null)) {
            throw new ConflictException("VEHICLE_RETIREMENT_BLOCKED",
                    "Cannot deactivate vehicle with active trip allocations");
        }
        repo.save(new Vehicle(v.id(), v.registrationNumber(), v.chassisNumber(), v.engineNumber(), v.categoryId(),
                v.typeId(), v.manufacturer(), v.model(), v.manufactureYear(), v.ownershipType(), v.operationalStatus(),
                v.currentOdometerKm(), v.engineHours(), v.capacityKg(), v.tareWeightKg(), v.grossVehicleWeightKg(),
                v.cargoVolumeCapacityM3(), v.axleCount(), v.maxAxleLoadKg(), false));
    }

    private void validateMasterDataReferences(UUID categoryId, UUID typeId) {
        if (categories != null) {
            var category = categories.findById(categoryId)
                    .orElseThrow(() -> new BusinessRuleException("VEHICLE_MASTER_REFERENCE_INVALID",
                            "Vehicle category not found: " + categoryId));
            if (!category.active()) {
                throw new BusinessRuleException("VEHICLE_MASTER_REFERENCE_INVALID",
                        "Vehicle category is inactive: " + categoryId);
            }
        }
        if (types != null) {
            var type = types.findById(typeId)
                    .orElseThrow(() -> new BusinessRuleException("VEHICLE_MASTER_REFERENCE_INVALID",
                            "Vehicle type not found: " + typeId));
            if (!type.active()) {
                throw new BusinessRuleException("VEHICLE_MASTER_REFERENCE_INVALID",
                        "Vehicle type is inactive: " + typeId);
            }
            if (!type.categoryId().equals(categoryId)) {
                throw new BusinessRuleException("VEHICLE_MASTER_REFERENCE_INVALID",
                        "Vehicle type does not belong to the selected category");
            }
        }
    }

    private void checkDuplicateOnCreate(Vehicle value) {
        repo.findByRegistrationNumber(value.registrationNumber()).ifPresent(existing -> {
            throw new ConflictException("VEHICLE_REGISTRATION_DUPLICATE",
                    "Vehicle with registration number " + value.registrationNumber() + " already exists");
        });
        if (value.chassisNumber() != null && !value.chassisNumber().isBlank()) {
            repo.findByChassisNumber(value.chassisNumber()).ifPresent(existing -> {
                throw new ConflictException("VEHICLE_CHASSIS_DUPLICATE",
                        "Vehicle with chassis number " + value.chassisNumber() + " already exists");
            });
        }
        if (value.engineNumber() != null && !value.engineNumber().isBlank()) {
            repo.findByEngineNumber(value.engineNumber()).ifPresent(existing -> {
                throw new ConflictException("VEHICLE_ENGINE_DUPLICATE",
                        "Vehicle with engine number " + value.engineNumber() + " already exists");
            });
        }
    }

    private void checkDuplicateOnUpdate(UUID id, Vehicle value) {
        if (repo.existsByRegistrationNumberAndIdNot(value.registrationNumber(), id)) {
            throw new ConflictException("VEHICLE_REGISTRATION_DUPLICATE",
                    "Vehicle with registration number " + value.registrationNumber() + " already exists");
        }
        if (value.chassisNumber() != null && !value.chassisNumber().isBlank()
                && repo.existsByChassisNumberAndIdNot(value.chassisNumber(), id)) {
            throw new ConflictException("VEHICLE_CHASSIS_DUPLICATE",
                    "Vehicle with chassis number " + value.chassisNumber() + " already exists");
        }
        if (value.engineNumber() != null && !value.engineNumber().isBlank()
                && repo.existsByEngineNumberAndIdNot(value.engineNumber(), id)) {
            throw new ConflictException("VEHICLE_ENGINE_DUPLICATE",
                    "Vehicle with engine number " + value.engineNumber() + " already exists");
        }
    }
}
