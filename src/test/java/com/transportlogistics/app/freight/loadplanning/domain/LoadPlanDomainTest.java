package com.transportlogistics.app.freight.loadplanning.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoadPlanDomainTest {

    @Test
    @DisplayName("Creates valid LoadPlan and accessors work properly")
    void shouldCreateValidLoadPlan() {
        UUID id = UUID.randomUUID();
        UUID manifestId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();

        LoadPlan plan = new LoadPlan(
                id,
                "LP-2026-000001",
                manifestId,
                vehicleId,
                List.of(),
                "Notes",
                now,
                now,
                "admin",
                "admin",
                0L
        );

        assertThat(plan.getLoadPlanId()).isEqualTo(id);
        assertThat(plan.getLoadPlanNumber()).isEqualTo("LP-2026-000001");
        assertThat(plan.getCargoManifestId()).isEqualTo(manifestId);
        assertThat(plan.getVehicleId()).isEqualTo(vehicleId);
        assertThat(plan.getPlacements()).isEmpty();
        assertThat(plan.getVersion()).isEqualTo(0L);
    }

    @Test
    @DisplayName("Rejects creation with invalid arguments")
    void shouldRejectInvalidCreation() {
        OffsetDateTime now = OffsetDateTime.now();
        UUID validId = UUID.randomUUID();

        assertThatThrownBy(() -> new LoadPlan(null, "LP-001", validId, validId, List.of(), null, now, now, "a", "a", 0L))
                .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> new LoadPlan(validId, "", validId, validId, List.of(), null, now, now, "a", "a", 0L))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new LoadPlan(validId, "LP-001", null, validId, List.of(), null, now, now, "a", "a", 0L))
                .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> new LoadPlan(validId, "LP-001", validId, null, List.of(), null, now, now, "a", "a", 0L))
                .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> new LoadPlan(validId, "LP-001", validId, validId, List.of(), null, now, now, "a", "a", -1L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Validates load plan layout: reports unplaced items")
    void shouldReportUnplacedItems() {
        UUID item1 = UUID.randomUUID();
        UUID item2 = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();

        LoadPlan plan = new LoadPlan(
                UUID.randomUUID(),
                "LP-001",
                UUID.randomUUID(),
                UUID.randomUUID(),
                List.of(new LoadPlanItemPlacement(UUID.randomUUID(), item1, 0, "FRONT", null, null, 1, null)),
                null,
                now,
                now,
                "admin",
                "admin",
                0L
        );

        List<ManifestItemFact> facts = List.of(
                new ManifestItemFact(item1, false, false, false),
                new ManifestItemFact(item2, false, false, false)
        );

        List<LoadPlanViolation> violations = plan.validate(facts);
        assertThat(violations).hasSize(1);
        assertThat(violations.getFirst().code()).isEqualTo(LoadPlanViolationCode.ITEM_NOT_PLACED);
        assertThat(violations.getFirst().message()).contains(item2.toString());
    }

    @Test
    @DisplayName("Validates load plan layout: reports duplicate placement")
    void shouldReportDuplicatePlacement() {
        UUID item1 = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();

        LoadPlan plan = new LoadPlan(
                UUID.randomUUID(),
                "LP-001",
                UUID.randomUUID(),
                UUID.randomUUID(),
                List.of(
                        new LoadPlanItemPlacement(UUID.randomUUID(), item1, 0, "FRONT", null, null, 1, null),
                        new LoadPlanItemPlacement(UUID.randomUUID(), item1, 1, "REAR", null, null, 2, null)
                ),
                null,
                now,
                now,
                "admin",
                "admin",
                0L
        );

        List<ManifestItemFact> facts = List.of(
                new ManifestItemFact(item1, false, false, false)
        );

        List<LoadPlanViolation> violations = plan.validate(facts);
        assertThat(violations).anyMatch(v -> v.code() == LoadPlanViolationCode.DUPLICATE_PLACEMENT);
    }

    @Test
    @DisplayName("Validates load plan layout: reports unknown special cargo classification")
    void shouldReportUnknownSpecialCargoClassification() {
        UUID item1 = UUID.randomUUID();
        UUID item2 = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();

        LoadPlan plan = new LoadPlan(
                UUID.randomUUID(),
                "LP-001",
                UUID.randomUUID(),
                UUID.randomUUID(),
                List.of(
                        new LoadPlanItemPlacement(UUID.randomUUID(), item1, 0, "FRONT", null, null, 1, null),
                        new LoadPlanItemPlacement(UUID.randomUUID(), item2, 1, "REAR", null, null, 2, null)
                ),
                null,
                now,
                now,
                "admin",
                "admin",
                0L
        );

        List<ManifestItemFact> facts = List.of(
                new ManifestItemFact(item1, false, null, false),
                new ManifestItemFact(item2, false, false, null)
        );

        List<LoadPlanViolation> violations = plan.validate(facts);
        assertThat(violations)
                .filteredOn(v -> v.code() == LoadPlanViolationCode.LOAD_PLAN_SPECIAL_CARGO_CLASSIFICATION_MISSING)
                .hasSize(2);
    }

    @Test
    @DisplayName("Validates load plan layout: fragile item in shared stack group fails fragile rule")
    void shouldReportFragileRuleFailedOnSharedStackGroup() {
        UUID item1 = UUID.randomUUID(); // fragile
        UUID item2 = UUID.randomUUID(); // standard
        OffsetDateTime now = OffsetDateTime.now();

        LoadPlan plan = new LoadPlan(
                UUID.randomUUID(),
                "LP-001",
                UUID.randomUUID(),
                UUID.randomUUID(),
                List.of(
                        new LoadPlanItemPlacement(UUID.randomUUID(), item1, 0, "FRONT", "STACK-B", null, 1, null),
                        new LoadPlanItemPlacement(UUID.randomUUID(), item2, 1, "FRONT", "STACK-B", null, 2, null)
                ),
                null,
                now,
                now,
                "admin",
                "admin",
                0L
        );

        List<ManifestItemFact> facts = List.of(
                new ManifestItemFact(item1, false, true, false),
                new ManifestItemFact(item2, false, false, false)
        );

        List<LoadPlanViolation> violations = plan.validate(facts);
        assertThat(violations).anyMatch(v -> v.code() == LoadPlanViolationCode.LOAD_PLAN_FRAGILE_RULE_FAILED);
    }

    @Test
    @DisplayName("Validates load plan layout: fragile item with unique or no stack group passes fragile rule")
    void shouldPassFragileWithUniqueOrNoStackGroup() {
        UUID item1 = UUID.randomUUID(); // fragile, no stackGroup
        UUID item2 = UUID.randomUUID(); // fragile, unique stackGroup
        UUID item3 = UUID.randomUUID(); // standard
        UUID item4 = UUID.randomUUID(); // standard, sharing stackGroup with item3
        OffsetDateTime now = OffsetDateTime.now();

        LoadPlan plan = new LoadPlan(
                UUID.randomUUID(),
                "LP-001",
                UUID.randomUUID(),
                UUID.randomUUID(),
                List.of(
                        new LoadPlanItemPlacement(UUID.randomUUID(), item1, 0, "FRONT", null, null, 1, null),
                        new LoadPlanItemPlacement(UUID.randomUUID(), item2, 1, "FRONT", "STACK-FRAGILE-SOLO", null, 2, null),
                        new LoadPlanItemPlacement(UUID.randomUUID(), item3, 2, "REAR", "STACK-SHARED-STD", null, 3, null),
                        new LoadPlanItemPlacement(UUID.randomUUID(), item4, 3, "REAR", "STACK-SHARED-STD", null, 4, null)
                ),
                null,
                now,
                now,
                "admin",
                "admin",
                0L
        );

        List<ManifestItemFact> facts = List.of(
                new ManifestItemFact(item1, false, true, false),
                new ManifestItemFact(item2, false, true, false),
                new ManifestItemFact(item3, false, false, false),
                new ManifestItemFact(item4, false, false, false)
        );

        List<LoadPlanViolation> violations = plan.validate(facts);
        assertThat(violations).noneMatch(v -> v.code() == LoadPlanViolationCode.LOAD_PLAN_FRAGILE_RULE_FAILED);
    }

    @Test
    @DisplayName("Validates load plan layout: temperature-sensitive item requires non-blank zoneReference")
    void shouldReportTemperatureRuleFailedWhenMissingZoneReference() {
        UUID item1 = UUID.randomUUID(); // temperature-sensitive
        OffsetDateTime now = OffsetDateTime.now();

        LoadPlan plan = new LoadPlan(
                UUID.randomUUID(),
                "LP-001",
                UUID.randomUUID(),
                UUID.randomUUID(),
                List.of(
                        new LoadPlanItemPlacement(UUID.randomUUID(), item1, 0, null, null, null, 1, null)
                ),
                null,
                now,
                now,
                "admin",
                "admin",
                0L
        );

        List<ManifestItemFact> facts = List.of(
                new ManifestItemFact(item1, false, false, true)
        );

        List<LoadPlanViolation> violations = plan.validate(facts);
        assertThat(violations).anyMatch(v -> v.code() == LoadPlanViolationCode.LOAD_PLAN_TEMPERATURE_RULE_FAILED);
    }

    @Test
    @DisplayName("Validates load plan layout: temperature-sensitive and standard cargo sharing zone fails temperature rule")
    void shouldReportTemperatureRuleFailedOnMixedZone() {
        UUID item1 = UUID.randomUUID(); // temperature-sensitive
        UUID item2 = UUID.randomUUID(); // standard cargo
        OffsetDateTime now = OffsetDateTime.now();

        LoadPlan plan = new LoadPlan(
                UUID.randomUUID(),
                "LP-001",
                UUID.randomUUID(),
                UUID.randomUUID(),
                List.of(
                        new LoadPlanItemPlacement(UUID.randomUUID(), item1, 0, "REEFER-ZONE", null, null, 1, null),
                        new LoadPlanItemPlacement(UUID.randomUUID(), item2, 1, "REEFER-ZONE", null, null, 2, null)
                ),
                null,
                now,
                now,
                "admin",
                "admin",
                0L
        );

        List<ManifestItemFact> facts = List.of(
                new ManifestItemFact(item1, false, false, true),
                new ManifestItemFact(item2, false, false, false)
        );

        List<LoadPlanViolation> violations = plan.validate(facts);
        assertThat(violations).anyMatch(v -> v.code() == LoadPlanViolationCode.LOAD_PLAN_TEMPERATURE_RULE_FAILED);
    }

    @Test
    @DisplayName("Validates load plan layout: dedicated temperature-sensitive zone passes")
    void shouldPassDedicatedTemperatureZone() {
        UUID item1 = UUID.randomUUID(); // temperature-sensitive
        UUID item2 = UUID.randomUUID(); // temperature-sensitive
        UUID item3 = UUID.randomUUID(); // standard
        OffsetDateTime now = OffsetDateTime.now();

        LoadPlan plan = new LoadPlan(
                UUID.randomUUID(),
                "LP-001",
                UUID.randomUUID(),
                UUID.randomUUID(),
                List.of(
                        new LoadPlanItemPlacement(UUID.randomUUID(), item1, 0, "REEFER-ZONE", null, null, 1, null),
                        new LoadPlanItemPlacement(UUID.randomUUID(), item2, 1, "REEFER-ZONE", null, null, 2, null),
                        new LoadPlanItemPlacement(UUID.randomUUID(), item3, 2, "AMBIENT-ZONE", null, null, 3, null)
                ),
                null,
                now,
                now,
                "admin",
                "admin",
                0L
        );

        List<ManifestItemFact> facts = List.of(
                new ManifestItemFact(item1, false, false, true),
                new ManifestItemFact(item2, false, false, true),
                new ManifestItemFact(item3, false, false, false)
        );

        List<LoadPlanViolation> violations = plan.validate(facts);
        assertThat(violations).isEmpty();
    }

    // ──────────────────────────────────────────────────────────
    // Mandatory Free-Text Regression Tests (Section 18)
    // ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("Regression Case A: fragile=false with notes='FRAGILE' is NOT fragile")
    void shouldNotTreatNotesAsFragileWhenStructuredFalse() {
        UUID item1 = UUID.randomUUID();
        UUID item2 = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();

        LoadPlan plan = new LoadPlan(
                UUID.randomUUID(),
                "LP-001",
                UUID.randomUUID(),
                UUID.randomUUID(),
                List.of(
                        new LoadPlanItemPlacement(UUID.randomUUID(), item1, 0, "FRONT", "STACK-1", null, 1, "FRAGILE GLASSWARE"),
                        new LoadPlanItemPlacement(UUID.randomUUID(), item2, 1, "FRONT", "STACK-1", null, 2, null)
                ),
                null,
                now,
                now,
                "admin",
                "admin",
                0L
        );

        List<ManifestItemFact> facts = List.of(
                new ManifestItemFact(item1, false, false, false),
                new ManifestItemFact(item2, false, false, false)
        );

        List<LoadPlanViolation> violations = plan.validate(facts);
        assertThat(violations).noneMatch(v -> v.code() == LoadPlanViolationCode.LOAD_PLAN_FRAGILE_RULE_FAILED);
    }

    @Test
    @DisplayName("Regression Case B: fragile=true with empty notes enforces fragile rule")
    void shouldEnforceFragileRuleWhenNotesEmpty() {
        UUID item1 = UUID.randomUUID();
        UUID item2 = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();

        LoadPlan plan = new LoadPlan(
                UUID.randomUUID(),
                "LP-001",
                UUID.randomUUID(),
                UUID.randomUUID(),
                List.of(
                        new LoadPlanItemPlacement(UUID.randomUUID(), item1, 0, "FRONT", "STACK-1", null, 1, ""),
                        new LoadPlanItemPlacement(UUID.randomUUID(), item2, 1, "FRONT", "STACK-1", null, 2, null)
                ),
                null,
                now,
                now,
                "admin",
                "admin",
                0L
        );

        List<ManifestItemFact> facts = List.of(
                new ManifestItemFact(item1, false, true, false),
                new ManifestItemFact(item2, false, false, false)
        );

        List<LoadPlanViolation> violations = plan.validate(facts);
        assertThat(violations).anyMatch(v -> v.code() == LoadPlanViolationCode.LOAD_PLAN_FRAGILE_RULE_FAILED);
    }

    @Test
    @DisplayName("Regression Case C: temperatureSensitive=false with notes='TEMPERATURE CONTROLLED' is NOT temperature-sensitive")
    void shouldNotTreatNotesAsTemperatureWhenStructuredFalse() {
        UUID item1 = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();

        // No zoneReference provided
        LoadPlan plan = new LoadPlan(
                UUID.randomUUID(),
                "LP-001",
                UUID.randomUUID(),
                UUID.randomUUID(),
                List.of(
                        new LoadPlanItemPlacement(UUID.randomUUID(), item1, 0, null, null, null, 1, "TEMPERATURE CONTROLLED REQUIRED")
                ),
                null,
                now,
                now,
                "admin",
                "admin",
                0L
        );

        List<ManifestItemFact> facts = List.of(
                new ManifestItemFact(item1, false, false, false)
        );

        List<LoadPlanViolation> violations = plan.validate(facts);
        assertThat(violations).noneMatch(v -> v.code() == LoadPlanViolationCode.LOAD_PLAN_TEMPERATURE_RULE_FAILED);
    }

    @Test
    @DisplayName("Regression Case D: temperatureSensitive=true with empty notes enforces temperature rule")
    void shouldEnforceTemperatureRuleWhenNotesEmpty() {
        UUID item1 = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();

        // No zoneReference provided
        LoadPlan plan = new LoadPlan(
                UUID.randomUUID(),
                "LP-001",
                UUID.randomUUID(),
                UUID.randomUUID(),
                List.of(
                        new LoadPlanItemPlacement(UUID.randomUUID(), item1, 0, null, null, null, 1, "")
                ),
                null,
                now,
                now,
                "admin",
                "admin",
                0L
        );

        List<ManifestItemFact> facts = List.of(
                new ManifestItemFact(item1, false, false, true)
        );

        List<LoadPlanViolation> violations = plan.validate(facts);
        assertThat(violations).anyMatch(v -> v.code() == LoadPlanViolationCode.LOAD_PLAN_TEMPERATURE_RULE_FAILED);
    }

    // ──────────────────────────────────────────────────────────
    // Hazardous Stacking and Zone Compatibility Tests
    // ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("Validates load plan layout: reports stacking conflict when mixing hazardous and non-hazardous")
    void shouldReportStackingConflict() {
        UUID item1 = UUID.randomUUID(); // hazardous
        UUID item2 = UUID.randomUUID(); // non-hazardous
        OffsetDateTime now = OffsetDateTime.now();

        LoadPlan plan = new LoadPlan(
                UUID.randomUUID(),
                "LP-001",
                UUID.randomUUID(),
                UUID.randomUUID(),
                List.of(
                        new LoadPlanItemPlacement(UUID.randomUUID(), item1, 0, "FRONT", "STACK-A", null, 1, null),
                        new LoadPlanItemPlacement(UUID.randomUUID(), item2, 1, "FRONT", "STACK-A", null, 2, null)
                ),
                null,
                now,
                now,
                "admin",
                "admin",
                0L
        );

        List<ManifestItemFact> facts = List.of(
                new ManifestItemFact(item1, true, false, false),
                new ManifestItemFact(item2, false, false, false)
        );

        List<LoadPlanViolation> violations = plan.validate(facts);
        assertThat(violations).anyMatch(v -> v.code() == LoadPlanViolationCode.STACKING_CONFLICT);
    }

    @Test
    @DisplayName("Validates load plan layout: reports compatibility conflict in same zone for hazardous")
    void shouldReportZoneCompatibilityConflict() {
        UUID item1 = UUID.randomUUID(); // hazardous
        UUID item2 = UUID.randomUUID(); // non-hazardous
        OffsetDateTime now = OffsetDateTime.now();

        LoadPlan plan = new LoadPlan(
                UUID.randomUUID(),
                "LP-001",
                UUID.randomUUID(),
                UUID.randomUUID(),
                List.of(
                        new LoadPlanItemPlacement(UUID.randomUUID(), item1, 0, "ZONE-1", null, null, 1, null),
                        new LoadPlanItemPlacement(UUID.randomUUID(), item2, 1, "ZONE-1", null, null, 2, null)
                ),
                null,
                now,
                now,
                "admin",
                "admin",
                0L
        );

        List<ManifestItemFact> facts = List.of(
                new ManifestItemFact(item1, true, false, false),
                new ManifestItemFact(item2, false, false, false)
        );

        List<LoadPlanViolation> violations = plan.validate(facts);
        assertThat(violations).anyMatch(v -> v.code() == LoadPlanViolationCode.COMPATIBILITY_CONFLICT);
    }

    @Test
    @DisplayName("Validates load plan layout: reports duplicate loading sequence")
    void shouldReportDuplicateLoadingSequence() {
        UUID item1 = UUID.randomUUID();
        UUID item2 = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();

        LoadPlan plan = new LoadPlan(
                UUID.randomUUID(),
                "LP-001",
                UUID.randomUUID(),
                UUID.randomUUID(),
                List.of(
                        new LoadPlanItemPlacement(UUID.randomUUID(), item1, 0, "ZONE-1", null, null, 1, null),
                        new LoadPlanItemPlacement(UUID.randomUUID(), item2, 1, "ZONE-2", null, null, 1, null)
                ),
                null,
                now,
                now,
                "admin",
                "admin",
                0L
        );

        List<ManifestItemFact> facts = List.of(
                new ManifestItemFact(item1, false, false, false),
                new ManifestItemFact(item2, false, false, false)
        );

        List<LoadPlanViolation> violations = plan.validate(facts);
        assertThat(violations).anyMatch(v -> v.code() == LoadPlanViolationCode.INVALID_LOADING_SEQUENCE);
    }

    @Test
    @DisplayName("Validates load plan layout: clean plan returns empty violations")
    void shouldPassCleanLayout() {
        UUID item1 = UUID.randomUUID();
        UUID item2 = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();

        LoadPlan plan = new LoadPlan(
                UUID.randomUUID(),
                "LP-001",
                UUID.randomUUID(),
                UUID.randomUUID(),
                List.of(
                        new LoadPlanItemPlacement(UUID.randomUUID(), item1, 0, "ZONE-FRONT", "STACK-1", "PALLET-1", 1, null),
                        new LoadPlanItemPlacement(UUID.randomUUID(), item2, 1, "ZONE-REAR", "STACK-2", "PALLET-2", 2, null)
                ),
                null,
                now,
                now,
                "admin",
                "admin",
                0L
        );

        List<ManifestItemFact> facts = List.of(
                new ManifestItemFact(item1, true, false, false),
                new ManifestItemFact(item2, false, false, false)
        );

        List<LoadPlanViolation> violations = plan.validate(facts);
        assertThat(violations).isEmpty();
    }
}
