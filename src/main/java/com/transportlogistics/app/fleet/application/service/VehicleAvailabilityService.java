package com.transportlogistics.app.fleet.application.service;

import com.transportlogistics.app.fleet.VehicleAllocationAvailability;
import com.transportlogistics.app.fleet.application.ports.in.VehicleAvailabilityUseCase;
import com.transportlogistics.app.fleet.application.ports.out.VehicleDocumentRepository;
import com.transportlogistics.app.fleet.application.ports.out.VehicleRepository;
import com.transportlogistics.app.fleet.domain.model.VehicleAvailability;
import com.transportlogistics.app.shared.domain.NotFoundException;

import java.util.ArrayList;

import static com.transportlogistics.app.fleet.domain.model.VehicleAvailability.Code.*;

public final class VehicleAvailabilityService implements VehicleAvailabilityUseCase {
    private final VehicleRepository vehicles;
    private final VehicleDocumentRepository documents;
    private final VehicleAllocationAvailability allocations;

    public VehicleAvailabilityService(VehicleRepository vehicles, VehicleDocumentRepository documents,
                                      VehicleAllocationAvailability allocations) {
        this.vehicles = vehicles;
        this.documents = documents;
        this.allocations = allocations;
    }

    @Override
    public VehicleAvailability evaluate(Query query) {
        if (query.from() == null || query.to() == null || !query.from().isBefore(query.to())) {
            throw new IllegalArgumentException("Availability period must have from before to");
        }
        if (query.requiredCapacityKg() != null && query.requiredCapacityKg() < 0) {
            throw new IllegalArgumentException("Required capacity cannot be negative");
        }
        var vehicle = (query.lockVehicle() ? vehicles.findByIdForUpdate(query.vehicleId())
                : vehicles.findById(query.vehicleId()))
                .orElseThrow(() -> new NotFoundException("Vehicle not found: " + query.vehicleId()));
        var reasons = new ArrayList<VehicleAvailability.Reason>();

        if (!vehicle.active()) add(reasons, INACTIVE, "Vehicle is inactive");
        addOperationalReason(vehicle.operationalStatus(), reasons);
        var mandatoryDocuments = documents.findVisibleByVehicleId(vehicle.id()).stream()
                .filter(document -> document.mandatoryForDispatch()).toList();
        if (mandatoryDocuments.stream().anyMatch(document -> !document.active()
                || document.issueDate() != null && document.issueDate().isAfter(query.from().toLocalDate()))) {
            add(reasons, MANDATORY_DOCUMENT_INVALID,
                    "A mandatory vehicle document is inactive or not yet valid");
        }
        if (mandatoryDocuments.stream().anyMatch(document -> document.active() && document.expiryDate() != null
                && document.expiryDate().isBefore(query.to().toLocalDate()))) {
            add(reasons, MANDATORY_DOCUMENT_EXPIRED,
                    "A mandatory vehicle document expires before the requested period ends");
        }
        if (query.requiredVehicleTypeId() != null && !query.requiredVehicleTypeId().equals(vehicle.typeId())) {
            add(reasons, VEHICLE_TYPE_MISMATCH, "Vehicle type does not match the required vehicle type");
        }
        if (query.requiredCapacityKg() != null
                && (vehicle.capacityKg() == null || vehicle.capacityKg() < query.requiredCapacityKg())) {
            add(reasons, INSUFFICIENT_CAPACITY, "Vehicle capacity is below the required capacity");
        }
        if (query.checkAllocationConflicts()
                && allocations.hasOverlap(vehicle.id(), query.from(), query.to(), query.excludeTripId())) {
            add(reasons, OVERLAPPING_ALLOCATION, "Vehicle has an overlapping trip allocation");
        }
        return VehicleAvailability.from(reasons);
    }

    private void addOperationalReason(String status, ArrayList<VehicleAvailability.Reason> reasons) {
        var normalized = status == null ? "" : status.trim().toUpperCase();
        switch (normalized) {
            case "AVAILABLE" -> {
            }
            case "BROKEN_DOWN", "BREAKDOWN" -> add(reasons, BROKEN_DOWN, "Vehicle is broken down");
            case "OUT_OF_SERVICE" -> add(reasons, OUT_OF_SERVICE, "Vehicle is out of service");
            case "MAINTENANCE", "UNDER_MAINTENANCE", "MAINTENANCE_DUE" ->
                    add(reasons, MAINTENANCE_BLOCKED, "Vehicle is blocked by maintenance status");
            default -> add(reasons, OPERATIONALLY_UNAVAILABLE, "Vehicle is not operationally available");
        }
    }

    private void add(ArrayList<VehicleAvailability.Reason> reasons, VehicleAvailability.Code code, String message) {
        reasons.add(new VehicleAvailability.Reason(code, message));
    }
}
