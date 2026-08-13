package com.transportlogistics.app.trip.application.service;

import com.transportlogistics.app.shared.domain.NotFoundException;
import com.transportlogistics.app.shared.domain.ConflictException;
import com.transportlogistics.app.trip.application.ports.in.TripUseCase;
import com.transportlogistics.app.trip.application.ports.out.TripRepository;
import com.transportlogistics.app.trip.application.ports.out.VehicleEligibilityPort;
import com.transportlogistics.app.trip.application.ports.out.DriverEligibilityPort;
import com.transportlogistics.app.trip.application.ports.out.TripHistoryRepository;
import com.transportlogistics.app.trip.application.ports.out.TripTransaction;
import com.transportlogistics.app.trip.domain.model.Trip;
import com.transportlogistics.app.trip.domain.model.TripCommand;
import com.transportlogistics.app.trip.domain.model.TripHistoryEntry;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class TripService implements TripUseCase {
    private final TripRepository repo;
    private final VehicleEligibilityPort vehicleEligibility;
    private final DriverEligibilityPort driverEligibility;
    private final TripHistoryRepository history;
    private final TripTransaction transactions;

    public TripService(TripRepository r, VehicleEligibilityPort vehicleEligibility,
                       DriverEligibilityPort driverEligibility, TripHistoryRepository history,
                       TripTransaction transactions) {
        repo = r;
        this.vehicleEligibility = vehicleEligibility;
        this.driverEligibility = driverEligibility;
        this.history = history;
        this.transactions = transactions;
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
                    auditActor, "Vehicle allocation recorded", now));
            return assigned;
        });
    }

    public Trip assignDriver(UUID id, UUID d, String requiredLicenseClass) {
        var t = get(id);
        driverEligibility.assertEligible(d, requiredLicenseClass, t.requestedStartTime(), t.requestedEndTime(),
                t.id());
        return repo.save(copy(t, t.status(), t.vehicleId(), d, t.actualStartTime(), t.actualEndTime(), t.startOdometerKm(), t.endOdometerKm(), t.completionRemarks()));
    }

    public Trip unassignVehicle(UUID id) {
        return transactions.execute(() -> {
            var trip = get(id);
            requireAssignmentState(trip);
            var now = OffsetDateTime.now();
            var unassigned = repo.save(copy(trip, "APPROVED", null, trip.driverId(), trip.actualStartTime(),
                    trip.actualEndTime(), trip.startOdometerKm(), trip.endOdometerKm(), trip.completionRemarks(), now));
            history.save(new TripHistoryEntry(UUID.randomUUID(), trip.id(), trip.status(), unassigned.status(),
                    "VEHICLE_UNASSIGNED", trip.vehicleId(), "system", "Vehicle allocation removed", now));
            return unassigned;
        });
    }

    public Trip unassignDriver(UUID id) {
        var t = get(id);
        return repo.save(copy(t, t.status(), t.vehicleId(), null, t.actualStartTime(), t.actualEndTime(),
                t.startOdometerKm(), t.endOdometerKm(), t.completionRemarks()));
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
            case TripCommand.Dispatch x -> {
                if (t.vehicleId() != null) vehicleEligibility.assertEligibleForDispatch(t.vehicleId(), t.requestedStartTime(),
                        t.requestedEndTime(), t.requiredVehicleTypeId(), t.requiredCapacityKg(), t.id());
                yield repo.save(copy(t, "DISPATCHED", t.vehicleId(), t.driverId(), t.actualStartTime(), t.actualEndTime(), t.startOdometerKm(), t.endOdometerKm(), t.completionRemarks()));
            }
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

    public List<TripHistoryEntry> history(UUID id) {
        get(id);
        return history.findByTripId(id);
    }

    private void requireAssignmentState(Trip trip) {
        if (!"APPROVED".equals(trip.status()) && !"ASSIGNED".equals(trip.status())) {
            throw new IllegalArgumentException("Vehicle assignment requires an APPROVED or ASSIGNED trip");
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
