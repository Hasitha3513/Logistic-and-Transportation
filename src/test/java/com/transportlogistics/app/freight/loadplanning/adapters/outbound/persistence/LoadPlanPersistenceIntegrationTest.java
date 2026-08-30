package com.transportlogistics.app.freight.loadplanning.adapters.outbound.persistence;

import com.transportlogistics.app.freight.loadplanning.domain.LoadPlan;
import com.transportlogistics.app.freight.loadplanning.domain.LoadPlanViolation;
import com.transportlogistics.app.freight.loadplanning.domain.LoadValidationResult;
import com.transportlogistics.app.freight.loadplanning.domain.ValidationOutcome;
import com.transportlogistics.app.freight.loadplanning.ports.inbound.LoadPlanUseCase;
import com.transportlogistics.app.freight.manifest.ports.inbound.CargoManifestUseCase;
import com.transportlogistics.app.freight.order.ports.inbound.FreightOrderUseCase;
import com.transportlogistics.app.shared.domain.ConflictException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class LoadPlanPersistenceIntegrationTest {

    @Autowired
    private LoadPlanUseCase loadPlanUseCase;

    @Autowired
    private CargoManifestUseCase manifests;

    @Autowired
    private FreightOrderUseCase orders;

    @Autowired
    private JdbcTemplate jdbc;

    private UUID customerId;
    private UUID originId;
    private UUID destinationId;
    private UUID categoryId;
    private UUID typeId;
    private UUID vehicleId;
    private UUID manifestId;
    private UUID manifestItemId;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        originId = UUID.randomUUID();
        destinationId = UUID.randomUUID();
        categoryId = UUID.randomUUID();
        typeId = UUID.randomUUID();
        vehicleId = UUID.randomUUID();

        jdbc.update("INSERT INTO customer (id, code, name, active) VALUES (?, ?, ?, TRUE)",
                customerId, "CUST-" + shortId(customerId), "Integration Customer");
        jdbc.update("INSERT INTO location (id, code, name, active) VALUES (?, ?, ?, TRUE)",
                originId, "LOC-O-" + shortId(originId), "Origin Hub");
        jdbc.update("INSERT INTO location (id, code, name, active) VALUES (?, ?, ?, TRUE)",
                destinationId, "LOC-D-" + shortId(destinationId), "Destination Hub");

        jdbc.update("INSERT INTO vehicle_category (id, code, name, description, active) VALUES (?, ?, ?, ?, TRUE)",
                categoryId, "CAT-" + shortId(categoryId), "Heavy Cargo", "Heavy commercial vehicles");
        jdbc.update("INSERT INTO vehicle_type (id, category_id, code, name, description, active) VALUES (?, ?, ?, ?, ?, TRUE)",
                typeId, categoryId, "TYPE-" + shortId(typeId), "20-Ton Rig", "Heavy 20T truck");

        jdbc.update("INSERT INTO vehicle (id, registration_number, category_id, type_id, manufacturer, model, manufacture_year, ownership_type, operational_status, current_odometer_km, engine_hours, capacity_kg, active) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE)",
                vehicleId, "REG-" + shortId(vehicleId).toUpperCase(), categoryId, typeId, "Volvo", "FH16", 2024, "COMPANY_OWNED", "AVAILABLE", 1000.0, 50.0, 20000.0);

        // Create and finalize a manifest
        var order = orders.create(new FreightOrderUseCase.CreateCommand(
                customerId, originId, destinationId,
                OffsetDateTime.parse("2026-09-01T00:00:00Z"),
                OffsetDateTime.parse("2026-09-02T00:00:00Z"),
                "STANDARD", "NORMAL", null,
                List.of(new FreightOrderUseCase.LineCommand(null, "Heavy Industrial Turbines", BigDecimal.TEN))
        ), "planner");

        var manifest = manifests.create(new CargoManifestUseCase.CreateCommand(order.id()), "planner");
        manifestId = manifest.id();
        var item = new CargoManifestUseCase.ItemCommand(
                manifest.version(),
                order.lines().getFirst().id(),
                "Turbine Components",
                BigDecimal.TEN,
                "Reinforced Crates",
                "INDUSTRIAL.MACHINERY",
                false,
                null,
                false,
                null,
                null,
                false,
                false
        );
        var added = manifests.addItem(manifestId, item, "planner");
        manifestItemId = added.items().getFirst().id();
        manifests.finalizeManifest(manifestId, added.version(), "planner");
    }

    @AfterEach
    void tearDown() {
        jdbc.update("DELETE FROM load_plan_item_placement");
        jdbc.update("DELETE FROM load_plan");
        jdbc.update("DELETE FROM cargo_manifest_item");
        jdbc.update("DELETE FROM cargo_manifest");
        jdbc.update("DELETE FROM freight_insurance_claim");
        jdbc.update("DELETE FROM freight_insurance_policy");
        jdbc.update("DELETE FROM freight_order_line");
        jdbc.update("DELETE FROM freight_order");
        jdbc.update("DELETE FROM vehicle WHERE id = ?", vehicleId);
        jdbc.update("DELETE FROM vehicle_type WHERE id = ?", typeId);
        jdbc.update("DELETE FROM vehicle_category WHERE id = ?", categoryId);
        jdbc.update("DELETE FROM location WHERE id IN (?, ?)", originId, destinationId);
        jdbc.update("DELETE FROM customer WHERE id = ?", customerId);
    }

    @Test
    @DisplayName("Round-trip persistence, layout validation, weight-volume validation, and optimistic concurrency")
    void testLoadPlanRoundTripAndValidations() {
        var placement = new LoadPlanUseCase.ItemPlacementCommand(
                manifestItemId, 0, "BAY-FRONT", "STACK-1", "PALLET-101", 1, "Handle with care"
        );
        var createCmd = new LoadPlanUseCase.CreateCommand(
                manifestId, vehicleId, List.of(placement), "Initial loading plan"
        );

        LoadPlan saved = loadPlanUseCase.create(createCmd, "planner");
        assertThat(saved.getLoadPlanId()).isNotNull();
        assertThat(saved.getLoadPlanNumber()).startsWith("LP-");
        assertThat(saved.getPlacements()).hasSize(1);
        assertThat(saved.getVersion()).isEqualTo(0L);

        // Retrieve by ID
        LoadPlan fetched = loadPlanUseCase.get(saved.getLoadPlanId());
        assertThat(fetched.getLoadPlanNumber()).isEqualTo(saved.getLoadPlanNumber());
        assertThat(fetched.getPlacements().getFirst().zoneReference()).isEqualTo("BAY-FRONT");

        // Validate layout
        List<LoadPlanViolation> layoutViolations = loadPlanUseCase.validateLayout(saved.getLoadPlanId());
        assertThat(layoutViolations).isEmpty();

        // Validate weight and volume
        LoadValidationResult wvResult = loadPlanUseCase.validateWeightAndVolume(saved.getLoadPlanId(), "planner");
        assertThat(wvResult.overallOutcome()).isEqualTo(ValidationOutcome.INCOMPLETE);
        assertThat(wvResult.missingData()).isNotEmpty();

        // Update load plan
        var updatedPlacement = new LoadPlanUseCase.ItemPlacementCommand(
                manifestItemId, 0, "BAY-REAR", "STACK-2", "PALLET-102", 1, null
        );
        var updateCmd = new LoadPlanUseCase.UpdateCommand(
                vehicleId, List.of(updatedPlacement), "Updated to rear bay", saved.getVersion()
        );
        LoadPlan updated = loadPlanUseCase.update(saved.getLoadPlanId(), updateCmd, "planner");
        assertThat(updated.getPlacements().getFirst().zoneReference()).isEqualTo("BAY-REAR");
        assertThat(updated.getVersion()).isEqualTo(1L);

        // Stale update rejection
        var staleCmd = new LoadPlanUseCase.UpdateCommand(
                vehicleId, List.of(updatedPlacement), "Stale retry", 0L
        );
        assertThatThrownBy(() -> loadPlanUseCase.update(saved.getLoadPlanId(), staleCmd, "planner"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("changed by another user");
    }

    @Test
    @DisplayName("Persists readiness lifecycle: DRAFT -> STRUCTURALLY_READY -> DRAFT on material edit, preserves on notes edit")
    void testReadinessLifecyclePersistence() {
        var placement = new LoadPlanUseCase.ItemPlacementCommand(
                manifestItemId, 0, "BAY-FRONT", "STACK-1", "PALLET-101", 1, null
        );
        var createCmd = new LoadPlanUseCase.CreateCommand(
                manifestId, vehicleId, List.of(placement), "Ready test plan"
        );

        LoadPlan saved = loadPlanUseCase.create(createCmd, "planner");
        assertThat(saved.getReadinessStatus()).isEqualTo(com.transportlogistics.app.freight.loadplanning.domain.LoadPlanReadinessStatus.DRAFT);
        assertThat(saved.getReadyAt()).isNull();
        assertThat(saved.getReadyBy()).isNull();

        // Verify DB raw columns for DRAFT
        var dbRow = jdbc.queryForMap("SELECT readiness_status, ready_at, ready_by, version FROM load_plan WHERE id = ?", saved.getLoadPlanId());
        assertThat(dbRow.get("readiness_status")).isEqualTo("DRAFT");
        assertThat(dbRow.get("ready_at")).isNull();
        assertThat(dbRow.get("ready_by")).isNull();

        // Mark structurally ready
        LoadPlan ready = loadPlanUseCase.markReady(saved.getLoadPlanId(), saved.getVersion(), "lead-planner");
        assertThat(ready.getReadinessStatus()).isEqualTo(com.transportlogistics.app.freight.loadplanning.domain.LoadPlanReadinessStatus.STRUCTURALLY_READY);
        assertThat(ready.getReadyBy()).isEqualTo("lead-planner");
        assertThat(ready.getReadyAt()).isNotNull();

        // Verify DB raw columns for STRUCTURALLY_READY
        var dbReadyRow = jdbc.queryForMap("SELECT readiness_status, ready_at, ready_by, version FROM load_plan WHERE id = ?", saved.getLoadPlanId());
        assertThat(dbReadyRow.get("readiness_status")).isEqualTo("STRUCTURALLY_READY");
        assertThat(dbReadyRow.get("ready_at")).isNotNull();
        assertThat(dbReadyRow.get("ready_by")).isEqualTo("lead-planner");

        // Notes-only update preserves STRUCTURALLY_READY
        var notesOnlyCmd = new LoadPlanUseCase.UpdateCommand(
                vehicleId, List.of(placement), "Notes updated only", ready.getVersion()
        );
        LoadPlan notesUpdated = loadPlanUseCase.update(saved.getLoadPlanId(), notesOnlyCmd, "planner2");
        assertThat(notesUpdated.getReadinessStatus()).isEqualTo(com.transportlogistics.app.freight.loadplanning.domain.LoadPlanReadinessStatus.STRUCTURALLY_READY);
        assertThat(notesUpdated.getReadyBy()).isEqualTo("lead-planner");
        assertThat(notesUpdated.getReadyAt()).isNotNull();

        // Material update (placement order change) invalidates to DRAFT
        var materialPlacement = new LoadPlanUseCase.ItemPlacementCommand(
                manifestItemId, 0, "BAY-REAR", "STACK-1", "PALLET-101", 1, null
        );
        var materialCmd = new LoadPlanUseCase.UpdateCommand(
                vehicleId, List.of(materialPlacement), "Zone changed to rear", notesUpdated.getVersion()
        );
        LoadPlan materialUpdated = loadPlanUseCase.update(saved.getLoadPlanId(), materialCmd, "planner3");
        assertThat(materialUpdated.getReadinessStatus()).isEqualTo(com.transportlogistics.app.freight.loadplanning.domain.LoadPlanReadinessStatus.DRAFT);
        assertThat(materialUpdated.getReadyAt()).isNull();
        assertThat(materialUpdated.getReadyBy()).isNull();

        var dbMaterialRow = jdbc.queryForMap("SELECT readiness_status, ready_at, ready_by FROM load_plan WHERE id = ?", saved.getLoadPlanId());
        assertThat(dbMaterialRow.get("readiness_status")).isEqualTo("DRAFT");
        assertThat(dbMaterialRow.get("ready_at")).isNull();
        assertThat(dbMaterialRow.get("ready_by")).isNull();
    }

    @Test
    @DisplayName("Database check constraints enforce status enum and audit nullability invariants")
    void testDatabaseCheckConstraints() {
        UUID validId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();

        // 1. Invalid status value
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> {
            jdbc.update(
                    "INSERT INTO load_plan (id, load_plan_number, cargo_manifest_id, vehicle_id, readiness_status, version, created_at, updated_at, created_by, updated_by) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    validId, "LP-CHK-1", manifestId, vehicleId, "UNKNOWN_STATUS", 0L, now, now, "admin", "admin"
            );
        }).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);

        // 2. DRAFT with non-null ready_at
        UUID draftWithAuditId = UUID.randomUUID();
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> {
            jdbc.update(
                    "INSERT INTO load_plan (id, load_plan_number, cargo_manifest_id, vehicle_id, readiness_status, ready_at, ready_by, version, created_at, updated_at, created_by, updated_by) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    draftWithAuditId, "LP-CHK-2", manifestId, vehicleId, "DRAFT", now, "admin", 0L, now, now, "admin", "admin"
            );
        }).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);

        // 3. STRUCTURALLY_READY with null ready_at/ready_by
        UUID readyWithoutAuditId = UUID.randomUUID();
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> {
            jdbc.update(
                    "INSERT INTO load_plan (id, load_plan_number, cargo_manifest_id, vehicle_id, readiness_status, ready_at, ready_by, version, created_at, updated_at, created_by, updated_by) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    readyWithoutAuditId, "LP-CHK-3", manifestId, vehicleId, "STRUCTURALLY_READY", null, null, 0L, now, now, "admin", "admin"
            );
        }).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Migration defaults: rows inserted without explicit readiness default to DRAFT and null audit")
    void testMigrationDefaults() {
        UUID id = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();

        jdbc.update(
                "INSERT INTO load_plan (id, load_plan_number, cargo_manifest_id, vehicle_id, version, created_at, updated_at, created_by, updated_by) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                id, "LP-MIG-1", manifestId, vehicleId, 0L, now, now, "admin", "admin"
        );

        var row = jdbc.queryForMap("SELECT readiness_status, ready_at, ready_by FROM load_plan WHERE id = ?", id);
        assertThat(row.get("readiness_status")).isEqualTo("DRAFT");
        assertThat(row.get("ready_at")).isNull();
        assertThat(row.get("ready_by")).isNull();
    }

    private String shortId(UUID id) {
        return id.toString().substring(0, 8);
    }
}
