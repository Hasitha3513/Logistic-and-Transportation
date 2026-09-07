package com.transportlogistics.app.fleet.payroll;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transportlogistics.app.fleet.DriverLookup;
import com.transportlogistics.app.fleet.DriverPayrollIntegrationPort;
import com.transportlogistics.app.fleet.DriverPayrollSourcePort;
import com.transportlogistics.app.fleet.FleetDriverSummary;
import com.transportlogistics.app.fleet.payroll.application.DriverPayrollService;
import com.transportlogistics.app.fleet.payroll.domain.DriverPayrollInputBatch;
import com.transportlogistics.app.fleet.payroll.domain.DriverPayrollInputLine;
import com.transportlogistics.app.fleet.payroll.ports.inbound.DriverPayrollUseCase;
import com.transportlogistics.app.fleet.payroll.ports.outbound.DriverPayrollStore;
import com.transportlogistics.app.fleet.payroll.ports.outbound.DriverPayrollTransaction;
import com.transportlogistics.app.shared.DurableEventEnvelope;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.shared.domain.ConflictException;
import com.transportlogistics.app.support.PostgreSqlIntegrationTest;
import com.transportlogistics.app.tenancy.TenantContextExecutor;
import com.transportlogistics.app.tenancy.TenantExecutionContext;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class DriverPayrollAtomicityAndIsolationPostgreSqlAcceptanceTest extends PostgreSqlIntegrationTest {
    private static final UUID TENANT_A = UUID.fromString("4f8b6a3b-2c1e-4d89-9a72-f9e4c5b3671a");
    private static final UUID TENANT_B = UUID.fromString("46000000-0000-0000-0000-000000000002");
    private static final UUID PREPARER = UUID.fromString("46000000-0000-0000-0000-000000000011");
    private static final UUID APPROVER = UUID.fromString("46000000-0000-0000-0000-000000000012");
    @Autowired DriverPayrollStore store;
    @Autowired DriverPayrollTransaction transaction;
    @Autowired DriverPayrollIntegrationPort durableIntegration;
    @Autowired ObjectMapper json;
    @Autowired Clock clock;
    @Autowired JdbcTemplate jdbc;
    @Autowired TenantContextExecutor contexts;
    private DriverLookup drivers;
    private DriverPayrollSourcePort trips;

    @BeforeEach
    void setUpSources() {
        drivers = mock(DriverLookup.class);
        when(drivers.findDriver(org.mockito.ArgumentMatchers.any())).thenAnswer(call -> {
            UUID id = call.getArgument(0);
            return Optional.of(new FleetDriverSummary(id, "DRV", "Safe", "Driver", "AVAILABLE", true));
        });
        trips = mock(DriverPayrollSourcePort.class);
        when(trips.trip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenAnswer(call ->
            Optional.of(new DriverPayrollSourcePort.TripFact(call.getArgument(1), "TRIP", "COMPLETED",
                call.getArgument(0), OffsetDateTime.now(ZoneOffset.UTC).minusDays(1))));
    }

    @Test
    void exportCommitPersistsBatchReleaseIdentityHistoryAndMatchingOutboxAtomically() {
        var service = service(durableIntegration);
        var approved = within(TENANT_A, () -> approved(service, "commit"));
        var requested = within(TENANT_A, () -> service.export(context(TENANT_A, PREPARER), approved.id(),
            approved.version()));

        assertThat(requested.lifecycle()).isEqualTo(DriverPayrollInputBatch.Lifecycle.EXPORT_REQUESTED);
        assertThat(jdbc.queryForMap("select tenant_id,event_id,aggregate_id,event_type from integration_outbox_event "
            + "where event_id=?", requested.exportEventId()))
            .containsEntry("tenant_id", TENANT_A)
            .containsEntry("event_id", requested.exportEventId())
            .containsEntry("aggregate_id", requested.id())
            .containsEntry("event_type", DriverPayrollInputExportRequestedV1.EVENT_TYPE);
        assertThat(actionCount(requested.id(), "EXPORT_REQUESTED")).isEqualTo(1);
    }

    @Test
    void controlledFailureAfterOutboxPublishRollsBackStateOutboxHistoryAndReleaseIdentity() {
        DriverPayrollIntegrationPort failAfterPublish = new DriverPayrollIntegrationPort() {
            @Override public Optional<UUID> activePayrollConfiguration(UUID tenantId) {
                return Optional.of(UUID.randomUUID());
            }
            @Override public void publish(DurableEventEnvelope event) {
                durableIntegration.publish(event);
                throw new IllegalStateException("controlled rollback after publish");
            }
        };
        var service = service(failAfterPublish);
        var approved = within(TENANT_A, () -> approved(service, "rollback"));

        assertThatThrownBy(() -> within(TENANT_A, () -> service.export(context(TENANT_A, PREPARER),
            approved.id(), approved.version()))).isInstanceOf(IllegalStateException.class)
            .hasMessage("controlled rollback after publish");
        var persisted = within(TENANT_A, () -> service.get(TENANT_A, approved.id()));
        assertThat(persisted.lifecycle()).isEqualTo(DriverPayrollInputBatch.Lifecycle.APPROVED);
        assertThat(persisted.exportEventId()).isNull();
        assertThat(jdbc.queryForObject("select count(*) from integration_outbox_event where aggregate_id=?",
            Integer.class, approved.id())).isZero();
        assertThat(actionCount(approved.id(), "EXPORT_REQUESTED")).isZero();
    }

    @Test
    void durableMappingReplayConflictAndTenantLeadingKeyIsolationPreserveOriginalAuditSnapshot() {
        var service = service(durableIntegration); UUID driverA = UUID.randomUUID(); UUID driverB = UUID.randomUUID();
        String key = "shared-key-" + UUID.randomUUID();
        var command = new DriverPayrollUseCase.MappingCommand("PAYROLL", "WORKER-A", true, 0);
        var first = within(TENANT_A, () -> service.mapWorker(context(TENANT_A, PREPARER), driverA, command, key));
        var rowBefore = jdbc.queryForMap("select * from driver_payroll_worker_mapping_command where tenant_id=? "
            + "and idempotency_key=?", TENANT_A, key);
        int historyBefore = jdbc.queryForObject("select count(*) from driver_payroll_input_history where tenant_id=?",
            Integer.class, TENANT_A);
        var replay = within(TENANT_A, () -> service.mapWorker(context(TENANT_A, PREPARER), driverA, command, key));
        assertThat(replay).isEqualTo(first);
        assertThat(jdbc.queryForMap("select * from driver_payroll_worker_mapping_command where tenant_id=? "
            + "and idempotency_key=?", TENANT_A, key)).isEqualTo(rowBefore);
        assertThat(jdbc.queryForObject("select count(*) from driver_payroll_input_history where tenant_id=?",
            Integer.class, TENANT_A)).isEqualTo(historyBefore);
        assertThatThrownBy(() -> within(TENANT_A, () -> service.mapWorker(context(TENANT_A, PREPARER), driverA,
            new DriverPayrollUseCase.MappingCommand("PAYROLL", "WORKER-CHANGED", true, 0), key)))
            .isInstanceOf(ConflictException.class).hasMessageContaining("DRIVER_PAYROLL_IDEMPOTENCY_CONFLICT");
        var tenantB = within(TENANT_B, () -> service.mapWorker(context(TENANT_B, PREPARER), driverB,
            new DriverPayrollUseCase.MappingCommand("PAYROLL", "WORKER-B", true, 0), key));
        assertThat(tenantB.tenantId()).isEqualTo(TENANT_B);
        assertThat(jdbc.queryForObject("select count(*) from driver_payroll_worker_mapping_command "
            + "where idempotency_key=?", Integer.class, key)).isEqualTo(2);
    }

    @Test
    void tenantBMatrixCannotReadApproveExportHistoryMappingOrCorrectTenantABatch() {
        var service = service(durableIntegration); UUID driver = UUID.randomUUID();
        var approved = within(TENANT_A, () -> approved(service, "tenant-matrix", driver, UUID.randomUUID()));
        assertThatThrownBy(() -> within(TENANT_B, () -> service.get(TENANT_B, approved.id())))
            .isInstanceOf(BusinessRuleException.class).hasMessageContaining("DRIVER_PAYROLL_BATCH_NOT_FOUND");
        assertThatThrownBy(() -> within(TENANT_B, () -> service.history(TENANT_B, approved.id())))
            .isInstanceOf(BusinessRuleException.class).hasMessageContaining("DRIVER_PAYROLL_BATCH_NOT_FOUND");
        assertThatThrownBy(() -> within(TENANT_B, () -> service.approve(context(TENANT_B, APPROVER), approved.id(),
            approved.version()))).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> within(TENANT_B, () -> service.export(context(TENANT_B, PREPARER), approved.id(),
            approved.version()))).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> within(TENANT_B, () -> service.correction(context(TENANT_B, PREPARER), approved.id(),
            correction(approved), "foreign-correction"))).isInstanceOf(BusinessRuleException.class);
        var mappingB = within(TENANT_B, () -> service.mapping(TENANT_B, driver));
        assertThat(mappingB).isEmpty();
    }

    @Test
    void payrollWorkflowDoesNotMutateDriverTripOrIntegrationConfigurationAndDeliveryIsIdempotent() {
        var service = service(durableIntegration); UUID driver = UUID.randomUUID(); UUID trip = UUID.randomUUID();
        insertSourceRows(driver, trip);
        var driverBefore = jdbc.queryForMap("select * from driver where id=?", driver);
        var tripBefore = jdbc.queryForMap("select * from trip where id=?", trip);
        int configurationsBefore = jdbc.queryForObject("select count(*) from integration_configuration",
            Integer.class);
        var approved = within(TENANT_A, () -> approved(service, "immutability", driver, trip));
        var requested = within(TENANT_A, () -> service.export(context(TENANT_A, PREPARER), approved.id(),
            approved.version()));
        within(TENANT_A, () -> { service.fileDelivered(TENANT_A, requested.exportEventId(), "a".repeat(64),
            "payroll.json", OffsetDateTime.now(ZoneOffset.UTC)); return null; });
        var exported = within(TENANT_A, () -> service.get(TENANT_A, requested.id()));
        within(TENANT_A, () -> { service.fileDelivered(TENANT_A, requested.exportEventId(), "a".repeat(64),
            "payroll.json", OffsetDateTime.now(ZoneOffset.UTC)); return null; });
        assertThat(exported.lifecycle()).isEqualTo(DriverPayrollInputBatch.Lifecycle.EXPORTED);
        assertThat(actionCount(exported.id(), "FILE_DELIVERED")).isEqualTo(1);
        assertThat(jdbc.queryForMap("select * from driver where id=?", driver)).isEqualTo(driverBefore);
        assertThat(jdbc.queryForMap("select * from trip where id=?", trip)).isEqualTo(tripBefore);
        assertThat(jdbc.queryForObject("select count(*) from integration_configuration", Integer.class))
            .isEqualTo(configurationsBefore);
    }

    private DriverPayrollService service(DriverPayrollIntegrationPort publisher) {
        DriverPayrollIntegrationPort integration = new DriverPayrollIntegrationPort() {
            @Override public Optional<UUID> activePayrollConfiguration(UUID tenantId) {
                return Optional.of(UUID.fromString("46000000-0000-0000-0000-000000000099"));
            }
            @Override public void publish(DurableEventEnvelope event) { publisher.publish(event); }
        };
        return new DriverPayrollService(store, trips, drivers, integration, transaction, json, clock);
    }

    private DriverPayrollInputBatch approved(DriverPayrollService service, String key) {
        return approved(service, key, UUID.randomUUID(), UUID.randomUUID());
    }

    private DriverPayrollInputBatch approved(DriverPayrollService service, String key, UUID driver, UUID trip) {
        service.mapWorker(context(TENANT_A, PREPARER), driver,
            new DriverPayrollUseCase.MappingCommand("PAYROLL", "WORKER-" + driver, true, 0), "map-" + key);
        var created = service.create(context(TENANT_A, PREPARER), create(), "batch-" + key);
        var lined = service.replaceLines(context(TENANT_A, PREPARER), created.id(), created.version(),
            List.of(line(driver, trip, null)));
        var validated = service.validate(context(TENANT_A, PREPARER), lined.id(), lined.version());
        return service.approve(context(TENANT_A, APPROVER), validated.id(), validated.version());
    }

    private static DriverPayrollUseCase.Create create() {
        var now = OffsetDateTime.now(ZoneOffset.UTC);
        return new DriverPayrollUseCase.Create(DriverPayrollInputBatch.Type.REGULAR, null,
            LocalDate.now(ZoneOffset.UTC).minusDays(7), LocalDate.now(ZoneOffset.UTC).plusDays(1),
            now.plusDays(1), "LKR");
    }

    private static DriverPayrollUseCase.CreateCorrection correction(DriverPayrollInputBatch original) {
        return new DriverPayrollUseCase.CreateCorrection(original.version(), original.periodStart(),
            original.periodEndExclusive(), original.cutoffAt(), original.currency(), List.of(
            line(UUID.randomUUID(), UUID.randomUUID(), original.lines().get(0).id())));
    }

    private static DriverPayrollUseCase.LineCommand line(UUID driver, UUID trip, UUID original) {
        return new DriverPayrollUseCase.LineCommand(UUID.randomUUID(), driver, trip, "TRIP",
            original == null ? DriverPayrollInputLine.Category.TRIP_EARNING : DriverPayrollInputLine.Category.DEDUCTION,
            original == null ? "AUTHORIZED_RATE" : "FIXED_AMOUNT", "source", BigDecimal.ONE,
            original == null ? DriverPayrollInputLine.Unit.TRIP : DriverPayrollInputLine.Unit.FIXED,
            BigDecimal.TEN, original == null ? null : BigDecimal.ONE, original);
    }

    private static DriverPayrollUseCase.Context context(UUID tenant, UUID actor) {
        return new DriverPayrollUseCase.Context(tenant, actor, "acceptance", "us46");
    }

    private <T> T within(UUID tenant, Supplier<T> work) {
        return contexts.within(new TenantExecutionContext(tenant, PREPARER, "acceptance", "us46"), work);
    }

    private int actionCount(UUID batch, String action) {
        return jdbc.queryForObject("select count(*) from driver_payroll_input_history where tenant_id=? "
            + "and batch_id=? and action=?", Integer.class, TENANT_A, batch, action);
    }

    private void insertSourceRows(UUID driver, UUID trip) {
        UUID origin = UUID.randomUUID(); UUID destination = UUID.randomUUID();
        jdbc.update("insert into location(id,code,name,active,tenant_id) values(?,?,?,true,?)",
            origin, "US46-O-" + origin.toString().substring(0, 20), "US46 origin", TENANT_A);
        jdbc.update("insert into location(id,code,name,active,tenant_id) values(?,?,?,true,?)",
            destination, "US46-D-" + destination.toString().substring(0, 20), "US46 destination", TENANT_A);
        jdbc.update("insert into driver(id,employee_number,first_name,last_name,status,active,tenant_id) "
            + "values(?,?, 'Safe','Driver','ACTIVE',true,?)", driver, "US46-" + driver, TENANT_A);
        jdbc.update("insert into trip(id,trip_number,priority,status,origin_location_id,destination_location_id,"
            + "requested_start_time,requested_end_time,driver_id,actual_start_time,actual_end_time,created_at,"
            + "updated_at,tenant_id) values(?,?,'NORMAL','COMPLETED',?,?,now()-interval '2 days',"
            + "now()-interval '1 day',?,now()-interval '2 days',now()-interval '1 day',now(),now(),?)",
            trip, "US46-" + trip, origin, destination, driver, TENANT_A);
    }
}
