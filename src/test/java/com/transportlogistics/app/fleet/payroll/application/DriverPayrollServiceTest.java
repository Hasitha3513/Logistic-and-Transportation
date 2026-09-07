package com.transportlogistics.app.fleet.payroll.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transportlogistics.app.fleet.DriverLookup;
import com.transportlogistics.app.fleet.DriverPayrollIntegrationPort;
import com.transportlogistics.app.fleet.DriverPayrollSourcePort;
import com.transportlogistics.app.fleet.FleetDriverSummary;
import com.transportlogistics.app.fleet.payroll.domain.DriverPayrollInputBatch;
import com.transportlogistics.app.fleet.payroll.domain.DriverPayrollInputLine;
import com.transportlogistics.app.fleet.payroll.domain.DriverPayrollWorkerMapping;
import com.transportlogistics.app.fleet.payroll.ports.inbound.DriverPayrollUseCase;
import com.transportlogistics.app.fleet.payroll.ports.outbound.DriverPayrollStore;
import com.transportlogistics.app.fleet.payroll.ports.outbound.DriverPayrollTransaction;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.shared.domain.ConflictException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DriverPayrollServiceTest {
    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID ACTOR = UUID.randomUUID();
    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-09-07T08:00:00Z");
    private final DriverPayrollStore store = mock(DriverPayrollStore.class);
    private final DriverPayrollSourcePort trips = mock(DriverPayrollSourcePort.class);
    private final DriverLookup drivers = mock(DriverLookup.class);
    private final DriverPayrollIntegrationPort integrations = mock(DriverPayrollIntegrationPort.class);
    private DriverPayrollService service;

    @BeforeEach
    void setUp() {
        DriverPayrollTransaction transaction = new DriverPayrollTransaction() {
            @Override public <T> T execute(Supplier<T> work) { return work.get(); }
        };
        service = new DriverPayrollService(store, trips, drivers, integrations, transaction,
            new ObjectMapper(), Clock.fixed(Instant.parse("2026-09-07T08:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void mappingCommandReplaysOriginalResultAndRejectsDifferentRequest() {
        UUID driverId = UUID.randomUUID();
        var context = context();
        var command = new DriverPayrollUseCase.MappingCommand("PAYROLL", "WORKER-1", true, 0);
        var persisted = new DriverPayrollWorkerMapping(UUID.randomUUID(), TENANT, driverId, "PAYROLL", "WORKER-1",
            true, 0, ACTOR, NOW, NOW);
        var commandResult = new AtomicReference<DriverPayrollStore.MappingCommandResult>();
        when(store.mappingCommand(TENANT, "mapping-key")).thenAnswer(invocation -> Optional.ofNullable(commandResult.get()));
        when(drivers.findDriver(driverId)).thenReturn(Optional.of(
            new FleetDriverSummary(driverId, "DRV-1", "Safe", "Driver", "AVAILABLE", true)));
        when(store.mapping(TENANT, driverId)).thenReturn(Optional.empty());
        when(store.saveMapping(any(), anyLong())).thenReturn(persisted);
        when(store.saveMappingCommand(any(), any(), any(), any(), any(), any(), any()))
            .thenAnswer(invocation -> {
                var result = new DriverPayrollStore.MappingCommandResult(invocation.getArgument(3), persisted);
                commandResult.set(result);
                return result;
            });

        var first = service.mapWorker(context, driverId, command, "mapping-key");
        var replay = service.mapWorker(context, driverId, command, "mapping-key");

        assertThat(replay).isEqualTo(first);
        verify(store).saveMapping(any(), anyLong());
        assertThatThrownBy(() -> service.mapWorker(context, driverId,
            new DriverPayrollUseCase.MappingCommand("PAYROLL", "WORKER-2", true, 0), "mapping-key"))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("DRIVER_PAYROLL_IDEMPOTENCY_CONFLICT");
    }

    @Test
    void validationRejectsCanonicalPayloadOver32KiBBeforePersistence() {
        UUID batchId = UUID.randomUUID();
        var lines = new ArrayList<DriverPayrollInputLine>();
        for (int index = 0; index < 150; index++) {
            UUID driverId = new UUID(0, index + 1L);
            UUID tripId = new UUID(1, index + 1L);
            lines.add(new DriverPayrollInputLine(UUID.randomUUID(), driverId, tripId, "TRIP-" + index,
                DriverPayrollInputLine.Category.TRIP_EARNING, "AUTHORIZED_RATE", "x".repeat(500),
                BigDecimal.ONE, DriverPayrollInputLine.Unit.TRIP, BigDecimal.ONE, null, null, null, null));
        }
        var batch = batch(batchId, DriverPayrollInputBatch.Lifecycle.DRAFT, lines, UUID.randomUUID(), null);
        when(store.find(TENANT, batchId)).thenReturn(Optional.of(batch));
        when(integrations.activePayrollConfiguration(TENANT)).thenReturn(Optional.of(UUID.randomUUID()));
        when(drivers.findDriver(any())).thenAnswer(invocation -> Optional.of(new FleetDriverSummary(
            invocation.getArgument(0), "DRV", "Safe", "Driver", "AVAILABLE", true)));
        when(trips.trip(any(), any(), any(), any())).thenAnswer(invocation -> Optional.of(
            new DriverPayrollSourcePort.TripFact(invocation.getArgument(1), "TRIP", "COMPLETED",
                invocation.getArgument(0), NOW.minusDays(1))));
        when(store.mapping(any(), any())).thenAnswer(invocation -> Optional.of(new DriverPayrollWorkerMapping(
            UUID.randomUUID(), TENANT, invocation.getArgument(1), "PAYROLL", "WORKER", true, 0,
            ACTOR, NOW, NOW)));

        assertThatThrownBy(() -> service.validate(context(), batchId, 0))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("DRIVER_PAYROLL_VALIDATION_FAILED");
        verify(store, never()).update(any(), anyLong());
    }

    @Test
    void successfulDeliveryTransitionsOnceAndPreservesReleasedContent() {
        UUID eventId = UUID.randomUUID();
        UUID batchId = UUID.randomUUID();
        var line = new DriverPayrollInputLine(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "TRIP-1",
            DriverPayrollInputLine.Category.TRIP_EARNING, "TRIP_RATE", "source", BigDecimal.ONE,
            DriverPayrollInputLine.Unit.TRIP, BigDecimal.TEN, null, null, "a".repeat(64), "WORKER-1");
        var requested = batch(batchId, DriverPayrollInputBatch.Lifecycle.EXPORT_REQUESTED,
            java.util.List.of(line), ACTOR, eventId);
        var current = new AtomicReference<>(requested);
        when(store.findByExportEvent(TENANT, eventId)).thenAnswer(invocation -> Optional.of(current.get()));
        when(store.update(any(), anyLong())).thenAnswer(invocation -> {
            var value = (DriverPayrollInputBatch) invocation.getArgument(0);
            current.set(value);
            return value;
        });

        service.fileDelivered(TENANT, eventId, "b".repeat(64), "payroll.json", NOW);
        service.fileDelivered(TENANT, eventId, "b".repeat(64), "payroll.json", NOW);

        assertThat(current.get().lifecycle()).isEqualTo(DriverPayrollInputBatch.Lifecycle.EXPORTED);
        assertThat(current.get().lines()).containsExactly(line);
        verify(store).update(any(), anyLong());
    }

    private static DriverPayrollUseCase.Context context() {
        return new DriverPayrollUseCase.Context(TENANT, ACTOR, "actor", "correlation");
    }

    private static DriverPayrollInputBatch batch(UUID id, DriverPayrollInputBatch.Lifecycle lifecycle,
                                                  java.util.List<DriverPayrollInputLine> lines,
                                                  UUID preparedBy, UUID eventId) {
        var totals = DriverPayrollInputBatch.Totals.calculate(lines);
        return new DriverPayrollInputBatch(id, TENANT, DriverPayrollInputBatch.Type.REGULAR, null,
            LocalDate.of(2026, 9, 1), LocalDate.of(2026, 10, 1), NOW, "LKR", lifecycle, lines, totals,
            preparedBy, UUID.randomUUID(), NOW, UUID.randomUUID(), eventId, "c".repeat(64), 0, NOW, NOW);
    }
}
