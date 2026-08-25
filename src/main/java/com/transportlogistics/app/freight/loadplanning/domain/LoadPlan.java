package com.transportlogistics.app.freight.loadplanning.domain;

import java.time.OffsetDateTime;
import java.util.ArrayList;
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
 * <p>Structural validation is performed via {@link #validate(List)} which returns
 * planning-level violations. Authoritative weight/capacity validation belongs to US-27.</p>
 */
public final class LoadPlan {

    private final UUID loadPlanId;
    private final String loadPlanNumber;
    private final UUID cargoManifestId;
    private final UUID vehicleId;
    private final List<LoadPlanItemPlacement> placements;
    private final String notes;
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
        this.loadPlanId = loadPlanId;
        this.loadPlanNumber = loadPlanNumber;
        this.cargoManifestId = cargoManifestId;
        this.vehicleId = vehicleId;
        this.placements = placements == null ? List.of() : List.copyOf(placements);
        this.notes = notes;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
        this.version = version;
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
     */
    public LoadPlan update(UUID vehicleId,
                           List<LoadPlanItemPlacement> newPlacements,
                           String notes,
                           String actor,
                           OffsetDateTime now) {
        return new LoadPlan(
                this.loadPlanId,
                this.loadPlanNumber,
                this.cargoManifestId,
                vehicleId,
                newPlacements,
                notes,
                this.createdAt,
                now,
                this.createdBy,
                actor,
                this.version
        );
    }

    // ──────────────────────────────────────────────────────────
    // Structural validation
    // ──────────────────────────────────────────────────────────

    /**
     * Validates the load plan against the set of manifest item IDs that should
     * be placed, and checks structural placement rules.
     *
     * @param manifestItemIds the complete set of manifest item IDs from the finalized manifest
     * @param hazardousItemIds set of manifest item IDs flagged as hazardous
     * @return list of violations; empty means structurally valid
     */
    public List<LoadPlanViolation> validate(Set<UUID> manifestItemIds,
                                            Set<UUID> hazardousItemIds) {
        List<LoadPlanViolation> violations = new ArrayList<>();

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

        // Check all manifest items are placed
        for (UUID itemId : manifestItemIds) {
            if (!placedItemIds.contains(itemId)) {
                violations.add(new LoadPlanViolation(
                        LoadPlanViolationCode.ITEM_NOT_PLACED,
                        "Manifest item " + itemId + " has not been placed"
                ));
            }
        }

        // Check stacking conflicts — items in the same stack group should be compatible
        validateStackingRules(violations, hazardousItemIds);

        // Check compatibility — hazardous items should not share zones with non-hazardous unless explicitly noted
        validateCompatibility(violations, hazardousItemIds);

        // Check loading sequence validity
        validateLoadingSequence(violations);

        return Collections.unmodifiableList(violations);
    }

    private void validateStackingRules(List<LoadPlanViolation> violations,
                                       Set<UUID> hazardousItemIds) {
        // Group placements by stack group
        Map<String, List<LoadPlanItemPlacement>> stackGroups = placements.stream()
                .filter(p -> p.stackGroup() != null && !p.stackGroup().isBlank())
                .collect(Collectors.groupingBy(LoadPlanItemPlacement::stackGroup));

        for (Map.Entry<String, List<LoadPlanItemPlacement>> entry : stackGroups.entrySet()) {
            List<LoadPlanItemPlacement> group = entry.getValue();
            // Items requiring special handling (fragile) should not be stacked with other items
            for (LoadPlanItemPlacement placement : group) {
                if (hasSpecialHandling(placement, "FRAGILE") && group.size() > 1) {
                    violations.add(new LoadPlanViolation(
                            LoadPlanViolationCode.FRAGILE_SEPARATION_REQUIRED,
                            "Fragile item " + placement.manifestItemId()
                                    + " in stack group '" + entry.getKey() + "' requires separation"
                    ));
                }
            }
            // Hazardous items should not be stacked with non-hazardous
            boolean hasHazardous = group.stream()
                    .anyMatch(p -> hazardousItemIds.contains(p.manifestItemId()));
            boolean hasNonHazardous = group.stream()
                    .anyMatch(p -> !hazardousItemIds.contains(p.manifestItemId()));
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
                                        Set<UUID> hazardousItemIds) {
        // Group placements by zone
        Map<String, List<LoadPlanItemPlacement>> zones = placements.stream()
                .filter(p -> p.zoneReference() != null && !p.zoneReference().isBlank())
                .collect(Collectors.groupingBy(LoadPlanItemPlacement::zoneReference));

        for (Map.Entry<String, List<LoadPlanItemPlacement>> entry : zones.entrySet()) {
            List<LoadPlanItemPlacement> zonePlacements = entry.getValue();
            // Hazardous and non-hazardous should not share a zone
            boolean hasHazardous = zonePlacements.stream()
                    .anyMatch(p -> hazardousItemIds.contains(p.manifestItemId()));
            boolean hasNonHazardous = zonePlacements.stream()
                    .anyMatch(p -> !hazardousItemIds.contains(p.manifestItemId()));
            if (hasHazardous && hasNonHazardous) {
                violations.add(new LoadPlanViolation(
                        LoadPlanViolationCode.COMPATIBILITY_CONFLICT,
                        "Zone '" + entry.getKey()
                                + "' contains both hazardous and non-hazardous cargo"
                ));
            }

            // Temperature-sensitive items should not share zones with items lacking temp handling
            boolean hasTempSensitive = zonePlacements.stream()
                    .anyMatch(p -> hasSpecialHandling(p, "TEMPERATURE"));
            boolean hasNonTemp = zonePlacements.stream()
                    .anyMatch(p -> !hasSpecialHandling(p, "TEMPERATURE"));
            if (hasTempSensitive && hasNonTemp) {
                violations.add(new LoadPlanViolation(
                        LoadPlanViolationCode.TEMPERATURE_SEPARATION_REQUIRED,
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

    /**
     * Checks if a placement's special handling notes contain a keyword (case-insensitive).
     */
    private static boolean hasSpecialHandling(LoadPlanItemPlacement placement, String keyword) {
        return placement.specialHandlingNotes() != null
                && placement.specialHandlingNotes().toUpperCase().contains(keyword.toUpperCase());
    }
}
