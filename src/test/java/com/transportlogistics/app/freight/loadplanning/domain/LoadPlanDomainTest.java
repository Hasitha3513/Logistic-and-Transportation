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

        List<LoadPlanViolation> violations = plan.validate(Set.of(item1, item2), Set.of());
        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).code()).isEqualTo(LoadPlanViolationCode.ITEM_NOT_PLACED);
        assertThat(violations.get(0).message()).contains(item2.toString());
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

        List<LoadPlanViolation> violations = plan.validate(Set.of(item1), Set.of());
        assertThat(violations).anyMatch(v -> v.code() == LoadPlanViolationCode.DUPLICATE_PLACEMENT);
    }

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

        List<LoadPlanViolation> violations = plan.validate(Set.of(item1, item2), Set.of(item1));
        assertThat(violations).anyMatch(v -> v.code() == LoadPlanViolationCode.STACKING_CONFLICT);
    }

    @Test
    @DisplayName("Validates load plan layout: reports fragile separation required in multi-item stack")
    void shouldReportFragileSeparationRequired() {
        UUID item1 = UUID.randomUUID();
        UUID item2 = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();

        LoadPlan plan = new LoadPlan(
                UUID.randomUUID(),
                "LP-001",
                UUID.randomUUID(),
                UUID.randomUUID(),
                List.of(
                        new LoadPlanItemPlacement(UUID.randomUUID(), item1, 0, "FRONT", "STACK-B", null, 1, "Fragile glassware"),
                        new LoadPlanItemPlacement(UUID.randomUUID(), item2, 1, "FRONT", "STACK-B", null, 2, null)
                ),
                null,
                now,
                now,
                "admin",
                "admin",
                0L
        );

        List<LoadPlanViolation> violations = plan.validate(Set.of(item1, item2), Set.of());
        assertThat(violations).anyMatch(v -> v.code() == LoadPlanViolationCode.FRAGILE_SEPARATION_REQUIRED);
    }

    @Test
    @DisplayName("Validates load plan layout: reports compatibility conflict in same zone")
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

        List<LoadPlanViolation> violations = plan.validate(Set.of(item1, item2), Set.of(item1));
        assertThat(violations).anyMatch(v -> v.code() == LoadPlanViolationCode.COMPATIBILITY_CONFLICT);
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

        List<LoadPlanViolation> violations = plan.validate(Set.of(item1, item2), Set.of(item1));
        assertThat(violations).isEmpty();
    }
}
