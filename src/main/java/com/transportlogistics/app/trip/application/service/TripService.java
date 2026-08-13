package com.transportlogistics.app.trip.application.service;

import com.transportlogistics.app.shared.domain.NotFoundException;
import com.transportlogistics.app.shared.domain.ConflictException;
import com.transportlogistics.app.trip.application.ports.in.TripUseCase;
import com.transportlogistics.app.trip.application.ports.out.TripRepository;
import com.transportlogistics.app.trip.application.ports.out.VehicleEligibilityPort;
import com.transportlogistics.app.trip.application.ports.out.DriverEligibilityPort;
import com.transportlogistics.app.trip.application.ports.out.TripHistoryRepository;
import com.transportlogistics.app.trip.application.ports.out.TripTransaction;
import com.transportlogistics.app.trip.application.ports.out.TripDispatchRepository;
import com.transportlogistics.app.trip.domain.model.Trip;
import com.transportlogistics.app.trip.domain.model.TripCommand;
import com.transportlogistics.app.trip.domain.model.TripHistoryEntry;
import com.transportlogistics.app.trip.domain.model.TripDispatchRecord;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class TripService implements TripUseCase {
    private final TripRepository repo;
    private final VehicleEligibilityPort vehicleEligibility;
    private final DriverEligibilityPort driverEligibility;
    private final TripHistoryRepository history;
    private final TripTransaction transactions;
    private final TripDispatchRepository dispatches;

    public TripService(TripRepository r, VehicleEligibilityPort vehicleEligibility,
                       DriverEligibilityPort driverEligibility, TripHistoryRepository history,
                       TripTransaction transactions, TripDispatchRepository dispatches) {
        repo = r;
        this.vehicleEligibility = vehicleEligibility;
        this.driverEligibility = driverEligibility;
        this.history = history;
        this.transactions = transactions;
        this.dispatches = dispatches;
    }

    public Trip create(Trip t) {
        return repo.save(t);
    }

    public Trip get(UUID id) {
        return repo.findById(id).orElseThrow(() -> new NotFoundException("Trip not found: " + id));
    }

    public List<Trip> list() {
        return repo.findAll();
    }

    public Trip update(UUID id, Trip t) {
        get(id);
        return repo.save(t);
    }

    public Trip assignVehicle(UUID id, UUID vehicleId, String actor) {
        if (vehicleId == null) {
            throw new IllegalArgumentException("Vehicle id is required");
        }
        var auditActor = actor == null || actor.isBlank() ? "system" : actor.trim();
        return transactions.execute(() -> {
            var trip = get(id);
            requireAssignmentState(trip);
            vehicleEligibility.assertEligibleForAssignment(vehicleId, trip.requestedStartTime(),
                    trip.requestedEndTime(), trip.requiredVehicleTypeId(), trip.requiredCapacityKg());
            if (repo.hasOverlappingVehicleAllocation(vehicleId, trip.requestedStartTime(), trip.requestedEndTime(),
                    trip.id())) {
                throw new ConflictException("Vehicle already has an overlapping trip allocation");
            }
            var now = OffsetDateTime.now();
            var assigned = repo.save(copy(trip, "ASSIGNED", vehicleId, trip.driverId(), trip.actualStartTime(),
                    trip.actualEndTime(), trip.startOdometerKm(), trip.endOdometerKm(), trip.completionRemarks(), now));
            history.save(new TripHistoryEntry(UUID.randomUUID(), trip.id(), trip.status(), assigned.status(),
                    trip.vehicleId() == null ? "VEHICLE_ASSIGNED" : "VEHICLE_REASSIGNED", vehicleId,
                    null, null, auditActor, "Vehicle allocation recorded", now));
            return assigned;
        });
    }

    public Trip assignDriver(UUID id, UUID driverId, String requiredLicenseClass, String actor) {
        if (driverId == null) {
            throw new IllegalArgumentException("Driver id is required");
        }
        if (requiredLicenseClass == null || requiredLicenseClass.isBlank()) {
            throw new IllegalArgumentException("Required license class is required");
        }
        var auditActor = actor == null || actor.isBlank() ? "system" : actor.trim();
        return transactions.execute(() -> {
            var trip = get(id);
            requireAssignmentState(trip);
            driverEligibility.assertEligible(driverId, requiredLicenseClass, trip.requestedStartTime(),
                    trip.requestedEndTime());
            if (repo.hasOverlappingDriverAssignment(driverId, trip.requestedStartTime(), trip.requestedEndTime(),
                    trip.id())) {
                throw new ConflictException("Driver already has an overlapping trip assignment");
            }
            var now = OffsetDateTime.now();
            var assigned = repo.save(copy(trip, "ASSIGNED", trip.vehicleId(), driverId, trip.actualStartTime(),
                    trip.actualEndTime(), trip.startOdometerKm(), trip.endOdometerKm(), trip.completionRemarks(), now));
            history.save(new TripHistoryEntry(UUID.randomUUID(), trip.id(), trip.status(), assigned.status(),
                    trip.driverId() == null ? "DRIVER_ASSIGNED" : "DRIVER_REASSIGNED", trip.vehicleId(), driverId,
                    requiredLicenseClass.trim().toUpperCase(), auditActor,
                    "Driver assignment recorded for license class " + requiredLicenseClass.trim(), now));
            return assigned;
        });
    }

    public Trip unassignVehicle(UUID id) {
        return transactions.execute(() -> {
            var trip = get(id);
            requireAssignmentState(trip);
            var now = OffsetDateTime.now();
            var unassigned = repo.save(copy(trip, "APPROVED", null, trip.driverId(), trip.actualStartTime(),
                    trip.actualEndTime(), trip.startOdometerKm(), trip.endOdometerKm(), trip.completionRemarks(), now));
            history.save(new TripHistoryEntry(UUID.randomUUID(), trip.id(), trip.status(), unassigned.status(),
                    "VEHICLE_UNASSIGNED", trip.vehicleId(), null, null, "system",
                    "Vehicle allocation removed", now));
            return unassigned;
        });
    }

    public Trip unassignDriver(UUID id) {
        return transactions.execute(() -> {
            var trip = get(id);
            requireAssignmentState(trip);
            var now = OffsetDateTime.now();
            var nextStatus = trip.vehicleId() == null ? "APPROVED" : "ASSIGNED";
            var unassigned = repo.save(copy(trip, nextStatus, trip.vehicleId(), null, trip.actualStartTime(),
                    trip.actualEndTime(), trip.startOdometerKm(), trip.endOdometerKm(), trip.completionRemarks(), now));
            history.save(new TripHistoryEntry(UUID.randomUUID(), trip.id(), trip.status(), unassigned.status(),
                    "DRIVER_UNASSIGNED", trip.vehicleId(), trip.driverId(), null, "system",
                    "Driver assignment removed", now));
            return unassigned;
        });
    }

    public Trip transition(UUID id, TripCommand c) {
        var t = get(id);
        var now = OffsetDateTime.now();
        return switch (c) {
            case TripCommand.Submit x ->
                    repo.save(copy(t, "SUBMITTED", t.vehicleId(), t.driverId(), t.actualStartTime(), t.actualEndTime(), t.startOdometerKm(), t.endOdometerKm(), t.completionRemarks()));
            case TripCommand.Approve x ->
                    repo.save(copy(t, "APPROVED", t.vehicleId(), t.driverId(), t.actualStartTime(), t.actualEndTime(), t.startOdometerKm(), t.endOdometerKm(), t.completionRemarks()));
            case TripCommand.Reject x ->
                    repo.save(copy(t, "REJECTED", t.vehicleId(), t.driverId(), t.actualStartTime(), t.actualEndTime(), t.startOdometerKm(), t.endOdometerKm(), x.reason()));
            case TripCommand.Dispatch x -> dispatch(id, "system", null);
            case TripCommand.Start x ->
                    repo.save(copy(t, "IN_PROGRESS", t.vehicleId(), t.driverId(), now, t.actualEndTime(), x.odometerKm(), t.endOdometerKm(), t.completionRemarks()));
            case TripCommand.Complete x ->
                    repo.save(copy(t, "COMPLETED", t.vehicleId(), t.driverId(), t.actualStartTime(), now, t.startOdometerKm(), x.odometerKm(), x.remarks()));
            case TripCommand.Close x ->
                    repo.save(copy(t, "CLOSED", t.vehicleId(), t.driverId(), t.actualStartTime(), t.actualEndTime(), t.startOdometerKm(), t.endOdometerKm(), t.completionRemarks()));
            case TripCommand.Cancel x ->
                    repo.save(copy(t, "CANCELLED", t.vehicleId(), t.driverId(), t.actualStartTime(), t.actualEndTime(), t.startOdometerKm(), t.endOdometerKm(), x.reason()));
        };
    }

    public Trip dispatch(UUID id, String actor, String remarks) {
        var dispatchedBy = actor == null || actor.isBlank() ? "system" : actor.trim();
        return transactions.execute(() -> {
            var trip = get(id);
            requireDispatchState(trip);
            var licenseClass = history.findCurrentDriverLicenseClass(trip.id(), trip.driverId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Assigned driver license class is required for dispatch"));

            vehicleEligibility.assertEligibleForAssignment(trip.vehicleId(), trip.requestedStartTime(),
                    trip.requestedEndTime(), trip.requiredVehicleTypeId(), trip.requiredCapacityKg());
            if (repo.hasOverlappingVehicleAllocation(trip.vehicleId(), trip.requestedStartTime(),
                    trip.requestedEndTime(), trip.id())) {
                throw new ConflictException("Vehicle has a conflicting allocation");
            }

            driverEligibility.assertEligible(trip.driverId(), licenseClass, trip.requestedStartTime(),
                    trip.requestedEndTime());
            if (repo.hasOverlappingDriverAssignment(trip.driverId(), trip.requestedStartTime(),
                    trip.requestedEndTime(), trip.id())) {
                throw new ConflictException("Driver has a conflicting assignment");
            }

            var now = OffsetDateTime.now();
            var dispatched = repo.save(copy(trip, "DISPATCHED", trip.vehicleId(), trip.driverId(),
                    trip.actualStartTime(), trip.actualEndTime(), trip.startOdometerKm(), trip.endOdometerKm(),
                    trip.completionRemarks(), now));
            dispatches.save(new TripDispatchRecord(trip.id(), now, dispatchedBy, remarks));
            history.save(new TripHistoryEntry(UUID.randomUUID(), trip.id(), trip.status(), dispatched.status(),
                    "TRIP_DISPATCHED", trip.vehicleId(), trip.driverId(), licenseClass, dispatchedBy,
                    remarks == null || remarks.isBlank() ? "Trip dispatched" : remarks.trim(), now));
            return dispatched;
        });
    }

    public List<TripHistoryEntry> history(UUID id) {
        get(id);
        return history.findByTripId(id);
    }

    private void requireAssignmentState(Trip trip) {
        if (!"APPROVED".equals(trip.status()) && !"ASSIGNED".equals(trip.status())) {
            throw new IllegalArgumentException("Resource assignment requires an APPROVED or ASSIGNED trip");
        }
    }

    private void requireDispatchState(Trip trip) {
        if (!"ASSIGNED".equals(trip.status())) {
            throw new IllegalArgumentException("Dispatch requires an ASSIGNED trip");
        }
        if (trip.vehicleId() == null) {
            throw new IllegalArgumentException("An assigned vehicle is required for dispatch");
        }
        if (trip.driverId() == null) {
            throw new IllegalArgumentException("An assigned driver is required for dispatch");
        }
    }

    private Trip copy(Trip t, String s, UUID v, UUID d, OffsetDateTime as, OffsetDateTime ae, Double so, Double eo, String remarks) {
        return copy(t, s, v, d, as, ae, so, eo, remarks, OffsetDateTime.now());
    }

    private Trip copy(Trip t, String s, UUID v, UUID d, OffsetDateTime as, OffsetDateTime ae, Double so,
                      Double eo, String remarks, OffsetDateTime updatedAt) {
        return new Trip(t.id(), t.tripNumber(), t.customerId(), t.departmentId(), t.projectId(), t.routeId(),
                t.priority(), s, t.originLocationId(), t.destinationLocationId(), t.requestedStartTime(),
                t.requestedEndTime(), t.requiredVehicleTypeId(), t.requiredCapacityKg(), t.cargoDescription(),
                t.passengerCount(), t.customerInstructions(), t.notes(), v, d, as, ae, so, eo, remarks,
                t.createdAt(), updatedAt);
    }
}
