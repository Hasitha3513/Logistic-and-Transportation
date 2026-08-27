package com.transportlogistics.app.freight.loadplanning.application;

import com.transportlogistics.app.freight.loadplanning.domain.LoadPlan;
import com.transportlogistics.app.freight.loadplanning.domain.LoadPlanItemPlacement;
import com.transportlogistics.app.freight.loadplanning.domain.LoadPlanViolation;
import com.transportlogistics.app.freight.loadplanning.domain.LoadPlanViolationCode;
import com.transportlogistics.app.freight.loadplanning.domain.LoadValidationResult;
import com.transportlogistics.app.freight.loadplanning.domain.ValidationOutcome;
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
import com.transportlogistics.app.shared.domain.ConflictException;
import com.transportlogistics.app.shared.domain.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoadPlanServiceTest {

    @Mock
    private LoadPlanRepository repository;

    @Mock
    private LoadPlanNumberGenerator numberGenerator;

    @Mock
    private CargoManifestLookupPort manifestLookup;

    @Mock
    private VehicleLoadSpaceLookupPort vehicleLookup;

    @Mock
    private LoadPlanEventPublisher eventPublisher;

    private final LoadPlanTransaction transaction = new LoadPlanTransaction() {
        @Override
        public <T> T execute(Supplier<T> operation) {
            return operation.get();
        }
    };

    private final Clock clock = Clock.fixed(Instant.parse("2026-08-25T10:00:00Z"), ZoneOffset.UTC);

    private LoadPlanService service;

    @BeforeEach
    void setUp() {
        service = new LoadPlanService(
                repository,
                numberGenerator,
                manifestLookup,
                vehicleLookup,
                eventPublisher,
                transaction,
                clock
        );
    }

    @Test
    @DisplayName("Creates load plan successfully when manifest is finalized and vehicle is active")
    void shouldCreateLoadPlanSuccessfully() {
        UUID manifestId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();

        ManifestItemPlanningView itemView = new ManifestItemPlanningView(
                itemId, "Generators", new BigDecimal("5"), "Boxes", "MACHINERY", false, null
        );
        ManifestPlanningView manifestView = new ManifestPlanningView(
                manifestId, "CM-2026-000001", true, List.of(itemView)
        );
        VehiclePlanningView vehicleView = new VehiclePlanningView(
                vehicleId, "TRK-100", 15000.0, "AVAILABLE", true
        );

        when(manifestLookup.findManifest(manifestId)).thenReturn(Optional.of(manifestView));
        when(vehicleLookup.findVehicle(vehicleId)).thenReturn(Optional.of(vehicleView));
        when(numberGenerator.next()).thenReturn("LP-2026-000001");
        when(repository.save(any(LoadPlan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LoadPlanUseCase.ItemPlacementCommand placement = new LoadPlanUseCase.ItemPlacementCommand(
                itemId, 0, "FRONT", "S1", "PALLET-1", 1, null
        );
        LoadPlanUseCase.CreateCommand command = new LoadPlanUseCase.CreateCommand(
                manifestId, vehicleId, List.of(placement), "Test notes"
        );

        LoadPlan result = service.create(command, "test-user");

        assertThat(result).isNotNull();
        assertThat(result.getLoadPlanNumber()).isEqualTo("LP-2026-000001");
        assertThat(result.getCargoManifestId()).isEqualTo(manifestId);
        assertThat(result.getVehicleId()).isEqualTo(vehicleId);
        assertThat(result.getPlacements()).hasSize(1);
        assertThat(result.getCreatedBy()).isEqualTo("test-user");

        verify(eventPublisher).publishLoadPlanCreated(any());
    }

    @Test
    @DisplayName("Fails to create load plan when manifest is not finalized")
    void shouldFailWhenManifestNotFinalized() {
        UUID manifestId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();

        ManifestPlanningView unfinalizedManifest = new ManifestPlanningView(
                manifestId, "CM-2026-000001", false, List.of()
        );

        when(manifestLookup.findManifest(manifestId)).thenReturn(Optional.of(unfinalizedManifest));

        LoadPlanUseCase.CreateCommand command = new LoadPlanUseCase.CreateCommand(
                manifestId, vehicleId, List.of(), "Test notes"
        );

        assertThatThrownBy(() -> service.create(command, "test-user"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("unfinalized");
    }

    @Test
    @DisplayName("Fails to create load plan when vehicle is inactive")
    void shouldFailWhenVehicleInactive() {
        UUID manifestId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();

        ManifestPlanningView manifest = new ManifestPlanningView(
                manifestId, "CM-2026-000001", true, List.of()
        );
        VehiclePlanningView inactiveVehicle = new VehiclePlanningView(
                vehicleId, "TRK-100", 15000.0, "MAINTENANCE", false
        );

        when(manifestLookup.findManifest(manifestId)).thenReturn(Optional.of(manifest));
        when(vehicleLookup.findVehicle(vehicleId)).thenReturn(Optional.of(inactiveVehicle));

        LoadPlanUseCase.CreateCommand command = new LoadPlanUseCase.CreateCommand(
                manifestId, vehicleId, List.of(), "Test notes"
        );

        assertThatThrownBy(() -> service.create(command, "test-user"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("not active");
    }

    @Test
    @DisplayName("Validates layout and reports missing items")
    void shouldValidateLayout() {
        UUID planId = UUID.randomUUID();
        UUID manifestId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        UUID item1 = UUID.randomUUID();
        UUID item2 = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now(clock);

        LoadPlan plan = new LoadPlan(
                planId, "LP-001", manifestId, vehicleId, List.of(), "notes", now, now, "user", "user", 0L
        );
        ManifestPlanningView manifest = new ManifestPlanningView(
                manifestId, "CM-001", true, List.of(
                new ManifestItemPlanningView(item1, "Item 1", BigDecimal.ONE, "Box", "GEN", false, null, false, false),
                new ManifestItemPlanningView(item2, "Item 2", BigDecimal.TEN, "Box", "GEN", false, null, false, false)
        )
        );

        when(repository.findById(planId)).thenReturn(Optional.of(plan));
        when(manifestLookup.findManifest(manifestId)).thenReturn(Optional.of(manifest));

        List<LoadPlanViolation> violations = service.validateLayout(planId);
        assertThat(violations).hasSize(2);
        assertThat(violations).allMatch(v -> v.code() == LoadPlanViolationCode.ITEM_NOT_PLACED);
    }

    @Test
    @DisplayName("Validates layout and evaluates structured fragile, temperature, and UNKNOWN violations")
    void shouldEvaluateStructuredSpecialCargoRulesInLayoutValidation() {
        UUID planId = UUID.randomUUID();
        UUID manifestId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        UUID itemFragile = UUID.randomUUID();
        UUID itemStandard = UUID.randomUUID();
        UUID itemTemp = UUID.randomUUID();
        UUID itemUnknown = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now(clock);

        LoadPlan plan = new LoadPlan(
                planId, "LP-001", manifestId, vehicleId,
                List.of(
                        new LoadPlanItemPlacement(UUID.randomUUID(), itemFragile, 0, "ZONE-1", "STACK-SHARED", null, 1, null),
                        new LoadPlanItemPlacement(UUID.randomUUID(), itemStandard, 1, "ZONE-1", "STACK-SHARED", null, 2, null),
                        new LoadPlanItemPlacement(UUID.randomUUID(), itemTemp, 2, null, null, null, 3, null),
                        new LoadPlanItemPlacement(UUID.randomUUID(), itemUnknown, 3, "ZONE-2", "STACK-2", null, 4, null)
                ),
                "notes", now, now, "user", "user", 0L
        );

        ManifestPlanningView manifest = new ManifestPlanningView(
                manifestId, "CM-001", true, List.of(
                new ManifestItemPlanningView(itemFragile, "Fragile Item", BigDecimal.ONE, "Box", "GEN", false, null, true, false),
                new ManifestItemPlanningView(itemStandard, "Standard Item", BigDecimal.ONE, "Box", "GEN", false, null, false, false),
                new ManifestItemPlanningView(itemTemp, "Temp Item", BigDecimal.ONE, "Box", "GEN", false, null, false, true),
                new ManifestItemPlanningView(itemUnknown, "Unknown Item", BigDecimal.ONE, "Box", "GEN", false, null, null, false)
        )
        );

        when(repository.findById(planId)).thenReturn(Optional.of(plan));
        when(manifestLookup.findManifest(manifestId)).thenReturn(Optional.of(manifest));

        List<LoadPlanViolation> violations = service.validateLayout(planId);
        assertThat(violations).anyMatch(v -> v.code() == LoadPlanViolationCode.LOAD_PLAN_FRAGILE_RULE_FAILED);
        assertThat(violations).anyMatch(v -> v.code() == LoadPlanViolationCode.LOAD_PLAN_TEMPERATURE_RULE_FAILED);
        assertThat(violations).anyMatch(v -> v.code() == LoadPlanViolationCode.LOAD_PLAN_SPECIAL_CARGO_CLASSIFICATION_MISSING);
    }

    @Test
    @DisplayName("Validates weight and volume: returns structured incomplete when measurements absent")
    void shouldReturnIncompleteWeightVolumeValidation() {
        UUID planId = UUID.randomUUID();
        UUID manifestId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now(clock);

        LoadPlan plan = new LoadPlan(
                planId, "LP-001", manifestId, vehicleId, List.of(), "notes", now, now, "user", "user", 0L
        );
        ManifestPlanningView manifest = new ManifestPlanningView(
                manifestId, "CM-001", true, List.of()
        );
        VehiclePlanningView vehicle = new VehiclePlanningView(
                vehicleId, "TRK-100", 10000.0, "AVAILABLE", true
        );

        when(repository.findById(planId)).thenReturn(Optional.of(plan));
        when(manifestLookup.findManifest(manifestId)).thenReturn(Optional.of(manifest));
        when(vehicleLookup.findVehicle(vehicleId)).thenReturn(Optional.of(vehicle));

        LoadValidationResult result = service.validateWeightAndVolume(planId, "test-user");

        assertThat(result.overallOutcome()).isEqualTo(ValidationOutcome.INCOMPLETE);
        assertThat(result.payloadResult()).isEqualTo(ValidationOutcome.INCOMPLETE);
        assertThat(result.volumeResult()).isEqualTo(ValidationOutcome.INCOMPLETE);
        assertThat(result.axleResult()).isEqualTo(ValidationOutcome.INCOMPLETE);
        assertThat(result.missingData()).contains("CARGO_ITEM_WEIGHT_DATA_MISSING", "CARGO_ITEM_DIMENSIONS_DATA_MISSING");
        assertThat(result.violations()).isNotEmpty();
    }

    // ──────────────────────────────────────────────────────────
    // US26-AC3 markReady Application Service Tests
    // ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("markReady succeeds when all structural checks pass, persists atomically, and publishes event")
    void shouldMarkReadySuccessfully() {
        UUID planId = UUID.randomUUID();
        UUID manifestId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now(clock);

        LoadPlan plan = new LoadPlan(
                planId, "LP-001", manifestId, vehicleId,
                List.of(new LoadPlanItemPlacement(UUID.randomUUID(), itemId, 0, "FRONT", "S1", "PALLET-1", 1, null)),
                "notes", now, now, "user", "user", 0L
        );

        ManifestPlanningView manifest = new ManifestPlanningView(
                manifestId, "CM-001", true, List.of(
                new ManifestItemPlanningView(itemId, "Item 1", BigDecimal.ONE, "Box", "GEN", false, null, false, false)
        )
        );
        VehiclePlanningView vehicle = new VehiclePlanningView(
                vehicleId, "TRK-100", 10000.0, "AVAILABLE", true
        );

        when(repository.findById(planId)).thenReturn(Optional.of(plan));
        when(manifestLookup.findManifest(manifestId)).thenReturn(Optional.of(manifest));
        when(vehicleLookup.findVehicle(vehicleId)).thenReturn(Optional.of(vehicle));
        when(repository.save(any(LoadPlan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LoadPlan readyPlan = service.markReady(planId, 0L, "lead-planner");

        assertThat(readyPlan).isNotNull();
        assertThat(readyPlan.getReadinessStatus()).isEqualTo(com.transportlogistics.app.freight.loadplanning.domain.LoadPlanReadinessStatus.STRUCTURALLY_READY);
        assertThat(readyPlan.getReadyAt()).isEqualTo(now);
        assertThat(readyPlan.getReadyBy()).isEqualTo("lead-planner");
        assertThat(readyPlan.getUpdatedBy()).isEqualTo("lead-planner");

        verify(eventPublisher).publishLoadPlanUpdated(any());
        verify(repository).save(any(LoadPlan.class));
    }

    @Test
    @DisplayName("markReady rejects stale version with 409 conflict")
    void shouldRejectStaleVersionOnMarkReady() {
        UUID planId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now(clock);

        LoadPlan plan = new LoadPlan(
                planId, "LP-001", UUID.randomUUID(), UUID.randomUUID(),
                List.of(), "notes", now, now, "user", "user", 2L
        );

        when(repository.findById(planId)).thenReturn(Optional.of(plan));

        assertThatThrownBy(() -> service.markReady(planId, 1L, "lead-planner"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("changed by another user");
    }

    @Test
    @DisplayName("markReady rejects when structural violations exist and throws BusinessRuleException")
    void shouldRejectMarkReadyWhenStructuralViolationsExist() {
        UUID planId = UUID.randomUUID();
        UUID manifestId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        UUID item1 = UUID.randomUUID();
        UUID item2 = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now(clock);

        // Only item1 placed, item2 is unplaced
        LoadPlan plan = new LoadPlan(
                planId, "LP-001", manifestId, vehicleId,
                List.of(new LoadPlanItemPlacement(UUID.randomUUID(), item1, 0, "FRONT", "S1", "PALLET-1", 1, null)),
                "notes", now, now, "user", "user", 0L
        );

        ManifestPlanningView manifest = new ManifestPlanningView(
                manifestId, "CM-001", true, List.of(
                new ManifestItemPlanningView(item1, "Item 1", BigDecimal.ONE, "Box", "GEN", false, null, false, false),
                new ManifestItemPlanningView(item2, "Item 2", BigDecimal.ONE, "Box", "GEN", false, null, false, false)
        )
        );
        VehiclePlanningView vehicle = new VehiclePlanningView(
                vehicleId, "TRK-100", 10000.0, "AVAILABLE", true
        );

        when(repository.findById(planId)).thenReturn(Optional.of(plan));
        when(manifestLookup.findManifest(manifestId)).thenReturn(Optional.of(manifest));
        when(vehicleLookup.findVehicle(vehicleId)).thenReturn(Optional.of(vehicle));

        assertThatThrownBy(() -> service.markReady(planId, 0L, "lead-planner"))
                .isInstanceOf(com.transportlogistics.app.shared.domain.BusinessRuleException.class)
                .hasMessageContaining("ITEM_NOT_PLACED");
    }

    @Test
    @DisplayName("markReady fails when load plan is not found")
    void shouldFailWhenLoadPlanNotFound() {
        UUID planId = UUID.randomUUID();
        when(repository.findById(planId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markReady(planId, 0L, "lead-planner"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Load plan not found");
    }

    @Test
    @DisplayName("markReady fails when cargo manifest is not found or not finalized")
    void shouldFailWhenManifestMissingOrUnfinalized() {
        UUID planId = UUID.randomUUID();
        UUID manifestId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now(clock);

        LoadPlan plan = new LoadPlan(
                planId, "LP-001", manifestId, vehicleId, List.of(), "notes", now, now, "user", "user", 0L
        );
        when(repository.findById(planId)).thenReturn(Optional.of(plan));

        // Manifest missing
        when(manifestLookup.findManifest(manifestId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.markReady(planId, 0L, "lead-planner"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Cargo manifest not found");

        // Manifest unfinalized
        ManifestPlanningView unfinalizedManifest = new ManifestPlanningView(manifestId, "CM-001", false, List.of());
        when(manifestLookup.findManifest(manifestId)).thenReturn(Optional.of(unfinalizedManifest));
        assertThatThrownBy(() -> service.markReady(planId, 0L, "lead-planner"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Cannot plan loads for unfinalized cargo manifest");
    }

    @Test
    @DisplayName("markReady fails when vehicle is not found or inactive")
    void shouldFailWhenVehicleMissingOrInactive() {
        UUID planId = UUID.randomUUID();
        UUID manifestId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now(clock);

        LoadPlan plan = new LoadPlan(
                planId, "LP-001", manifestId, vehicleId, List.of(), "notes", now, now, "user", "user", 0L
        );
        ManifestPlanningView manifest = new ManifestPlanningView(manifestId, "CM-001", true, List.of());

        when(repository.findById(planId)).thenReturn(Optional.of(plan));
        when(manifestLookup.findManifest(manifestId)).thenReturn(Optional.of(manifest));

        // Vehicle missing
        when(vehicleLookup.findVehicle(vehicleId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.markReady(planId, 0L, "lead-planner"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Vehicle not found");

        // Vehicle inactive
        VehiclePlanningView inactiveVehicle = new VehiclePlanningView(vehicleId, "TRK-100", 10000.0, "AVAILABLE", false);
        when(vehicleLookup.findVehicle(vehicleId)).thenReturn(Optional.of(inactiveVehicle));
        assertThatThrownBy(() -> service.markReady(planId, 0L, "lead-planner"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Vehicle is not active");
    }

    @Test
    @DisplayName("markReady requires authenticated actor and non-null version")
    void shouldRequireActorAndVersion() {
        UUID planId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now(clock);

        LoadPlan plan = new LoadPlan(
                planId, "LP-001", UUID.randomUUID(), UUID.randomUUID(), List.of(), "notes", now, now, "user", "user", 0L
        );

        // Missing actor
        assertThatThrownBy(() -> service.markReady(planId, 0L, null))
                .isInstanceOf(com.transportlogistics.app.shared.domain.BusinessRuleException.class)
                .hasMessageContaining("actor is required");

        assertThatThrownBy(() -> service.markReady(planId, 0L, "   "))
                .isInstanceOf(com.transportlogistics.app.shared.domain.BusinessRuleException.class)
                .hasMessageContaining("actor is required");

        // Missing version
        when(repository.findById(planId)).thenReturn(Optional.of(plan));
        assertThatThrownBy(() -> service.markReady(planId, null, "lead-planner"))
                .isInstanceOf(com.transportlogistics.app.shared.domain.BusinessRuleException.class)
                .hasMessageContaining("Version is required");
    }

    @Test
    @DisplayName("validateWeightAndVolume returns PASS when measurements are within all vehicle capacities")
    void shouldReturnPassWhenCargoIsWithinCapacities() {
        UUID planId = UUID.randomUUID();
        UUID manifestId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        UUID item1 = UUID.randomUUID();
        UUID item2 = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now(clock);

        LoadPlan plan = new LoadPlan(
                planId, "LP-001", manifestId, vehicleId, List.of(), "notes", now, now, "user", "user", 0L
        );
        ManifestPlanningView manifest = new ManifestPlanningView(
                manifestId, "CM-001", true, List.of(
                new ManifestItemPlanningView(item1, "Generators", new BigDecimal("2"), "Box", "GEN", false, null, false, false,
                        new BigDecimal("500.00"), "KG", new BigDecimal("1.0"), new BigDecimal("1.0"), new BigDecimal("1.0"), "M"),
                new ManifestItemPlanningView(item2, "Pumps", new BigDecimal("2"), "Box", "GEN", false, null, false, false,
                        new BigDecimal("500.00"), "KG", new BigDecimal("1.0"), new BigDecimal("1.0"), new BigDecimal("1.0"), "M")
        )
        );
        VehiclePlanningView vehicle = new VehiclePlanningView(
                vehicleId, "TRK-100", 5000.0, 3500.0, 8500.0, 25.5, 2, 4500.0, "AVAILABLE", true
        );

        when(repository.findById(planId)).thenReturn(Optional.of(plan));
        when(manifestLookup.findManifest(manifestId)).thenReturn(Optional.of(manifest));
        when(vehicleLookup.findVehicle(vehicleId)).thenReturn(Optional.of(vehicle));

        LoadValidationResult result = service.validateWeightAndVolume(planId, "lead-planner");

        assertThat(result.overallOutcome()).isEqualTo(ValidationOutcome.PASS);
        assertThat(result.payloadResult()).isEqualTo(ValidationOutcome.PASS);
        assertThat(result.volumeResult()).isEqualTo(ValidationOutcome.PASS);
        assertThat(result.gvwResult()).isEqualTo(ValidationOutcome.PASS);
        assertThat(result.cargoWeightKg()).isEqualByComparingTo("2000.00");
        assertThat(result.cargoVolumeM3()).isEqualByComparingTo("4.000");
        assertThat(result.projectedGrossWeightKg()).isEqualByComparingTo("5500.00");
    }

    @Test
    @DisplayName("validateWeightAndVolume returns FAIL when cargo weight exceeds payload capacity")
    void shouldReturnFailWhenPayloadExceeded() {
        UUID planId = UUID.randomUUID();
        UUID manifestId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        UUID item1 = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now(clock);

        LoadPlan plan = new LoadPlan(
                planId, "LP-001", manifestId, vehicleId, List.of(), "notes", now, now, "user", "user", 0L
        );
        ManifestPlanningView manifest = new ManifestPlanningView(
                manifestId, "CM-001", true, List.of(
                new ManifestItemPlanningView(item1, "Heavy Machinery", new BigDecimal("2"), "Box", "MACH", false, null, false, false,
                        new BigDecimal("3000.00"), "KG", new BigDecimal("1.0"), new BigDecimal("1.0"), new BigDecimal("1.0"), "M")
        )
        );
        VehiclePlanningView vehicle = new VehiclePlanningView(
                vehicleId, "TRK-100", 5000.0, 3500.0, 8500.0, 25.5, 2, 4500.0, "AVAILABLE", true
        );

        when(repository.findById(planId)).thenReturn(Optional.of(plan));
        when(manifestLookup.findManifest(manifestId)).thenReturn(Optional.of(manifest));
        when(vehicleLookup.findVehicle(vehicleId)).thenReturn(Optional.of(vehicle));

        LoadValidationResult result = service.validateWeightAndVolume(planId, "lead-planner");

        assertThat(result.overallOutcome()).isEqualTo(ValidationOutcome.FAIL);
        assertThat(result.payloadResult()).isEqualTo(ValidationOutcome.FAIL);
        assertThat(result.cargoWeightKg()).isEqualByComparingTo("6000.00");
        assertThat(result.violations()).anyMatch(v -> "VEHICLE_PAYLOAD_EXCEEDED".equals(v.code()));
    }

    @Test
    @DisplayName("validateWeightAndVolume returns FAIL when cargo volume exceeds volume capacity")
    void shouldReturnFailWhenVolumeExceeded() {
        UUID planId = UUID.randomUUID();
        UUID manifestId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        UUID item1 = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now(clock);

        LoadPlan plan = new LoadPlan(
                planId, "LP-001", manifestId, vehicleId, List.of(), "notes", now, now, "user", "user", 0L
        );
        ManifestPlanningView manifest = new ManifestPlanningView(
                manifestId, "CM-001", true, List.of(
                new ManifestItemPlanningView(item1, "Bulky Foam", new BigDecimal("2"), "Box", "FOAM", false, null, false, false,
                        new BigDecimal("100.00"), "KG", new BigDecimal("4.0"), new BigDecimal("2.0"), new BigDecimal("2.0"), "M")
        )
        );
        VehiclePlanningView vehicle = new VehiclePlanningView(
                vehicleId, "TRK-100", 5000.0, 3500.0, 8500.0, 25.5, 2, 4500.0, "AVAILABLE", true
        );

        when(repository.findById(planId)).thenReturn(Optional.of(plan));
        when(manifestLookup.findManifest(manifestId)).thenReturn(Optional.of(manifest));
        when(vehicleLookup.findVehicle(vehicleId)).thenReturn(Optional.of(vehicle));

        LoadValidationResult result = service.validateWeightAndVolume(planId, "lead-planner");

        assertThat(result.overallOutcome()).isEqualTo(ValidationOutcome.FAIL);
        assertThat(result.volumeResult()).isEqualTo(ValidationOutcome.FAIL);
        assertThat(result.cargoVolumeM3()).isEqualByComparingTo("32.000");
        assertThat(result.violations()).anyMatch(v -> "VEHICLE_VOLUME_CAPACITY_EXCEEDED".equals(v.code()));
    }

    @Test
    @DisplayName("validateWeightAndVolume returns INCOMPLETE when measurements are missing")
    void shouldReturnIncompleteWhenMeasurementsMissing() {
        UUID planId = UUID.randomUUID();
        UUID manifestId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        UUID item1 = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now(clock);

        LoadPlan plan = new LoadPlan(
                planId, "LP-001", manifestId, vehicleId, List.of(), "notes", now, now, "user", "user", 0L
        );
        ManifestPlanningView manifest = new ManifestPlanningView(
                manifestId, "CM-001", true, List.of(
                new ManifestItemPlanningView(item1, "Unmeasured Cargo", new BigDecimal("2"), "Box", "GEN", false, null, false, false,
                        null, null, null, null, null, null)
        )
        );
        VehiclePlanningView vehicle = new VehiclePlanningView(
                vehicleId, "TRK-100", 5000.0, 3500.0, 8500.0, 25.5, 2, 4500.0, "AVAILABLE", true
        );

        when(repository.findById(planId)).thenReturn(Optional.of(plan));
        when(manifestLookup.findManifest(manifestId)).thenReturn(Optional.of(manifest));
        when(vehicleLookup.findVehicle(vehicleId)).thenReturn(Optional.of(vehicle));

        LoadValidationResult result = service.validateWeightAndVolume(planId, "lead-planner");

        assertThat(result.overallOutcome()).isEqualTo(ValidationOutcome.INCOMPLETE);
        assertThat(result.payloadResult()).isEqualTo(ValidationOutcome.INCOMPLETE);
        assertThat(result.volumeResult()).isEqualTo(ValidationOutcome.INCOMPLETE);
        assertThat(result.missingData()).contains("CARGO_ITEM_WEIGHT_DATA_MISSING", "CARGO_ITEM_DIMENSIONS_DATA_MISSING");
    }
}
