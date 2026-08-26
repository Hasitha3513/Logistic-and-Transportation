package com.transportlogistics.app.freight.loadplanning.application;

import com.transportlogistics.app.freight.loadplanning.domain.LoadPlan;
import com.transportlogistics.app.freight.loadplanning.domain.LoadPlanItemPlacement;
import com.transportlogistics.app.freight.loadplanning.domain.LoadPlanViolation;
import com.transportlogistics.app.freight.loadplanning.domain.ManifestItemFact;
import com.transportlogistics.app.freight.loadplanning.domain.LoadValidationResult;
import com.transportlogistics.app.freight.loadplanning.domain.LoadValidationViolation;
import com.transportlogistics.app.freight.loadplanning.domain.ValidationOutcome;
import com.transportlogistics.app.freight.loadplanning.domain.event.LoadPlanCreated;
import com.transportlogistics.app.freight.loadplanning.domain.event.LoadPlanUpdated;
import com.transportlogistics.app.freight.loadplanning.ports.inbound.CargoManifestLookupPort;
import com.transportlogistics.app.freight.loadplanning.ports.inbound.CargoManifestLookupPort.ManifestItemPlanningView;
import com.transportlogistics.app.freight.loadplanning.ports.inbound.CargoManifestLookupPort.ManifestPlanningView;
import com.transportlogistics.app.freight.loadplanning.ports.inbound.LoadPlanUseCase;
import com.transportlogistics.app.freight.loadplanning.ports.inbound.VehicleLoadSpaceLookupPort;
import com.transportlogistics.app.freight.loadplanning.ports.inbound.VehicleLoadSpaceLookupPort.VehiclePlanningView;
import com.transportlogistics.app.freight.loadplanning.ports.outbound.LoadPlanEventPublisher;
import com.transportlogistics.app.freight.loadplanning.ports.outbound.LoadPlanNumberGenerator;
import com.transportlogistics.app.freight.loadplanning.ports.outbound.LoadPlanRepository;
import com.transportlogistics.app.freight.loadplanning.ports.outbound.LoadPlanTransaction;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.shared.domain.ConflictException;
import com.transportlogistics.app.shared.domain.NotFoundException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Application service implementing the LoadPlanUseCase inbound port.
 *
 * <p>Coordinates domain logic, cross-module/feature lookups, transactions, and event publishing.</p>
 */
public final class LoadPlanService implements LoadPlanUseCase {

    private final LoadPlanRepository repository;
    private final LoadPlanNumberGenerator numberGenerator;
    private final CargoManifestLookupPort manifestLookup;
    private final VehicleLoadSpaceLookupPort vehicleLookup;
    private final LoadPlanEventPublisher eventPublisher;
    private final LoadPlanTransaction transactions;
    private final Clock clock;

    public LoadPlanService(LoadPlanRepository repository,
                           LoadPlanNumberGenerator numberGenerator,
                           CargoManifestLookupPort manifestLookup,
                           VehicleLoadSpaceLookupPort vehicleLookup,
                           LoadPlanEventPublisher eventPublisher,
                           LoadPlanTransaction transactions,
                           Clock clock) {
        this.repository = Objects.requireNonNull(repository, "repository is required");
        this.numberGenerator = Objects.requireNonNull(numberGenerator, "numberGenerator is required");
        this.manifestLookup = Objects.requireNonNull(manifestLookup, "manifestLookup is required");
        this.vehicleLookup = Objects.requireNonNull(vehicleLookup, "vehicleLookup is required");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher is required");
        this.transactions = Objects.requireNonNull(transactions, "transactions is required");
        this.clock = Objects.requireNonNull(clock, "clock is required");
    }

