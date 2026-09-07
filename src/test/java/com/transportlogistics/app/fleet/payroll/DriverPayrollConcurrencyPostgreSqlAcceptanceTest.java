package com.transportlogistics.app.fleet.payroll;

import static org.assertj.core.api.Assertions.assertThat;
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
import com.transportlogistics.app.shared.domain.ConflictException;
import com.transportlogistics.app.support.PostgreSqlIntegrationTest;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class DriverPayrollConcurrencyPostgreSqlAcceptanceTest extends PostgreSqlIntegrationTest {
    private static final UUID TENANT = UUID.fromString("4f8b6a3b-2c1e-4d89-9a72-f9e4c5b3671a");
    private static final UUID PREPARER = UUID.fromString("46000000-0000-0000-0000-000000000011");
    private static final UUID APPROVER = UUID.fromString("46000000-0000-0000-0000-000000000012");
    @Autowired DriverPayrollStore store;
    @Autowired DriverPayrollTransaction transaction;
    @Autowired DriverPayrollIntegrationPort durableIntegration;
    @Autowired ObjectMapper json;
    @Autowired Clock clock;
    @Autowired JdbcTemplate jdbc;
    private DriverPayrollService service;

    @BeforeEach
    void setUpService() {
        DriverLookup drivers = mock(DriverLookup.class);
        when(drivers.findDriver(org.mockito.ArgumentMatchers.any())).thenAnswer(call -> {
            UUID id = call.getArgument(0);
            return Optional.of(new FleetDriverSummary(id, "DRV", "Safe", "Driver", "AVAILABLE", true));
        });
        DriverPayrollSourcePort trips = mock(DriverPayrollSourcePort.class);
        when(trips.trip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenAnswer(call ->
            Optional.of(new DriverPayrollSourcePort.TripFact(call.getArgument(1), "TRIP", "COMPLETED",
                call.getArgument(0), OffsetDateTime.now(ZoneOffset.UTC).minusDays(1))));
        DriverPayrollIntegrationPort integration = new DriverPayrollIntegrationPort() {
            @Override public Optional<UUID> activePayrollConfiguration(UUID tenantId) {
                return Optional.of(UUID.fromString("46000000-0000-0000-0000-000000000099"));
            }
            @Override public void publish(com.transportlogistics.app.shared.DurableEventEnvelope event) {
                durableIntegration.publish(event);
            }
        };
        service = new DriverPayrollService(store, trips, drivers, integration, transaction, json, clock);
    }

    @Test
    void race1DuplicateBatchCreateProducesOneBatchAndOneHistory() throws Exception {
        String key = "race-batch-" + UUID.randomUUID();
        var results = race(() -> service.create(preparer(), create(), key),
            () -> service.create(preparer(), create(), key));
        assertThat(results).allSatisfy(result -> assertThat(result.failure()).isNull());
        assertThat(results.get(0).value().id()).isEqualTo(results.get(1).value().id());
        assertThat(count("driver_payroll_input_batch", "idempotency_key", key)).isEqualTo(1);
        assertThat(history(results.get(0).value().id(), "BATCH_CREATED")).isEqualTo(1);
    }

    @Test
    void race2DuplicateWorkerMappingReplayProducesOneCommandAndStableResult() throws Exception {
        UUID driver = UUID.randomUUID(); String key = "race-map-" + UUID.randomUUID();
        var command = new DriverPayrollUseCase.MappingCommand("PAYROLL", "WORKER-1", true, 0);
        var results = race(() -> service.mapWorker(preparer(), driver, command, key),
            () -> service.mapWorker(preparer(), driver, command, key));
        assertThat(results).allSatisfy(result -> assertThat(result.failure()).isNull());
        assertThat(results.get(0).value()).isEqualTo(results.get(1).value());
        assertThat(count("driver_payroll_worker_mapping_command", "idempotency_key", key)).isEqualTo(1);
    }

    @Test
    void race3SameMappingKeyDifferentRequestHasOneWinnerAndOneDeterministicConflict() throws Exception {
        UUID driver = UUID.randomUUID(); String key = "race-conflict-" + UUID.randomUUID();
        var results = race(
            () -> service.mapWorker(preparer(), driver,
                new DriverPayrollUseCase.MappingCommand("PAYROLL", "WORKER-A", true, 0), key),
            () -> service.mapWorker(preparer(), driver,
                new DriverPayrollUseCase.MappingCommand("PAYROLL", "WORKER-B", true, 0), key));
        assertThat(results.stream().filter(Result::succeeded)).hasSize(1);
        assertThat(results.stream().map(Result::failure).filter(ConflictException.class::isInstance)).hasSize(1);
        assertThat(count("driver_payroll_worker_mapping_command", "idempotency_key", key)).isEqualTo(1);
    }

    @Test
    void race4ApproveVersusLineEditCannotProduceApprovedSilentlyChangedLines() throws Exception {
        var source = new Source(UUID.randomUUID(), UUID.randomUUID());
        var batch = validated("approve-edit", source);
        var edited = line(source, DriverPayrollInputLine.Category.ALLOWANCE);
        var results = race(() -> service.approve(approver(), batch.id(), batch.version()),
            () -> service.replaceLines(preparer(), batch.id(), batch.version(), List.of(command(edited, null))));
        assertThat(results.stream().filter(Result::succeeded)).hasSize(1);
        var persisted = service.get(TENANT, batch.id());
        if (persisted.lifecycle() == DriverPayrollInputBatch.Lifecycle.APPROVED) {
            assertThat(persisted.lines()).extracting(DriverPayrollInputLine::category)
                .containsExactly(DriverPayrollInputLine.Category.TRIP_EARNING);
        } else {
            assertThat(persisted.lifecycle()).isEqualTo(DriverPayrollInputBatch.Lifecycle.DRAFT);
            assertThat(persisted.lines()).extracting(DriverPayrollInputLine::category)
                .containsExactly(DriverPayrollInputLine.Category.ALLOWANCE);
        }
    }

    @Test
    void race5DoubleApproveHasOneEffectiveApprovalHistory() throws Exception {
        var batch = validated("double-approve", new Source(UUID.randomUUID(), UUID.randomUUID()));
        var results = race(() -> service.approve(approver(), batch.id(), batch.version()),
            () -> service.approve(approver(), batch.id(), batch.version()));
        assertThat(results.stream().filter(Result::succeeded)).hasSize(1);
        assertThat(service.get(TENANT, batch.id()).lifecycle()).isEqualTo(DriverPayrollInputBatch.Lifecycle.APPROVED);
        assertThat(history(batch.id(), "BATCH_APPROVED")).isEqualTo(1);
    }

    @Test
    void race6DoubleExportPersistsOneStableReleaseEventAndOneOutboxEvent() throws Exception {
        var approved = approved("double-export", new Source(UUID.randomUUID(), UUID.randomUUID()));
        var results = race(() -> service.export(preparer(), approved.id(), approved.version()),
            () -> service.export(preparer(), approved.id(), approved.version()));
        assertThat(results.stream().filter(Result::succeeded)).hasSize(1);
        var persisted = service.get(TENANT, approved.id());
        assertThat(persisted.lifecycle()).isEqualTo(DriverPayrollInputBatch.Lifecycle.EXPORT_REQUESTED);
        assertThat(outbox(persisted.exportEventId())).isEqualTo(1);
        assertThat(history(approved.id(), "EXPORT_REQUESTED")).isEqualTo(1);
    }

    @Test
    void race7CorrectionVersusExportPreservesLifecycleConsistency() throws Exception {
        var approved = approved("correction-export", new Source(UUID.randomUUID(), UUID.randomUUID()));
        var delta = line(new Source(UUID.randomUUID(), UUID.randomUUID()), DriverPayrollInputLine.Category.DEDUCTION);
        var correction = new DriverPayrollUseCase.CreateCorrection(approved.version(), create().periodStart(),
            create().periodEndExclusive(), create().cutoffAt(), "LKR", List.of(command(delta, approved.lines().get(0).id())));
        var results = race(() -> service.export(preparer(), approved.id(), approved.version()),
            () -> service.correction(preparer(), approved.id(), correction, "race-correction-" + approved.id()));
        assertThat(results.stream().filter(Result::succeeded)).hasSizeGreaterThanOrEqualTo(1);
        assertThat(service.get(TENANT, approved.id()).lifecycle())
            .isIn(DriverPayrollInputBatch.Lifecycle.EXPORT_REQUESTED, DriverPayrollInputBatch.Lifecycle.EXPORTED);
        assertThat(jdbc.queryForObject("select count(*) from driver_payroll_input_batch where tenant_id=? "
            + "and correction_of_batch_id=?", Integer.class, TENANT, approved.id())).isLessThanOrEqualTo(1);
    }

    @Test
    void race8DuplicateReleasedSourceInclusionAllowsOneApproval() throws Exception {
        var source = new Source(UUID.randomUUID(), UUID.randomUUID());
        var first = validated("duplicate-source-a", source);
        var second = validated("duplicate-source-b", source);
        var results = race(() -> service.approve(approver(), first.id(), first.version()),
            () -> service.approve(approver(), second.id(), second.version()));
        assertThat(results.stream().filter(Result::succeeded)).hasSize(1);
        assertThat(jdbc.queryForObject("select count(*) from driver_payroll_input_batch where tenant_id=? "
            + "and id in (?,?) and lifecycle='APPROVED'", Integer.class, TENANT, first.id(), second.id())).isEqualTo(1);
    }

    @Test
    void race9ExportReplayReturnsSameEventAndOneEffectiveOutboxIdentity() throws Exception {
        var approved = approved("export-replay", new Source(UUID.randomUUID(), UUID.randomUUID()));
        var released = service.export(preparer(), approved.id(), approved.version());
        var results = race(() -> service.export(preparer(), released.id(), released.version()),
            () -> service.export(preparer(), released.id(), released.version()));
        assertThat(results).allSatisfy(result -> {
            assertThat(result.failure()).isNull();
            assertThat(result.value().exportEventId()).isEqualTo(released.exportEventId());
        });
        assertThat(outbox(released.exportEventId())).isEqualTo(1);
        assertThat(history(released.id(), "EXPORT_REQUESTED")).isEqualTo(1);
    }

    private DriverPayrollInputBatch validated(String key, Source source) {
        service.mapWorker(preparer(), source.driver(),
            new DriverPayrollUseCase.MappingCommand("PAYROLL", "WORKER-" + source.driver(), true, 0), "map-" + key);
        var batch = service.create(preparer(), create(), "batch-" + key);
        batch = service.replaceLines(preparer(), batch.id(), batch.version(),
            List.of(command(line(source, DriverPayrollInputLine.Category.TRIP_EARNING), null)));
        return service.validate(preparer(), batch.id(), batch.version());
    }

    private DriverPayrollInputBatch approved(String key, Source source) {
        var batch = validated(key, source);
        return service.approve(approver(), batch.id(), batch.version());
    }

    private static DriverPayrollUseCase.Create create() {
        var now = OffsetDateTime.now(ZoneOffset.UTC);
        return new DriverPayrollUseCase.Create(DriverPayrollInputBatch.Type.REGULAR, null,
            LocalDate.now(ZoneOffset.UTC).minusDays(7), LocalDate.now(ZoneOffset.UTC).plusDays(1),
            now.plusDays(1), "LKR");
    }

    private static DriverPayrollInputLine line(Source source, DriverPayrollInputLine.Category category) {
        return new DriverPayrollInputLine(UUID.randomUUID(), source.driver(), source.trip(), "TRIP",
            category, "AUTHORIZED_RATE", "source", BigDecimal.ONE, DriverPayrollInputLine.Unit.TRIP,
            BigDecimal.TEN, null, null, null, null);
    }

    private static DriverPayrollUseCase.LineCommand command(DriverPayrollInputLine line, UUID originalLineId) {
        return new DriverPayrollUseCase.LineCommand(line.id(), line.driverId(), line.tripId(), line.tripNumber(),
            line.category(), line.reasonCode(), line.description(), line.quantity(), line.unit(), line.rate(),
            line.amount(), originalLineId);
    }

    private static DriverPayrollUseCase.Context preparer() {
        return new DriverPayrollUseCase.Context(TENANT, PREPARER, "preparer", "race");
    }

    private static DriverPayrollUseCase.Context approver() {
        return new DriverPayrollUseCase.Context(TENANT, APPROVER, "approver", "race");
    }

    private int history(UUID batch, String action) {
        return jdbc.queryForObject("select count(*) from driver_payroll_input_history where tenant_id=? "
            + "and batch_id=? and action=?", Integer.class, TENANT, batch, action);
    }

    private int outbox(UUID event) {
        return jdbc.queryForObject("select count(*) from integration_outbox_event where tenant_id=? and event_id=?",
            Integer.class, TENANT, event);
    }

    private int count(String table, String column, String value) {
        if (!List.of("driver_payroll_input_batch", "driver_payroll_worker_mapping_command").contains(table)
                || !"idempotency_key".equals(column)) throw new IllegalArgumentException("unsafe test query");
        return jdbc.queryForObject("select count(*) from " + table + " where tenant_id=? and " + column + "=?",
            Integer.class, TENANT, value);
    }

    private static <T> List<Result<T>> race(Callable<T> first, Callable<T> second) throws Exception {
        var barrier = new CyclicBarrier(2);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var futures = new ArrayList<java.util.concurrent.Future<Result<T>>>();
            for (var work : List.of(first, second)) futures.add(executor.submit(() -> {
                barrier.await();
                try { return new Result<>(work.call(), null); }
                catch (Throwable failure) { return new Result<>(null, failure); }
            }));
            return List.of(futures.get(0).get(), futures.get(1).get());
        }
    }

    private record Source(UUID driver, UUID trip) {}
    private record Result<T>(T value, Throwable failure) { boolean succeeded() { return failure == null; } }
}
