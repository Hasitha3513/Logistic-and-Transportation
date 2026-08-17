package com.transportlogistics.app.fuel.application.service;

import com.transportlogistics.app.fuel.FuelIssueAuthorized;
import com.transportlogistics.app.fuel.FuelIssueCancelled;
import com.transportlogistics.app.fuel.FuelIssued;
import com.transportlogistics.app.fuel.application.ports.in.FuelIssueUseCase;
import com.transportlogistics.app.fuel.application.ports.out.*;
import com.transportlogistics.app.fuel.domain.model.FuelIssue;
import com.transportlogistics.app.fuel.domain.model.FuelIssueHistory;
import com.transportlogistics.app.fuel.domain.model.FuelIssueStatus;
import com.transportlogistics.app.fuel.domain.service.FuelIssuePolicy;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.shared.domain.NotFoundException;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public final class FuelIssueService implements FuelIssueUseCase {
    private static final Set<String> INVALID_VEHICLE_STATUSES = Set.of(
            "RETIRED", "OUT_OF_SERVICE", "BROKEN_DOWN", "BREAKDOWN", "MAINTENANCE", "INACTIVE");
    private static final Set<String> FUEL_ELIGIBLE_TRIP_STATUSES = Set.of("ASSIGNED", "DISPATCHED", "IN_PROGRESS");

    private final FuelIssueRepository issues;
    private final FuelIssueHistoryRepository history;
    private final FuelStationRepository stations;
    private final FuelLimitPolicyRepository limits;
    private final VehicleFuelContextPort vehicles;
    private final TripFuelContextPort trips;
    private final FuelActorPort actors;
    private final FuelVehicleReadingPort vehicleReadings;
    private final FuelVoucherGenerator vouchers;
    private final FuelTransaction transactions;
    private final FuelEventPublisher events;
    private final Clock clock;
    private final FuelIssuePolicy policy = new FuelIssuePolicy();

    public FuelIssueService(FuelIssueRepository issues, FuelIssueHistoryRepository history,
                            FuelStationRepository stations, FuelLimitPolicyRepository limits,
                            VehicleFuelContextPort vehicles, TripFuelContextPort trips, FuelActorPort actors,
                            FuelVoucherGenerator vouchers, FuelTransaction transactions, FuelEventPublisher events,
                            Clock clock) {
        this(issues, history, stations, limits, vehicles, trips, actors, noOpVehicleReadingPort(), vouchers,
                transactions, events, clock);
    }

    public FuelIssueService(FuelIssueRepository issues, FuelIssueHistoryRepository history,
                            FuelStationRepository stations, FuelLimitPolicyRepository limits,
                            VehicleFuelContextPort vehicles, TripFuelContextPort trips, FuelActorPort actors,
                            FuelVehicleReadingPort vehicleReadings, FuelVoucherGenerator vouchers,
                            FuelTransaction transactions, FuelEventPublisher events, Clock clock) {
        this.issues = issues;
        this.history = history;
        this.stations = stations;
        this.limits = limits;
        this.vehicles = vehicles;
        this.trips = trips;
        this.actors = actors;
        this.vehicleReadings = vehicleReadings;
        this.vouchers = vouchers;
        this.transactions = transactions;
        this.events = events;
        this.clock = clock;
    }

    private static FuelVehicleReadingPort noOpVehicleReadingPort() {
        return (vehicleId, fuelIssueId, odometerKm, engineHours, issueDateTime, actorId) -> {
        };
    }

    @Override
    public FuelIssue create(CreateCommand command, String actorName) {
        return transactions.execute(() -> {
            var actor = actor(actorName);
            var now = OffsetDateTime.now(clock);
            var issue = new FuelIssue(UUID.randomUUID(), vouchers.next(command.issueDateTime() == null
                    ? now.getYear() : command.issueDateTime().getYear()), command.vehicleId(), command.tripId(),
                    command.driverId(), normalizeFuelType(command.fuelType()), command.quantity(), command.unitPrice(),
                    FuelIssue.total(command.quantity(), command.unitPrice()), command.stationId(), command.odometer(),
                    command.engineHours(), command.issueDateTime(), FuelIssueStatus.DRAFT, actor.id(), null, null,
                    trim(command.notes()), now, now);
            validateOperational(issue);
            var saved = issues.save(issue);
            append(saved, null, FuelIssueStatus.DRAFT, "CREATED", actor, "Fuel issue created", now);
            return saved;
        });
    }

    @Override
    public FuelIssue update(UUID id, UpdateCommand command, String actorName) {
        return transactions.execute(() -> {
            var actor = actor(actorName);
            var current = locked(id);
            policy.requireEditable(current);
            var now = OffsetDateTime.now(clock);
            var requested = new FuelIssue(current.id(), current.voucherNumber(), command.vehicleId(), command.tripId(),
                    command.driverId(), normalizeFuelType(command.fuelType()), command.quantity(), command.unitPrice(),
                    FuelIssue.total(command.quantity(), command.unitPrice()), command.stationId(), command.odometer(),
                    command.engineHours(), command.issueDateTime(), current.status(), current.requestedBy(), null, null,
                    trim(command.notes()), current.createdAt(), now);
            validateOperational(requested);
            var saved = issues.save(requested);
            append(saved, current.status(), current.status(), "UPDATED", actor, "Fuel issue updated", now);
            return saved;
        });
    }

    @Override
    public FuelIssue submit(UUID id, String actorName) {
        return transition(id, actorName, FuelIssueStatus.PENDING_AUTHORIZATION, "SUBMITTED", null, issue -> {
            policy.requireSubmittable(issue);
            validateOperational(issue);
        });
    }

    @Override
    public FuelIssue authorize(UUID id, String comment, String actorName) {
        return transactions.execute(() -> {
            var actor = actor(actorName);
            var current = locked(id);
            policy.requireAuthorizable(current);
            validateOperational(current);
            var now = OffsetDateTime.now(clock);
            var saved = issues.save(copy(current, FuelIssueStatus.AUTHORIZED, actor.id(), now, now));
            append(saved, current.status(), saved.status(), "AUTHORIZED", actor, trim(comment), now);
            events.publish(new FuelIssueAuthorized(saved.id(), saved.voucherNumber(), actor.id(), now));
            return saved;
        });
    }

    @Override
    public FuelIssue issue(UUID id, String actorName) {
        return transactions.execute(() -> {
            var actor = actor(actorName);
            var current = locked(id);
            policy.requireIssuable(current);
            validateOperational(current);
            vehicleReadings.recordIssue(current.vehicleId(), current.id(), current.odometer(),
                    current.engineHours(), current.issueDateTime(), actor.id());
            var now = OffsetDateTime.now(clock);
            var saved = issues.save(copy(current, FuelIssueStatus.ISSUED, current.authorizedBy(),
                    current.authorizationDateTime(), now));
            append(saved, current.status(), saved.status(), "ISSUED", actor, "Fuel issued", now);
            events.publish(new FuelIssued(saved.id(), saved.voucherNumber(), saved.vehicleId(), saved.tripId(),
                    saved.quantity(), now));
            return saved;
        });
    }

    @Override
    public FuelIssue cancel(UUID id, String reason, String actorName) {
        return transactions.execute(() -> {
            var actor = actor(actorName);
            var current = locked(id);
            policy.requireCancellable(current, reason);
            var now = OffsetDateTime.now(clock);
            var saved = issues.save(copy(current, FuelIssueStatus.CANCELLED, current.authorizedBy(),
                    current.authorizationDateTime(), now));
            var cancellationReason = trim(reason);
            append(saved, current.status(), saved.status(), "CANCELLED", actor,
                    cancellationReason == null ? "Draft fuel issue cancelled" : cancellationReason, now);
            events.publish(new FuelIssueCancelled(saved.id(), saved.voucherNumber(), actor.id(),
                    cancellationReason, now));
            return saved;
        });
    }

    @Override
    public FuelIssue get(UUID id) {
        return issues.findById(id).orElseThrow(() -> notFound(id));
    }

    @Override
    public PageResult<FuelIssue> search(SearchQuery query) {
        return issues.search(query);
    }

    @Override
    public List<FuelIssueHistory> history(UUID id) {
        get(id);
        return history.findByFuelIssueId(id);
    }

    private FuelIssue transition(UUID id, String actorName, FuelIssueStatus target, String action, String comment,
                                 java.util.function.Consumer<FuelIssue> guard) {
        return transactions.execute(() -> {
            var actor = actor(actorName);
            var current = locked(id);
            guard.accept(current);
            var now = OffsetDateTime.now(clock);
            var saved = issues.save(copy(current, target, current.authorizedBy(),
                    current.authorizationDateTime(), now));
            append(saved, current.status(), saved.status(), action, actor, comment, now);
            return saved;
        });
    }

    private void validateOperational(FuelIssue issue) {
        policy.validateDraft(issue);
        var vehicle = vehicles.find(issue.vehicleId()).orElseThrow(() ->
                new BusinessRuleException("FUEL_VEHICLE_NOT_FOUND", "Vehicle not found: " + issue.vehicleId()));
        if (!vehicle.active() || INVALID_VEHICLE_STATUSES.contains(normalize(vehicle.operationalStatus()))) {
            throw new BusinessRuleException("FUEL_VEHICLE_INELIGIBLE", "Vehicle is not eligible for operational fuel issue");
        }
        var station = stations.findById(issue.stationId()).orElseThrow(() ->
                new BusinessRuleException("FUEL_STATION_NOT_FOUND", "Fuel station not found: " + issue.stationId()));
        if (!station.active()) {
            throw new BusinessRuleException("FUEL_STATION_INACTIVE", "Inactive fuel station cannot issue fuel");
        }
        validateTrip(issue);
        policy.enforceLimits(issue, limits.findApplicable(issue.vehicleId()));
    }

    private void validateTrip(FuelIssue issue) {
        if (issue.tripId() == null) return;
        var trip = trips.find(issue.tripId()).orElseThrow(() ->
                new BusinessRuleException("FUEL_TRIP_NOT_FOUND", "Trip not found: " + issue.tripId()));
        if (!FUEL_ELIGIBLE_TRIP_STATUSES.contains(normalize(trip.status()))) {
            throw new BusinessRuleException("FUEL_TRIP_NOT_ELIGIBLE",
                    "Trip status " + trip.status() + " is not eligible for fuel issue");
        }
        if (trip.vehicleId() == null || !trip.vehicleId().equals(issue.vehicleId())) {
            throw new BusinessRuleException("FUEL_VEHICLE_TRIP_MISMATCH",
                    "Fuel issue vehicle must match the vehicle assigned to the trip");
        }
        if (issue.driverId() != null && (trip.driverId() == null || !trip.driverId().equals(issue.driverId()))) {
            throw new BusinessRuleException("FUEL_DRIVER_TRIP_MISMATCH",
                    "Fuel issue driver must match the driver assigned to the trip");
        }
    }

    private FuelActorPort.Actor actor(String username) {
        if (username == null || username.isBlank()) {
            throw new BusinessRuleException("FUEL_ACTOR_REQUIRED", "An authenticated actor is required");
        }
        return actors.find(username).orElseThrow(() ->
                new BusinessRuleException("FUEL_ACTOR_NOT_FOUND", "Authenticated user could not be resolved"));
    }

    private FuelIssue locked(UUID id) {
        return issues.findByIdForUpdate(id).orElseThrow(() -> notFound(id));
    }

    private NotFoundException notFound(UUID id) {
        return new NotFoundException("FUEL_ISSUE_NOT_FOUND", "Fuel issue not found: " + id);
    }

    private FuelIssue copy(FuelIssue issue, FuelIssueStatus status, UUID authorizedBy,
                           OffsetDateTime authorizationDateTime, OffsetDateTime updatedAt) {
        return new FuelIssue(issue.id(), issue.voucherNumber(), issue.vehicleId(), issue.tripId(), issue.driverId(),
                issue.fuelType(), issue.quantity(), issue.unitPrice(), issue.totalAmount(), issue.stationId(),
                issue.odometer(), issue.engineHours(), issue.issueDateTime(), status, issue.requestedBy(),
                authorizedBy, authorizationDateTime, issue.notes(), issue.createdAt(), updatedAt);
    }

    private void append(FuelIssue issue, FuelIssueStatus from, FuelIssueStatus to, String action,
                        FuelActorPort.Actor actor, String comment, OffsetDateTime occurredAt) {
        history.save(new FuelIssueHistory(UUID.randomUUID(), issue.id(), from, to, action, actor.id(),
                actor.username(), comment, occurredAt));
    }

    private String normalizeFuelType(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
