package com.transportlogistics.app.fuel.application.service;

import com.transportlogistics.app.fuel.FuelIssueAuthorized;
import com.transportlogistics.app.fuel.FuelIssueCancelled;
import com.transportlogistics.app.fuel.FuelIssued;
import com.transportlogistics.app.fuel.application.ports.in.FuelIssueUseCase;
import com.transportlogistics.app.fuel.application.ports.out.FuelActorPort;
import com.transportlogistics.app.fuel.application.ports.out.FuelEventPublisher;
import com.transportlogistics.app.fuel.application.ports.out.FuelIssueHistoryRepository;
import com.transportlogistics.app.fuel.application.ports.out.FuelIssueRepository;
import com.transportlogistics.app.fuel.application.ports.out.FuelLimitPolicyRepository;
import com.transportlogistics.app.fuel.application.ports.out.FuelStationRepository;
import com.transportlogistics.app.fuel.application.ports.out.FuelTransaction;
import com.transportlogistics.app.fuel.application.ports.out.FuelVoucherGenerator;
import com.transportlogistics.app.fuel.application.ports.out.TripFuelContextPort;
import com.transportlogistics.app.fuel.application.ports.out.VehicleFuelContextPort;
import com.transportlogistics.app.fuel.application.ports.out.FuelPriceRepository;
import com.transportlogistics.app.fuel.application.ports.out.BunkerStockLedgerRepository;
import com.transportlogistics.app.fuel.application.ports.out.BunkerTankRepository;
import com.transportlogistics.app.fuel.domain.model.BunkerMovementType;
import com.transportlogistics.app.fuel.domain.model.BunkerReferenceType;
import com.transportlogistics.app.fuel.domain.model.BunkerStockMovement;
import com.transportlogistics.app.fuel.domain.model.FuelIssue;
import com.transportlogistics.app.fuel.domain.model.FuelIssueHistory;
import com.transportlogistics.app.fuel.domain.model.FuelIssueStatus;
import com.transportlogistics.app.fuel.domain.model.FuelStation;
import com.transportlogistics.app.fuel.domain.service.FuelIssuePolicy;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.shared.domain.NotFoundException;

