package com.transportlogistics.app.freight.loadplanning.domain;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Aggregate root representing a Load Plan.
 *
 * <p>This class lives in the pure domain layer — no Spring, JPA, or web dependencies.</p>
 *
 * <p>A load plan references a finalized Cargo Manifest and a Vehicle, and contains
 * placement entries that describe how manifested cargo items should be physically
 * arranged, stacked, sequenced, and separated within the vehicle.</p>
 *
 * <p>Structural validation is performed via {@link #validate(Collection)} which returns
 * planning-level violations. Authoritative weight/capacity validation belongs to US-27.</p>
 */
public final class LoadPlan {

    private final UUID loadPlanId;
    private final String loadPlanNumber;
    private final UUID cargoManifestId;
    private final UUID vehicleId;
    private final List<LoadPlanItemPlacement> placements;
    private final String notes;
    private final LoadPlanReadinessStatus readinessStatus;
    private final OffsetDateTime readyAt;
    private final String readyBy;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime updatedAt;
    private final String createdBy;
    private final String updatedBy;
    private final long version;

    public LoadPlan(UUID loadPlanId,
                    String loadPlanNumber,
                    UUID cargoManifestId,
                    UUID vehicleId,
                    List<LoadPlanItemPlacement> placements,
                    String notes,
                    LoadPlanReadinessStatus readinessStatus,
                    OffsetDateTime readyAt,
                    String readyBy,
                    OffsetDateTime createdAt,
                    OffsetDateTime updatedAt,
                    String createdBy,
                    String updatedBy,
                    long version) {
        Objects.requireNonNull(loadPlanId, "loadPlanId is required");
        Objects.requireNonNull(cargoManifestId, "cargoManifestId is required");
        Objects.requireNonNull(vehicleId, "vehicleId is required");
        if (loadPlanNumber == null || loadPlanNumber.isBlank()) {
            throw new IllegalArgumentException("loadPlanNumber is required");
        }
        if (version < 0) {
            throw new IllegalArgumentException("version must be >= 0");
        }
        this.readinessStatus = readinessStatus != null ? readinessStatus : LoadPlanReadinessStatus.DRAFT;
        if (this.readinessStatus == LoadPlanReadinessStatus.DRAFT) {
            if (readyAt != null || readyBy != null) {
                throw new IllegalArgumentException("Draft load plan must not have readyAt or readyBy audit fields");
            }
        } else if (this.readinessStatus == LoadPlanReadinessStatus.STRUCTURALLY_READY
                && (readyAt == null || readyBy == null || readyBy.isBlank())) {
            throw new IllegalArgumentException("Structurally ready load plan requires readyAt and readyBy audit fields");
        }
        this.loadPlanId = loadPlanId;
        this.loadPlanNumber = loadPlanNumber;
        this.cargoManifestId = cargoManifestId;
        this.vehicleId = vehicleId;
        this.placements = placements == null ? List.of() : List.copyOf(placements);
        this.notes = notes;
        this.readyAt = readyAt;
        this.readyBy = readyBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
        this.version = version;
    }

    public LoadPlan(UUID loadPlanId,
                    String loadPlanNumber,
                    UUID cargoManifestId,
                    UUID vehicleId,
                    List<LoadPlanItemPlacement> placements,
                    String notes,
                    OffsetDateTime createdAt,
                    OffsetDateTime updatedAt,
                    String createdBy,
                    String updatedBy,
                    long version) {
        this(loadPlanId, loadPlanNumber, cargoManifestId, vehicleId, placements, notes,
                LoadPlanReadinessStatus.DRAFT, null, null, createdAt, updatedAt, createdBy, updatedBy, version);
    }

    // ──────────────────────────────────────────────────────────
    // Accessors
    // ──────────────────────────────────────────────────────────

    public UUID getLoadPlanId() { return loadPlanId; }
    public String getLoadPlanNumber() { return loadPlanNumber; }
    public UUID getCargoManifestId() { return cargoManifestId; }
    public UUID getVehicleId() { return vehicleId; }
    public List<LoadPlanItemPlacement> getPlacements() { return placements; }
    public String getNotes() { return notes; }
    public LoadPlanReadinessStatus getReadinessStatus() { return readinessStatus; }
    public OffsetDateTime getReadyAt() { return readyAt; }
    public String getReadyBy() { return readyBy; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public String getCreatedBy() { return createdBy; }
    public String getUpdatedBy() { return updatedBy; }
    public long getVersion() { return version; }

    // ──────────────────────────────────────────────────────────
    // Domain mutations (return new instances — immutable style)
    // ──────────────────────────────────────────────────────────

    /**
     * Returns a new LoadPlan with updated placements, vehicle, notes, and audit info.
     * Material changes (vehicle, placements, order, zone, stack, container, sequence)
     * automatically return readiness to DRAFT and clear readiness audit.
     * Notes-only updates preserve existing readiness status and audit.
     */
    public LoadPlan update(UUID newVehicleId,
                           List<LoadPlanItemPlacement> newPlacements,
                           String newNotes,
                           String actor,
                           OffsetDateTime now) {
        boolean materialChange = isMaterialChange(newVehicleId, newPlacements);
        LoadPlanReadinessStatus newStatus = materialChange ? LoadPlanReadinessStatus.DRAFT : this.readinessStatus;
        OffsetDateTime newReadyAt = materialChange ? null : this.readyAt;
        String newReadyBy = materialChange ? null : this.readyBy;

        return new LoadPlan(
                this.loadPlanId,
                this.loadPlanNumber,
                this.cargoManifestId,
                newVehicleId,
                newPlacements,
                newNotes,
                newStatus,
                newReadyAt,
                newReadyBy,
                this.createdAt,
                now,
                this.createdBy,
                actor,
                this.version
        );
    }

    /**
     * Marks the load plan as structurally ready.
     */
    public LoadPlan markStructurallyReady(String actor, OffsetDateTime now) {
        if (actor == null || actor.isBlank()) {
            throw new IllegalArgumentException("Actor is required to mark structurally ready");
        }
        Objects.requireNonNull(now, "now is required");
        return new LoadPlan(
                this.loadPlanId,
                this.loadPlanNumber,
                this.cargoManifestId,
                this.vehicleId,
                this.placements,
                this.notes,
                LoadPlanReadinessStatus.STRUCTURALLY_READY,
                now,
                actor,
                this.createdAt,
                now,
                this.createdBy,
                actor,
                this.version
        );
    }

    /**
     * Invalidation boundary: returns the load plan to DRAFT status and clears ready audit.
     */
    public LoadPlan invalidateReadiness(String actor, OffsetDateTime now) {
        return new LoadPlan(
                this.loadPlanId,
                this.loadPlanNumber,
                this.cargoManifestId,
                this.vehicleId,
                this.placements,
                this.notes,
                LoadPlanReadinessStatus.DRAFT,
                null,
                null,
                this.createdAt,
                now,
                this.createdBy,
                actor,
                this.version
        );
    }

    private boolean isMaterialChange(UUID newVehicleId, List<LoadPlanItemPlacement> newPlacements) {
        if (!Objects.equals(this.vehicleId, newVehicleId)) {
            return true;
        }
        return !arePlacementsStructurallyEqual(this.placements, newPlacements);
    }

    private static boolean arePlacementsStructurallyEqual(List<LoadPlanItemPlacement> current,
                                                          List<LoadPlanItemPlacement> updated) {
        if (current == null && updated == null) return true;
        if (current == null || updated == null) return false;
        if (current.size() != updated.size()) return false;

        Map<UUID, LoadPlanItemPlacement> currentMap = current.stream()
                .collect(Collectors.toMap(LoadPlanItemPlacement::manifestItemId, p -> p, (a, b) -> a));

        for (LoadPlanItemPlacement up : updated) {
            LoadPlanItemPlacement cur = currentMap.get(up.manifestItemId());
            if (cur == null) return false;
            if (cur.placementOrder() != up.placementOrder()) return false;
            if (!Objects.equals(normalize(cur.zoneReference()), normalize(up.zoneReference()))) return false;
            if (!Objects.equals(normalize(cur.stackGroup()), normalize(up.stackGroup()))) return false;
            if (!Objects.equals(normalize(cur.containerReference()), normalize(up.containerReference()))) return false;
            if (cur.loadingSequence() != up.loadingSequence()) return false;
        }
        return true;
    }

    private static String normalize(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    // ──────────────────────────────────────────────────────────
    // Structural validation
    // ──────────────────────────────────────────────────────────

    /**
     * Validates the load plan against the manifest-owned facts of cargo items,
     * enforcing structured fragile, temperature-sensitive, hazardous, stacking,
     * and sequence rules.
     *
     * @param manifestItems collection of planning facts for each manifested item
     * @return list of violations; empty means structurally valid
     */
    public List<LoadPlanViolation> validate(Collection<ManifestItemFact> manifestItems) {
        List<LoadPlanViolation> violations = new ArrayList<>();
        if (manifestItems == null) {
            return Collections.emptyList();
        }

        Map<UUID, ManifestItemFact> manifestItemMap = manifestItems.stream()
                .collect(Collectors.toMap(ManifestItemFact::itemId, f -> f, (a, b) -> a));

        Set<UUID> placedItemIds = new HashSet<>();

        // Check for duplicate placements
        for (LoadPlanItemPlacement placement : placements) {
            if (!placedItemIds.add(placement.manifestItemId())) {
                violations.add(new LoadPlanViolation(
                        LoadPlanViolationCode.DUPLICATE_PLACEMENT,
                        "Manifest item " + placement.manifestItemId() + " is placed more than once"
                ));
            }
        }

        // Check all manifest items are placed and classifications are known
        for (ManifestItemFact item : manifestItems) {
            if (!placedItemIds.contains(item.itemId())) {
                violations.add(new LoadPlanViolation(
                        LoadPlanViolationCode.ITEM_NOT_PLACED,
                        "Manifest item " + item.itemId() + " has not been placed"
                ));
            }
            if (item.fragile() == null || item.temperatureSensitive() == null) {
                violations.add(new LoadPlanViolation(
                        LoadPlanViolationCode.LOAD_PLAN_SPECIAL_CARGO_CLASSIFICATION_MISSING,
                        "Manifest item " + item.itemId() + " has unknown special cargo classification"
                ));
            }
        }

        // Check stacking conflicts — fragile separation and hazardous separation
        validateStackingRules(violations, manifestItemMap);

        // Check compatibility — temperature-sensitive separation and hazardous separation
        validateCompatibility(violations, manifestItemMap);

        // Check loading sequence validity
        validateLoadingSequence(violations);

        return Collections.unmodifiableList(violations);
    }

    /**
     * Backward-compatible delegation for callers supplying item IDs and hazardous flags.
     */
    public List<LoadPlanViolation> validate(Set<UUID> manifestItemIds,
                                            Set<UUID> hazardousItemIds) {
        List<ManifestItemFact> facts = manifestItemIds.stream()
                .map(id -> new ManifestItemFact(id, hazardousItemIds != null && hazardousItemIds.contains(id), false, false))
                .toList();
        return validate(facts);
    }

    private void validateStackingRules(List<LoadPlanViolation> violations,
                                       Map<UUID, ManifestItemFact> manifestItemMap) {
        Map<String, List<LoadPlanItemPlacement>> stackGroups = placements.stream()
                .filter(p -> p.stackGroup() != null && !p.stackGroup().isBlank())
                .collect(Collectors.groupingBy(LoadPlanItemPlacement::stackGroup));

        for (Map.Entry<String, List<LoadPlanItemPlacement>> entry : stackGroups.entrySet()) {
            List<LoadPlanItemPlacement> group = entry.getValue();

            // Fragile rule: fragile items must not share a nonblank stack group with any other placement
            if (group.size() > 1) {
                for (LoadPlanItemPlacement placement : group) {
                    ManifestItemFact fact = manifestItemMap.get(placement.manifestItemId());
                    if (fact != null && Boolean.TRUE.equals(fact.fragile())) {
                        violations.add(new LoadPlanViolation(
                                LoadPlanViolationCode.LOAD_PLAN_FRAGILE_RULE_FAILED,
                                "Fragile item " + placement.manifestItemId()
                                        + " in stack group '" + entry.getKey() + "' must not share a stack group with other cargo"
                        ));
                    }
                }
            }

            // Hazardous items should not be stacked with non-hazardous
            boolean hasHazardous = group.stream()
                    .anyMatch(p -> {
                        ManifestItemFact fact = manifestItemMap.get(p.manifestItemId());
                        return fact != null && fact.hazardous();
                    });
            boolean hasNonHazardous = group.stream()
                    .anyMatch(p -> {
                        ManifestItemFact fact = manifestItemMap.get(p.manifestItemId());
                        return fact == null || !fact.hazardous();
                    });
            if (hasHazardous && hasNonHazardous) {
                violations.add(new LoadPlanViolation(
                        LoadPlanViolationCode.STACKING_CONFLICT,
                        "Stack group '" + entry.getKey()
                                + "' mixes hazardous and non-hazardous cargo"
                ));
            }
        }
    }

    private void validateCompatibility(List<LoadPlanViolation> violations,
                                        Map<UUID, ManifestItemFact> manifestItemMap) {
        // Temperature-sensitive rule: temperature-sensitive item requires non-blank zoneReference
        for (LoadPlanItemPlacement placement : placements) {
            ManifestItemFact fact = manifestItemMap.get(placement.manifestItemId());
            if (fact != null && Boolean.TRUE.equals(fact.temperatureSensitive())
                    && (placement.zoneReference() == null || placement.zoneReference().isBlank())) {
                violations.add(new LoadPlanViolation(
                        LoadPlanViolationCode.LOAD_PLAN_TEMPERATURE_RULE_FAILED,
                        "Temperature-sensitive item " + placement.manifestItemId()
                                + " requires a designated zone reference"
                ));
            }
        }

        // Group placements by non-blank zone
        Map<String, List<LoadPlanItemPlacement>> zones = placements.stream()
                .filter(p -> p.zoneReference() != null && !p.zoneReference().isBlank())
                .collect(Collectors.groupingBy(LoadPlanItemPlacement::zoneReference));

        for (Map.Entry<String, List<LoadPlanItemPlacement>> entry : zones.entrySet()) {
            List<LoadPlanItemPlacement> zonePlacements = entry.getValue();

            // Hazardous and non-hazardous should not share a zone
            boolean hasHazardous = zonePlacements.stream()
                    .anyMatch(p -> {
                        ManifestItemFact fact = manifestItemMap.get(p.manifestItemId());
                        return fact != null && fact.hazardous();
                    });
            boolean hasNonHazardous = zonePlacements.stream()
                    .anyMatch(p -> {
                        ManifestItemFact fact = manifestItemMap.get(p.manifestItemId());
                        return fact == null || !fact.hazardous();
                    });
            if (hasHazardous && hasNonHazardous) {
                violations.add(new LoadPlanViolation(
                        LoadPlanViolationCode.COMPATIBILITY_CONFLICT,
                        "Zone '" + entry.getKey()
                                + "' contains both hazardous and non-hazardous cargo"
                ));
            }

            // Temperature-sensitive rule: temperature-sensitive and standard cargo must not share a zone
            boolean hasTempSensitive = zonePlacements.stream()
                    .anyMatch(p -> {
                        ManifestItemFact fact = manifestItemMap.get(p.manifestItemId());
                        return fact != null && Boolean.TRUE.equals(fact.temperatureSensitive());
                    });
            boolean hasNonTemp = zonePlacements.stream()
                    .anyMatch(p -> {
                        ManifestItemFact fact = manifestItemMap.get(p.manifestItemId());
                        return fact == null || !Boolean.TRUE.equals(fact.temperatureSensitive());
                    });
            if (hasTempSensitive && hasNonTemp) {
                violations.add(new LoadPlanViolation(
                        LoadPlanViolationCode.LOAD_PLAN_TEMPERATURE_RULE_FAILED,
                        "Zone '" + entry.getKey()
                                + "' mixes temperature-sensitive and standard cargo"
                ));
            }
        }
    }

    private void validateLoadingSequence(List<LoadPlanViolation> violations) {
        Set<Integer> sequences = new HashSet<>();
        for (LoadPlanItemPlacement placement : placements) {
            if (!sequences.add(placement.loadingSequence())) {
                violations.add(new LoadPlanViolation(
                        LoadPlanViolationCode.INVALID_LOADING_SEQUENCE,
                        "Duplicate loading sequence " + placement.loadingSequence()
                                + " for item " + placement.manifestItemId()
                ));
            }
        }
    }
}
