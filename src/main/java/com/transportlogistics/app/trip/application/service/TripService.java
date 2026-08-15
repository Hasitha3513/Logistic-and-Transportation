package com.transportlogistics.app.trip.application.service;

import com.transportlogistics.app.shared.domain.ConflictException;
import com.transportlogistics.app.shared.domain.NotFoundException;
import com.transportlogistics.app.trip.application.ports.in.TripUseCase;
import com.transportlogistics.app.trip.application.ports.out.DriverEligibilityPort;
import com.transportlogistics.app.trip.application.ports.out.RouteEligibilityPort;
import com.transportlogistics.app.trip.application.ports.out.TripDispatchRepository;
import com.transportlogistics.app.trip.application.ports.out.TripHistoryRepository;
import com.transportlogistics.app.trip.application.ports.out.TripRepository;
import com.transportlogistics.app.trip.application.ports.out.TripTransaction;
import com.transportlogistics.app.trip.application.ports.out.VehicleEligibilityPort;
import com.transportlogistics.app.trip.domain.model.Trip;
import com.transportlogistics.app.trip.domain.model.TripCommand;
import com.transportlogistics.app.trip.domain.model.TripDispatchRecord;
import com.transportlogistics.app.trip.domain.model.TripHistoryEntry;
import com.transportlogistics.app.trip.domain.model.TripLifecyclePolicy;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class TripService implements TripUseCase {
    private final TripRepository repo;
    private final VehicleEligibilityPort vehicleEligibility;
    private final DriverEligibilityPort driverEligibility;
    private final RouteEligibilityPort routeEligibility;
    private final TripHistoryRepository history;
    private final TripTransaction transactions;
    private final TripDispatchRepository dispatches;
    private final Clock clock;
    private final TripLifecyclePolicy lifecycle = new TripLifecyclePolicy();

    public TripService(TripRepository repo, VehicleEligibilityPort vehicleEligibility,
                       DriverEligibilityPort driverEligibility, TripHistoryRepository history,
                       TripTransaction transactions, TripDispatchRepository dispatches) {
        this(repo, vehicleEligibility, driverEligibility, noOpRouteEligibility(), history, transactions, dispatches,
                Clock.systemUTC());
    }

    public TripService(TripRepository repo, VehicleEligibilityPort vehicleEligibility,
                       DriverEligibilityPort driverEligibility, TripHistoryRepository history,
                       TripTransaction transactions, TripDispatchRepository dispatches, Clock clock) {
        this(repo, vehicleEligibility, driverEligibility, noOpRouteEligibility(), history, transactions, dispatches,
                clock);
    }

    public TripService(TripRepository repo, VehicleEligibilityPort vehicleEligibility,
                       DriverEligibilityPort driverEligibility, RouteEligibilityPort routeEligibility,
                       TripHistoryRepository history, TripTransaction transactions,
                       TripDispatchRepository dispatches, Clock clock) {
        this.repo = repo;
        this.vehicleEligibility = vehicleEligibility;
        this.driverEligibility = driverEligibility;
        this.routeEligibility = routeEligibility;
        this.history = history;
        this.transactions = transactions;
        this.dispatches = dispatches;
        this.clock = clock;
    }

    @Override
    public Trip create(CreateCommand command) {
        var now = now();
        var trip = new Trip(UUID.randomUUID(), "TRIP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                command.customerId(), command.departmentId(), command.projectId(), command.routeId(),
                command.priority() == null || command.priority().isBlank() ? "NORMAL" : command.priority().trim(),
                TripLifecyclePolicy.DRAFT, command.originLocationId(), command.destinationLocationId(),
                command.requestedStartTime(), command.requestedEndTime(), command.requiredVehicleTypeId(),
                command.requiredCapacityKg(), command.cargoDescription(), command.passengerCount(),
                command.customerInstructions(), command.notes(), null, null, null, null, null, null, null, now, now);
        lifecycle.validateOrder(trip);
        validateRoute(trip);
        return repo.save(trip);
    }

    @Override
    public Trip get(UUID id) {
        return repo.findById(id).orElseThrow(() -> new NotFoundException("Trip not found: " + id));
    }

    @Override
    public List<Trip> list() {
        return repo.findAll();
    }

    @Override
    public Trip update(UUID id, Trip requested) {
        return transactions.execute(() -> {
            var current = getForUpdate(id);
            lifecycle.requireEditable(current);
            lifecycle.validateOrder(requested);
            validateRoute(requested);
            if ((current.vehicleId() != null || current.driverId() != null) && affectsEligibility(current, requested)) {
                throw new ConflictException("ASSIGNED_TRIP_UPDATE_REQUIRES_REVALIDATION",
                        "Assignment-affecting trip fields cannot be changed while a vehicle or driver is assigned");
            }
            var updated = new Trip(current.id(), current.tripNumber(), requested.customerId(),
                    requested.departmentId(), requested.projectId(), requested.routeId(), requested.priority(),
                    current.status(), requested.originLocationId(), requested.destinationLocationId(),
                    requested.requestedStartTime(), requested.requestedEndTime(), requested.requiredVehicleTypeId(),
                    requested.requiredCapacityKg(), requested.cargoDescription(), requested.passengerCount(),
                    requested.customerInstructions(), requested.notes(), current.vehicleId(), current.driverId(),
                    current.actualStartTime(), current.actualEndTime(), current.startOdometerKm(),
                    current.endOdometerKm(), current.completionRemarks(), current.createdAt(), now());
            return repo.save(updated);
        });
    }

    @Override
    public Trip assignVehicle(UUID id, UUID vehicleId, String actor) {
        if (vehicleId == null) {
            throw new IllegalArgumentException("Vehicle id is required");
        }
        var auditActor = actor(actor);
        return transactions.execute(() -> {
            var trip = getForUpdate(id);
            lifecycle.requireAssignmentAllowed(trip);
            vehicleEligibility.assertEligibleForAssignment(vehicleId, trip.requestedStartTime(),
                    trip.requestedEndTime(), trip.requiredVehicleTypeId(), trip.requiredCapacityKg());
            if (repo.hasOverlappingVehicleAllocation(vehicleId, trip.requestedStartTime(), trip.requestedEndTime(),
                    trip.id())) {
                throw new ConflictException("Vehicle already has an overlapping trip allocation");
            }
            var now = now();
            var nextStatus = lifecycle.statusAfterAssignment(trip, vehicleId, trip.driverId());
            var assigned = repo.save(copy(trip, nextStatus, vehicleId, trip.driverId(), trip.actualStartTime(),
                    trip.actualEndTime(), trip.startOdometerKm(), trip.endOdometerKm(), trip.completionRemarks(), now));
            history.save(new TripHistoryEntry(UUID.randomUUID(), trip.id(), trip.status(), assigned.status(),
                    trip.vehicleId() == null ? "VEHICLE_ASSIGNED" : "VEHICLE_REASSIGNED", vehicleId,
                    trip.driverId(), null, auditActor, "Vehicle allocation recorded", now));
            return assigned;
        });
    }

    @Override
    public Trip assignDriver(UUID id, UUID driverId, String requiredLicenseClass, String actor) {
        if (driverId == null) {
            throw new IllegalArgumentException("Driver id is required");
        }
        if (requiredLicenseClass == null || requiredLicenseClass.isBlank()) {
            throw new IllegalArgumentException("Required license class is required");
        }
        var auditActor = actor(actor);
        return transactions.execute(() -> {
            var trip = getForUpdate(id);
            lifecycle.requireAssignmentAllowed(trip);
            driverEligibility.assertEligible(driverId, requiredLicenseClass, trip.requestedStartTime(),
                    trip.requestedEndTime());
            if (repo.hasOverlappingDriverAssignment(driverId, trip.requestedStartTime(), trip.requestedEndTime(),
                    trip.id())) {
                throw new ConflictException("Driver already has an overlapping trip assignment");
            }
            var now = now();
            var nextStatus = lifecycle.statusAfterAssignment(trip, trip.vehicleId(), driverId);
            var assigned = repo.save(copy(trip, nextStatus, trip.vehicleId(), driverId, trip.actualStartTime(),
                    trip.actualEndTime(), trip.startOdometerKm(), trip.endOdometerKm(), trip.completionRemarks(), now));
            history.save(new TripHistoryEntry(UUID.randomUUID(), trip.id(), trip.status(), assigned.status(),
                    trip.driverId() == null ? "DRIVER_ASSIGNED" : "DRIVER_REASSIGNED", trip.vehicleId(), driverId,
                    requiredLicenseClass.trim().toUpperCase(), auditActor,
                    "Driver assignment recorded for license class " + requiredLicenseClass.trim(), now));
            return assigned;
        });
    }

    @Override
    public Trip unassignVehicle(UUID id, String actor) {
        var auditActor = actor(actor);
        return transactions.execute(() -> {
            var trip = getForUpdate(id);
            lifecycle.requireAssignmentAllowed(trip);
            var now = now();
            var nextStatus = lifecycle.statusAfterAssignment(trip, null, trip.driverId());
            var unassigned = repo.save(copy(trip, nextStatus, null, trip.driverId(), trip.actualStartTime(),
                    trip.actualEndTime(), trip.startOdometerKm(), trip.endOdometerKm(), trip.completionRemarks(), now));
            history.save(new TripHistoryEntry(UUID.randomUUID(), trip.id(), trip.status(), unassigned.status(),
                    "VEHICLE_UNASSIGNED", trip.vehicleId(), trip.driverId(), null, auditActor,
                    "Vehicle allocation removed", now));
            return unassigned;
        });
    }

    @Override
    public Trip unassignDriver(UUID id, String actor) {
        var auditActor = actor(actor);
        return transactions.execute(() -> {
            var trip = getForUpdate(id);
            lifecycle.requireAssignmentAllowed(trip);
            var now = now();
            var nextStatus = lifecycle.statusAfterAssignment(trip, trip.vehicleId(), null);
            var unassigned = repo.save(copy(trip, nextStatus, trip.vehicleId(), null, trip.actualStartTime(),
                    trip.actualEndTime(), trip.startOdometerKm(), trip.endOdometerKm(), trip.completionRemarks(), now));
            history.save(new TripHistoryEntry(UUID.randomUUID(), trip.id(), trip.status(), unassigned.status(),
                    "DRIVER_UNASSIGNED", trip.vehicleId(), trip.driverId(), null, auditActor,
                    "Driver assignment removed", now));
            return unassigned;
        });
    }

    @Override
    public Trip assignRoute(UUID id, UUID routeId, String actor) {
        if (routeId == null) {
            throw new IllegalArgumentException("Route id is required");
        }
        var auditActor = actor(actor);
        return transactions.execute(() -> {
            var trip = getForUpdate(id);
            lifecycle.requireRouteAssignmentAllowed(trip);
            routeEligibility.assertAssignable(routeId, trip.originLocationId(), trip.destinationLocationId());
            var occurredAt = now();
            var assigned = new Trip(trip.id(), trip.tripNumber(), trip.customerId(), trip.departmentId(),
                    trip.projectId(), routeId, trip.priority(), trip.status(), trip.originLocationId(),
                    trip.destinationLocationId(), trip.requestedStartTime(), trip.requestedEndTime(),
                    trip.requiredVehicleTypeId(), trip.requiredCapacityKg(), trip.cargoDescription(),
                    trip.passengerCount(), trip.customerInstructions(), trip.notes(), trip.vehicleId(),
                    trip.driverId(), trip.actualStartTime(), trip.actualEndTime(), trip.startOdometerKm(),
                    trip.endOdometerKm(), trip.completionRemarks(), trip.createdAt(), occurredAt);
            var saved = repo.save(assigned);
            history.save(new TripHistoryEntry(UUID.randomUUID(), trip.id(), trip.status(), saved.status(),
                    trip.routeId() == null ? "ROUTE_ASSIGNED" : "ROUTE_REASSIGNED", trip.vehicleId(),
                    trip.driverId(), null, auditActor, "Route assigned: " + routeId, occurredAt));
            return saved;
        });
    }

    @Override
    public Trip transition(UUID id, TripCommand command, String actor) {
        if (command instanceof TripCommand.Dispatch) {
            return dispatch(id, actor, null);
        }
        return transactions.execute(() -> {
            var trip = getForUpdate(id);
            var occurredAt = now();
            lifecycle.validateTransition(trip, command, actor, occurredAt);
            var auditActor = actor(actor);
            var changed = apply(trip, command, occurredAt);
            var saved = repo.save(changed);
            history.save(new TripHistoryEntry(UUID.randomUUID(), trip.id(), trip.status(), saved.status(),
                    lifecycle.action(command), trip.vehicleId(), trip.driverId(), null, auditActor,
                    lifecycle.details(command), occurredAt));
            return saved;
        });
    }

    @Override
    public Trip dispatch(UUID id, String actor, String remarks) {
        var dispatchedBy = actor(actor);
        return transactions.execute(() -> {
            var trip = getForUpdate(id);
            lifecycle.requireDispatchable(trip);
            var licenseClass = history.findCurrentDriverLicenseClass(trip.id(), trip.driverId())
                    .orElseThrow(() -> new ConflictException("ASSIGNMENT_INCOMPLETE",
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

            var occurredAt = now();
            var dispatched = repo.save(copy(trip, TripLifecyclePolicy.DISPATCHED, trip.vehicleId(), trip.driverId(),
                    trip.actualStartTime(), trip.actualEndTime(), trip.startOdometerKm(), trip.endOdometerKm(),
                    trip.completionRemarks(), occurredAt));
            dispatches.save(new TripDispatchRecord(trip.id(), occurredAt, dispatchedBy, remarks));
            history.save(new TripHistoryEntry(UUID.randomUUID(), trip.id(), trip.status(), dispatched.status(),
                    "TRIP_DISPATCHED", trip.vehicleId(), trip.driverId(), licenseClass, dispatchedBy,
                    remarks == null || remarks.isBlank() ? "Trip dispatched" : remarks.trim(), occurredAt));
            return dispatched;
        });
    }

    @Override
    public List<TripHistoryEntry> history(UUID id) {
        get(id);
        return history.findByTripId(id);
    }

    private Trip apply(Trip trip, TripCommand command, OffsetDateTime occurredAt) {
        var target = lifecycle.targetStatus(command);
        return switch (command) {
            case TripCommand.Submit ignored -> copy(trip, target, trip.vehicleId(), trip.driverId(),
                    trip.actualStartTime(), trip.actualEndTime(), trip.startOdometerKm(), trip.endOdometerKm(),
                    TripLifecyclePolicy.REJECTED.equals(trip.status()) ? null : trip.completionRemarks(), occurredAt);
            case TripCommand.Approve ignored -> copy(trip, target, trip.vehicleId(), trip.driverId(),
                    trip.actualStartTime(), trip.actualEndTime(), trip.startOdometerKm(), trip.endOdometerKm(),
                    null, occurredAt);
            case TripCommand.Reject reject -> copy(trip, target, trip.vehicleId(), trip.driverId(),
                    trip.actualStartTime(), trip.actualEndTime(), trip.startOdometerKm(), trip.endOdometerKm(),
                    reject.reason().trim(), occurredAt);
            case TripCommand.Start start -> copy(trip, target, trip.vehicleId(), trip.driverId(), occurredAt,
                    trip.actualEndTime(), start.odometerKm(), trip.endOdometerKm(), trip.completionRemarks(), occurredAt);
            case TripCommand.Complete complete -> copy(trip, target, trip.vehicleId(), trip.driverId(),
                    trip.actualStartTime(), occurredAt, trip.startOdometerKm(), complete.odometerKm(),
                    complete.remarks() == null || complete.remarks().isBlank() ? null : complete.remarks().trim(),
                    occurredAt);
            case TripCommand.Close ignored -> copy(trip, target, trip.vehicleId(), trip.driverId(),
                    trip.actualStartTime(), trip.actualEndTime(), trip.startOdometerKm(), trip.endOdometerKm(),
                    trip.completionRemarks(), occurredAt);
            case TripCommand.Cancel cancel -> copy(trip, target, trip.vehicleId(), trip.driverId(),
                    trip.actualStartTime(), trip.actualEndTime(), trip.startOdometerKm(), trip.endOdometerKm(),
                    cancel.reason().trim(), occurredAt);
            case TripCommand.Dispatch ignored -> throw new IllegalStateException("Dispatch uses the hardened dispatch flow");
        };
    }

    private boolean affectsEligibility(Trip current, Trip requested) {
        return !Objects.equals(current.requestedStartTime(), requested.requestedStartTime())
                || !Objects.equals(current.requestedEndTime(), requested.requestedEndTime())
                || !Objects.equals(current.requiredVehicleTypeId(), requested.requiredVehicleTypeId())
                || !Objects.equals(current.requiredCapacityKg(), requested.requiredCapacityKg())
                || !Objects.equals(current.routeId(), requested.routeId())
                || !Objects.equals(current.originLocationId(), requested.originLocationId())
                || !Objects.equals(current.destinationLocationId(), requested.destinationLocationId());
    }

    private Trip copy(Trip trip, String status, UUID vehicleId, UUID driverId, OffsetDateTime actualStart,
                      OffsetDateTime actualEnd, Double startOdometer, Double endOdometer, String remarks,
                      OffsetDateTime updatedAt) {
        return new Trip(trip.id(), trip.tripNumber(), trip.customerId(), trip.departmentId(), trip.projectId(),
                trip.routeId(), trip.priority(), status, trip.originLocationId(), trip.destinationLocationId(),
                trip.requestedStartTime(), trip.requestedEndTime(), trip.requiredVehicleTypeId(),
                trip.requiredCapacityKg(), trip.cargoDescription(), trip.passengerCount(), trip.customerInstructions(),
                trip.notes(), vehicleId, driverId, actualStart, actualEnd, startOdometer, endOdometer, remarks,
                trip.createdAt(), updatedAt);
    }

    private String actor(String actor) {
        return actor == null || actor.isBlank() ? "system" : actor.trim();
    }

    private OffsetDateTime now() {
        return OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
    }

    private Trip getForUpdate(UUID id) {
        var locked = repo.findByIdForUpdate(id);
        return (locked == null ? java.util.Optional.<Trip>empty() : locked)
                .or(() -> repo.findById(id))
                .orElseThrow(() -> new NotFoundException("Trip not found: " + id));
    }

    private void validateRoute(Trip trip) {
        if (trip.routeId() != null) {
            routeEligibility.assertAssignable(trip.routeId(), trip.originLocationId(), trip.destinationLocationId());
        }
    }

    private static RouteEligibilityPort noOpRouteEligibility() {
        return (routeId, originLocationId, destinationLocationId) -> { };
    }
}