import com.transportlogistics.app.fuel.application.ports.out.FuelVehicleReadingPort;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
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
    private final FuelVoucherGenerator vouchers;
    private final FuelTransaction transactions;
    private final FuelEventPublisher events;
    private final FuelVehicleReadingPort readings;
    private final FuelPriceRepository fuelPrices;
    private final BunkerTankRepository bunkerTanks;
    private final BunkerStockLedgerRepository bunkerMovements;
    private final Clock clock;
    private final FuelIssuePolicy policy = new FuelIssuePolicy();

    // Primary constructor with all dependencies including bunker repositories
    public FuelIssueService(FuelIssueRepository issues, FuelIssueHistoryRepository history,
                            FuelStationRepository stations, FuelLimitPolicyRepository limits,
                            VehicleFuelContextPort vehicles, TripFuelContextPort trips, FuelActorPort actors,
                            FuelVoucherGenerator vouchers, FuelTransaction transactions, FuelEventPublisher events,
                            FuelPriceRepository fuelPrices, FuelVehicleReadingPort readings,
                            BunkerTankRepository bunkerTanks, BunkerStockLedgerRepository bunkerMovements,
                            Clock clock) {
        this.issues = issues;
        this.history = history;
        this.stations = stations;
        this.limits = limits;
        this.vehicles = vehicles;
        this.trips = trips;
        this.actors = actors;
        this.vouchers = vouchers;
        this.transactions = transactions;
        this.events = events;
        this.fuelPrices = fuelPrices;
        this.readings = readings == null ? noOpFuelVehicleReadingPort() : readings;
        this.bunkerTanks = bunkerTanks;
        this.bunkerMovements = bunkerMovements;
        this.clock = clock;
    }

    // Convenience constructor when bunker repositories are not explicitly supplied
    public FuelIssueService(FuelIssueRepository issues, FuelIssueHistoryRepository history,
                            FuelStationRepository stations, FuelLimitPolicyRepository limits,
                            VehicleFuelContextPort vehicles, TripFuelContextPort trips, FuelActorPort actors,
                            FuelVoucherGenerator vouchers, FuelTransaction transactions, FuelEventPublisher events,
                            FuelPriceRepository fuelPrices, FuelVehicleReadingPort readings, Clock clock) {
        this(issues, history, stations, limits, vehicles, trips, actors, vouchers, transactions, events,
                fuelPrices, readings, null, null, clock);
    }

    // Convenience constructor when only price repository is provided (readings default to no‑op)
    public FuelIssueService(FuelIssueRepository issues, FuelIssueHistoryRepository history,
                            FuelStationRepository stations, FuelLimitPolicyRepository limits,
                            VehicleFuelContextPort vehicles, TripFuelContextPort trips, FuelActorPort actors,
                            FuelVoucherGenerator vouchers, FuelTransaction transactions, FuelEventPublisher events,
                            FuelPriceRepository fuelPrices, Clock clock) {
        this(issues, history, stations, limits, vehicles, trips, actors, vouchers, transactions, events,
                fuelPrices, null, null, null, clock);
    }

    // Convenience constructor when only vehicle reading port is provided (price repository may be null)
    public FuelIssueService(FuelIssueRepository issues, FuelIssueHistoryRepository history,
                            FuelStationRepository stations, FuelLimitPolicyRepository limits,
                            VehicleFuelContextPort vehicles, TripFuelContextPort trips, FuelActorPort actors,
                            FuelVoucherGenerator vouchers, FuelTransaction transactions, FuelEventPublisher events,
                            FuelVehicleReadingPort readings, Clock clock) {
        this(issues, history, stations, limits, vehicles, trips, actors, vouchers, transactions, events,
                null, readings, null, null, clock);
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
            var now = OffsetDateTime.now(clock);
            // Resolve effective unit price if not already set
            var unitPrice = current.unitPrice();
            var resolvedStation = current.stationId() != null ? stations.findById(current.stationId()) : Optional.<FuelStation>empty();
            if (unitPrice == null && resolvedStation.isPresent()) {
                var station = resolvedStation.get();
                var date = current.issueDateTime() != null
                        ? current.issueDateTime().toLocalDate()
                        : java.time.LocalDate.now(clock);
                var priceOpt = fuelPrices != null ? fuelPrices.findEffective(station.vendorId(), current.fuelType(), date) : Optional.<com.transportlogistics.app.fuel.domain.model.FuelPrice>empty();
                if (priceOpt.isPresent()) {
                    unitPrice = priceOpt.get().unitPrice();
                }
            }
            // Create a new issue instance with the resolved price (if any)
            var saved = issues.save(new FuelIssue(
                    current.id(), current.voucherNumber(), current.vehicleId(), current.tripId(),
                    current.driverId(), current.fuelType(), current.quantity(), unitPrice,
                    FuelIssue.total(current.quantity(), unitPrice), current.stationId(),
                    current.odometer(), current.engineHours(), current.issueDateTime(),
                    FuelIssueStatus.ISSUED, current.requestedBy(), current.authorizedBy(),
                    current.authorizationDateTime(), current.notes(), current.createdAt(), now));

            // Internal station bunker stock deduction under pessimistic write lock
            if (resolvedStation.isPresent() && resolvedStation.get().isInternal() && bunkerTanks != null) {
                var bunkerTank = bunkerTanks.findActiveByStationAndFuelTypeForUpdate(current.stationId(), current.fuelType())
                        .orElseThrow(() -> new BusinessRuleException("NO_ACTIVE_BUNKER_TANK",
                                "No active bunker tank found for internal station " + current.stationId() + " and fuel type " + current.fuelType()));
                if (bunkerTank.currentStockLiters().compareTo(saved.quantity()) < 0) {
                    throw new BusinessRuleException("INSUFFICIENT_BUNKER_STOCK",
                            "Insufficient bunker stock in tank " + bunkerTank.tankCode() + ". Required: " + saved.quantity()
                                    + " L, Available: " + bunkerTank.currentStockLiters() + " L");
                }
                var newBalance = bunkerTank.currentStockLiters().subtract(saved.quantity());
                bunkerTanks.save(bunkerTank.withStock(newBalance));
                if (bunkerMovements != null && !bunkerMovements.existsByTankIdAndReference(bunkerTank.id(), BunkerReferenceType.FUEL_ISSUE, saved.id())) {
                    bunkerMovements.save(new BunkerStockMovement(
                            UUID.randomUUID(),
                            bunkerTank.id(),
                            bunkerMovements.nextLedgerSequence(bunkerTank.id()),
                            BunkerMovementType.FUEL_ISSUE,
                            saved.quantity(),
                            newBalance,
                            BunkerReferenceType.FUEL_ISSUE,
                            saved.id(),
                            now,
                            actor.id(),
                            "Fuel issue " + saved.voucherNumber() + " to vehicle " + saved.vehicleId(),
                            now
                    ));
                }
            }

            readings.record(saved.vehicleId(), saved.id(), saved.odometer(), saved.engineHours(), now, actor.id());
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
        if (station.isInternal() && bunkerTanks != null) {
            var bunkerTank = bunkerTanks.findActiveByStationAndFuelType(issue.stationId(), issue.fuelType());
            if (bunkerTank.isEmpty()) {
                throw new BusinessRuleException("NO_ACTIVE_BUNKER_TANK",
                        "No active bunker tank found for internal station " + issue.stationId() + " and fuel type " + issue.fuelType());
            }
            if (bunkerTank.get().currentStockLiters().compareTo(issue.quantity()) < 0) {
                throw new BusinessRuleException("INSUFFICIENT_BUNKER_STOCK",
                        "Insufficient bunker stock in tank " + bunkerTank.get().tankCode() + ". Required: " + issue.quantity()
                                + " L, Available: " + bunkerTank.get().currentStockLiters() + " L");
            }
        }
        validateReadings(issue, vehicle);
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

    private void validateReadings(FuelIssue issue, VehicleFuelContextPort.VehicleContext vehicle) {
        if (issue.odometer() != null && vehicle.currentOdometerKm() != null
                && issue.odometer().compareTo(vehicle.currentOdometerKm()) < 0) {
            throw new BusinessRuleException("INVALID_FUEL_ODOMETER",
                    "Fuel issue odometer cannot be lower than the latest vehicle odometer");
        }
        if (issue.engineHours() != null && vehicle.engineHours() != null
                && issue.engineHours().compareTo(vehicle.engineHours()) < 0) {
            throw new BusinessRuleException("INVALID_FUEL_ENGINE_HOURS",
                    "Fuel issue engine hours cannot be lower than the latest vehicle engine hours");
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

    private static FuelVehicleReadingPort noOpFuelVehicleReadingPort() {
        return (vehicleId, fuelIssueId, odometer, engineHours, recordedAt, actorId) -> { };
    }
}
