package com.transportlogistics.app.fleet.application.service;

import com.transportlogistics.app.fleet.application.ports.in.VehicleUseCase;
import com.transportlogistics.app.fleet.application.ports.out.VehicleRepository;
import com.transportlogistics.app.fleet.application.ports.out.VehicleDocumentRepository;
import com.transportlogistics.app.fleet.domain.model.Vehicle;
import com.transportlogistics.app.fleet.domain.model.VehicleAvailability;
import com.transportlogistics.app.shared.domain.NotFoundException;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class VehicleService implements VehicleUseCase {
    private final VehicleRepository repo;
    private final VehicleDocumentRepository documents;

    public VehicleService(VehicleRepository repo, VehicleDocumentRepository documents) {
        this.repo = repo;
        this.documents = documents;
    }

    public Vehicle create(Vehicle value) {
        return repo.save(value);
    }

    public Vehicle get(UUID id) {
        return repo.findById(id).orElseThrow(() -> new NotFoundException("Vehicle not found: " + id));
    }

    public List<Vehicle> list() {
        return repo.findAll();
    }

    public Vehicle update(UUID id, Vehicle value) {
        return repo.save(value);
    }

    public void deactivate(UUID id) {
        var v = get(id);
        repo.save(new Vehicle(v.id(), v.registrationNumber(), v.chassisNumber(), v.engineNumber(), v.categoryId(), v.typeId(), v.manufacturer(), v.model(), v.manufactureYear(), v.ownershipType(), v.operationalStatus(), v.currentOdometerKm(), v.engineHours(), v.capacityKg(), false));
    }

    @Override
    public VehicleAvailability availability(UUID id, LocalDate onDate) {
        var vehicle = get(id);
        if (!vehicle.active()) return VehicleAvailability.unavailable("INACTIVE");
        if (!"AVAILABLE".equalsIgnoreCase(vehicle.operationalStatus())) {
            return VehicleAvailability.unavailable("OPERATIONAL_STATUS_" + vehicle.operationalStatus().toUpperCase());
        }
        var expiredMandatoryDocument = documents.findActiveByVehicleId(id).stream()
                .anyMatch(document -> document.blocksDispatchOn(onDate));
        return expiredMandatoryDocument
                ? VehicleAvailability.unavailable("MANDATORY_DOCUMENT_EXPIRED")
                : VehicleAvailability.eligible();
    }

    @Override
    public void assertAvailableForDispatch(UUID id, LocalDate onDate) {
        var availability = availability(id, onDate);
        if (!availability.available()) {
            throw new IllegalArgumentException("Vehicle is unavailable for allocation or dispatch: " + availability.reason());
        }
    }
}