    @Override
    public LoadPlan create(CreateCommand command, String actor) {
        return transactions.execute(() -> {
            requireActor(actor);
            if (command.cargoManifestId() == null) {
                throw new BusinessRuleException("CARGO_MANIFEST_ID_REQUIRED", "Cargo manifest ID is required");
            }
            if (command.vehicleId() == null) {
                throw new BusinessRuleException("VEHICLE_ID_REQUIRED", "Vehicle ID is required");
            }

            ManifestPlanningView manifest = getAndValidateManifest(command.cargoManifestId());
            VehiclePlanningView vehicle = getAndValidateVehicle(command.vehicleId());

            Set<UUID> validManifestItemIds = manifest.items().stream()
                    .map(ManifestItemPlanningView::itemId)
                    .collect(Collectors.toSet());

            List<LoadPlanItemPlacement> placements = mapPlacements(command.placements(), validManifestItemIds);

            OffsetDateTime now = OffsetDateTime.now(clock);
            String number = numberGenerator.next();

            LoadPlan loadPlan = new LoadPlan(
                    UUID.randomUUID(),
                    number,
                    manifest.manifestId(),
                    vehicle.vehicleId(),
                    placements,
                    command.notes(),
                    now,
                    now,
                    actor,
                    actor,
                    0L
            );

            LoadPlan saved = repository.save(loadPlan);
            eventPublisher.publishLoadPlanCreated(new LoadPlanCreated(saved.getLoadPlanId(), now));
            return saved;
        });
    }

