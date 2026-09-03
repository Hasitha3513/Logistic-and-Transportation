package com.transportlogistics.app.freight.loadplanning.application;

import com.transportlogistics.app.freight.loadplanning.domain.LoadPlan;
import com.transportlogistics.app.freight.loadplanning.domain.LoadPlanItemPlacement;
import com.transportlogistics.app.freight.loadplanning.domain.LoadPlanReadinessStatus;
import com.transportlogistics.app.freight.loadplanning.domain.LoadPlanViolation;
import com.transportlogistics.app.freight.loadplanning.domain.ManifestItemFact;
import com.transportlogistics.app.freight.loadplanning.domain.LoadValidationResult;
import com.transportlogistics.app.freight.loadplanning.domain.WeightVolumeCalculationEngine;
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
                    LoadPlanReadinessStatus.DRAFT,
                    null,
                    null,
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
    public LoadPlan markReady(UUID id, Long expectedVersion, String actor) {
        return transactions.execute(() -> {
            requireActor(actor);
            LoadPlan current = get(id);
            requireVersion(current, expectedVersion);

            ManifestPlanningView manifest = getAndValidateManifest(current.getCargoManifestId());
            getAndValidateVehicle(current.getVehicleId());

            List<ManifestItemFact> itemFacts = manifest.items().stream()
                    .map(i -> new ManifestItemFact(i.itemId(), i.hazardous(), i.fragile(), i.temperatureSensitive()))
                    .toList();

            List<LoadPlanViolation> violations = current.validate(itemFacts);
            if (!violations.isEmpty()) {
                String details = violations.stream()
                        .map(v -> v.code().name() + ": " + v.message())
                        .collect(Collectors.joining("; "));
                throw new BusinessRuleException("LOAD_PLAN_STRUCTURAL_VIOLATIONS", details);
            }

            OffsetDateTime now = OffsetDateTime.now(clock);
            LoadPlan readyPlan = current.markStructurallyReady(actor, now);
            LoadPlan saved = repository.save(readyPlan);
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
        ManifestPlanningView manifest = getAndValidateManifest(loadPlan.getCargoManifestId());
        VehiclePlanningView vehicle = getAndValidateVehicle(loadPlan.getVehicleId());

        OffsetDateTime now = OffsetDateTime.now(clock);

        WeightVolumeCalculationEngine.VehicleCapacityInput vehicleInput = new WeightVolumeCalculationEngine.VehicleCapacityInput(
                vehicle.capacityKg(),
                vehicle.tareWeightKg(),
                vehicle.grossVehicleWeightKg(),
                vehicle.cargoVolumeCapacityM3(),
                vehicle.axleCount(),
                vehicle.maxAxleLoadKg()
        );

        List<WeightVolumeCalculationEngine.CargoLineMeasurement> cargoMeasurements = new ArrayList<>();
        if (manifest.items() != null) {
            for (CargoManifestLookupPort.ManifestItemPlanningView item : manifest.items()) {
                WeightVolumeCalculationEngine.WeightUnit weightUnit = item.weightUnit() != null ? parseWeightUnit(item.weightUnit()) : item.unitWeight() != null ? WeightVolumeCalculationEngine.WeightUnit.KG : null;
                WeightVolumeCalculationEngine.DimensionUnit dimensionUnit = item.dimensionUnit() != null ? parseDimensionUnit(item.dimensionUnit()) : item.length() != null || item.width() != null || item.height() != null ? WeightVolumeCalculationEngine.DimensionUnit.M : null;
                cargoMeasurements.add(new WeightVolumeCalculationEngine.CargoLineMeasurement(
                        item.quantity(),
                        item.unitWeight(),
                        weightUnit,
                        item.length(),
                        item.width(),
                        item.height(),
                        dimensionUnit
                ));
            }
        }

        WeightVolumeCalculationEngine.EvaluationResult evaluation = WeightVolumeCalculationEngine.evaluate(vehicleInput, cargoMeasurements);

        return new LoadValidationResult(
                loadPlan.getLoadPlanId(),
                now,
                actor,
                evaluation.overallOutcome(),
                evaluation.cargoWeightKg(),
                evaluation.payloadCapacityKg(),
                evaluation.payloadUtilizationPercent(),
                evaluation.cargoVolumeM3(),
                evaluation.volumeCapacityM3(),
                evaluation.volumeUtilizationPercent(),
                evaluation.projectedGrossWeightKg(),
                evaluation.grossWeightLimitKg(),
                evaluation.tareWeightKg(),
                evaluation.payloadResult(),
                evaluation.volumeResult(),
                evaluation.gvwResult(),
                evaluation.axleResult(),
                evaluation.violations(),
                evaluation.missingData()
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
            throw new BusinessRuleException("LOAD_PLAN_VERSION_REQUIRED", "Version is required");
        }
        if (version != current.getVersion()) {
            throw new ConflictException("LOAD_PLAN_STALE_VERSION", "Load plan was changed by another user");
        }
    }

    private void requireActor(String actor) {
        if (actor == null || actor.isBlank()) {
            throw new BusinessRuleException("LOAD_PLAN_ACTOR_REQUIRED", "An authenticated actor is required");
        }
    }

    private WeightVolumeCalculationEngine.WeightUnit parseWeightUnit(String unit) {
        if (unit == null || unit.isBlank()) return WeightVolumeCalculationEngine.WeightUnit.KG;
        try {
            return WeightVolumeCalculationEngine.WeightUnit.valueOf(unit.toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return WeightVolumeCalculationEngine.WeightUnit.KG;
        }
    }

    private WeightVolumeCalculationEngine.DimensionUnit parseDimensionUnit(String unit) {
        if (unit == null || unit.isBlank()) return WeightVolumeCalculationEngine.DimensionUnit.M;
        try {
            return WeightVolumeCalculationEngine.DimensionUnit.valueOf(unit.toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return WeightVolumeCalculationEngine.DimensionUnit.M;
        }
    }
}