    @Override
    public LoadPlan get(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("LOAD_PLAN_NOT_FOUND", "Load plan not found: " + id));
    }

    @Override
    public List<LoadPlan> list() {
        return repository.findAll();
    }

    @Override
    public LoadPlan update(UUID id, UpdateCommand command, String actor) {
        return transactions.execute(() -> {
            requireActor(actor);
            if (command.vehicleId() == null) {
                throw new BusinessRuleException("VEHICLE_ID_REQUIRED", "Vehicle ID is required");
            }

            LoadPlan current = get(id);
            requireVersion(current, command.version());

            ManifestPlanningView manifest = getAndValidateManifest(current.getCargoManifestId());
            VehiclePlanningView vehicle = getAndValidateVehicle(command.vehicleId());

            Set<UUID> validManifestItemIds = manifest.items().stream()
                    .map(ManifestItemPlanningView::itemId)
                    .collect(Collectors.toSet());

            List<LoadPlanItemPlacement> placements = mapPlacements(command.placements(), validManifestItemIds);

            OffsetDateTime now = OffsetDateTime.now(clock);
            LoadPlan updated = current.update(
                    vehicle.vehicleId(),
                    placements,
                    command.notes(),
                    actor,
                    now
            );

            LoadPlan saved = repository.save(updated);
            eventPublisher.publishLoadPlanUpdated(new LoadPlanUpdated(saved.getLoadPlanId(), now));
            return saved;
        });
    }

    @Override
    public List<LoadPlanViolation> validateLayout(UUID id) {
        LoadPlan loadPlan = get(id);
        ManifestPlanningView manifest = getAndValidateManifest(loadPlan.getCargoManifestId());

        List<ManifestItemFact> itemFacts = manifest.items().stream()
                .map(i -> new ManifestItemFact(i.itemId(), i.hazardous(), i.fragile(), i.temperatureSensitive()))
                .toList();

        return loadPlan.validate(itemFacts);
    }

    @Override
    public LoadValidationResult validateWeightAndVolume(UUID id, String actor) {
        requireActor(actor);
        LoadPlan loadPlan = get(id);
        getAndValidateManifest(loadPlan.getCargoManifestId());
        VehiclePlanningView vehicle = getAndValidateVehicle(loadPlan.getVehicleId());

        OffsetDateTime now = OffsetDateTime.now(clock);
        List<LoadValidationViolation> violations = new ArrayList<>();
        List<String> missingData = new ArrayList<>();

        missingData.add("CARGO_ITEM_WEIGHT_DATA_MISSING");
        missingData.add("CARGO_ITEM_DIMENSIONS_DATA_MISSING");
        missingData.add("VEHICLE_VOLUME_CAPACITY_UNAVAILABLE");
        missingData.add("VEHICLE_AXLE_LIMITS_UNAVAILABLE");

        violations.add(new LoadValidationViolation(
                "LOAD_WEIGHT_DATA_MISSING",
                "Cargo item weight measurements are unavailable to compute gross weight"
        ));
        violations.add(new LoadValidationViolation(
                "LOAD_VOLUME_DATA_MISSING",
                "Cargo item dimensions and vehicle volume capacity are unavailable to compute cubic volume"
        ));
        violations.add(new LoadValidationViolation(
                "LOAD_AXLE_DATA_UNAVAILABLE",
                "Vehicle axle configuration and legal axle limits are unavailable"
        ));

        ValidationOutcome payloadResult = ValidationOutcome.INCOMPLETE;
        ValidationOutcome volumeResult = ValidationOutcome.INCOMPLETE;
        ValidationOutcome axleResult = ValidationOutcome.INCOMPLETE;
        ValidationOutcome overallOutcome = ValidationOutcome.INCOMPLETE;

        return new LoadValidationResult(
                loadPlan.getLoadPlanId(),
                now,
                actor,
                overallOutcome,
                null,
                null,
                null,
                payloadResult,
                volumeResult,
                axleResult,
                violations,
                missingData
        );
    }

    private ManifestPlanningView getAndValidateManifest(UUID manifestId) {
        ManifestPlanningView manifest = manifestLookup.findManifest(manifestId)
                .orElseThrow(() -> new NotFoundException("CARGO_MANIFEST_NOT_FOUND", "Cargo manifest not found: " + manifestId));
        if (!manifest.finalized()) {
            throw new ConflictException("CARGO_MANIFEST_NOT_FINALIZED", "Cannot plan loads for unfinalized cargo manifest: " + manifestId);
        }
        return manifest;
    }

    private VehiclePlanningView getAndValidateVehicle(UUID vehicleId) {
        VehiclePlanningView vehicle = vehicleLookup.findVehicle(vehicleId)
                .orElseThrow(() -> new NotFoundException("VEHICLE_NOT_FOUND", "Vehicle not found: " + vehicleId));
        if (!vehicle.active()) {
            throw new ConflictException("VEHICLE_INACTIVE", "Vehicle is not active: " + vehicleId);
        }
        return vehicle;
    }

    private List<LoadPlanItemPlacement> mapPlacements(List<ItemPlacementCommand> placementCommands,
                                                      Set<UUID> validManifestItemIds) {
        if (placementCommands == null || placementCommands.isEmpty()) {
            return List.of();
        }

        List<LoadPlanItemPlacement> result = new ArrayList<>();
        Set<UUID> seenItems = new HashSet<>();

        for (ItemPlacementCommand cmd : placementCommands) {
            if (cmd.manifestItemId() == null) {
                throw new BusinessRuleException("MANIFEST_ITEM_ID_REQUIRED", "Manifest item ID is required for placement");
            }
            if (!validManifestItemIds.contains(cmd.manifestItemId())) {
                throw new BusinessRuleException("INVALID_MANIFEST_ITEM", "Manifest item " + cmd.manifestItemId() + " does not belong to manifest");
            }
            if (!seenItems.add(cmd.manifestItemId())) {
                throw new BusinessRuleException("DUPLICATE_PLACEMENT_COMMAND", "Manifest item " + cmd.manifestItemId() + " is placed more than once");
            }

            result.add(new LoadPlanItemPlacement(
                    UUID.randomUUID(),
                    cmd.manifestItemId(),
                    cmd.placementOrder(),
                    cmd.zoneReference(),
                    cmd.stackGroup(),
                    cmd.containerReference(),
                    cmd.loadingSequence(),
                    cmd.specialHandlingNotes()
            ));
        }

        return result;
    }

    private void requireVersion(LoadPlan current, Long version) {
        if (version == null) {
            throw new BusinessRuleException("LOAD_PLAN_VERSION_REQUIRED", "Version is required for update");
        }
        if (version != current.getVersion()) {
            throw new ConflictException("LOAD_PLAN_CONCURRENT_UPDATE", "Load plan was changed by another user");
        }
    }

    private void requireActor(String actor) {
        if (actor == null || actor.isBlank()) {
            throw new BusinessRuleException("LOAD_PLAN_ACTOR_REQUIRED", "An authenticated actor is required");
        }
    }
}
