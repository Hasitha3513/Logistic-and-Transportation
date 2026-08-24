package com.transportlogistics.app.fuel.application.service;

import com.transportlogistics.app.fuel.application.ports.in.BunkerTankUseCase;
import com.transportlogistics.app.fuel.application.ports.out.*;
import com.transportlogistics.app.fuel.domain.model.*;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BunkerTankServiceTest {

    @Mock private BunkerTankRepository tanks;
    @Mock private BunkerStockLedgerRepository movements;
    @Mock private DipReadingRepository dipReadings;
    @Mock private StockAdjustmentRepository adjustments;
    @Mock private FuelStationRepository stations;
    @Mock private FuelActorPort actors;
    @Mock private FuelTransaction transactions;

    private Clock clock;
    private BunkerTankService service;
    private UUID actorId;
    private FuelStation internalStation;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(Instant.parse("2026-08-18T10:00:00Z"), ZoneOffset.UTC);
        service = new BunkerTankService(tanks, movements, dipReadings, adjustments, stations, actors, transactions, clock);
        actorId = UUID.randomUUID();
        internalStation = new FuelStation(UUID.randomUUID(), "FUEL-INTERNAL", "Internal Depot", FuelStationType.INTERNAL, true, null, UUID.randomUUID());

        when(transactions.execute(any())).thenAnswer(invocation -> {
            Supplier<?> supplier = invocation.getArgument(0);
            return supplier.get();
        });
        lenient().when(actors.find(any())).thenReturn(Optional.of(new FuelActorPort.Actor(actorId, "admin")));
    }

    @Test
    void shouldCreateTankWithOpeningBalance() {
        when(stations.findById(internalStation.id())).thenReturn(Optional.of(internalStation));
        when(tanks.findByTankCode("BNK-01")).thenReturn(Optional.empty());
        when(tanks.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var cmd = new BunkerTankUseCase.CreateTankCommand(
                internalStation.id(), "BNK-01", "Depot Diesel 1", "DIESEL",
                new BigDecimal("10000.000"), new BigDecimal("1000.000"), new BigDecimal("2000.000"), null
        );

        var created = service.create(cmd, "admin");

        assertThat(created).isNotNull();
        assertThat(created.tankCode()).isEqualTo("BNK-01");
        assertThat(created.currentStockLiters()).isEqualByComparingTo("2000.000");

        verify(movements).save(argThat(m -> m.movementType() == BunkerMovementType.OPENING_BALANCE &&
                m.quantityLiters().compareTo(new BigDecimal("2000.000")) == 0));
    }

    @Test
    void shouldRecordDipReadingAndCalculateVariance() {
        var tank = new BunkerTank(
                UUID.randomUUID(), internalStation.id(), "BNK-01", "Depot Diesel 1", "DIESEL",
                new BigDecimal("10000.000"), new BigDecimal("2000.000"), new BigDecimal("1000.000"),
                BunkerTankStatus.ACTIVE, OffsetDateTime.now(clock), true, OffsetDateTime.now(clock), OffsetDateTime.now(clock)
        );
        when(tanks.findById(tank.id())).thenReturn(Optional.of(tank));
        when(dipReadings.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var reading = service.recordDipReading(tank.id(), new BigDecimal("1950.000"), "Weekly manual dip", "admin");

        assertThat(reading.physicalQuantityLiters()).isEqualByComparingTo("1950.000");
        assertThat(reading.bookQuantityAtMeasurement()).isEqualByComparingTo("2000.000");
        assertThat(reading.varianceQuantityLiters()).isEqualByComparingTo("-50.000");
    }

    @Test
    void shouldAdjustStockWithLedgerEntry() {
        var tank = new BunkerTank(
                UUID.randomUUID(), internalStation.id(), "BNK-01", "Depot Diesel 1", "DIESEL",
                new BigDecimal("10000.000"), new BigDecimal("2000.000"), new BigDecimal("1000.000"),
                BunkerTankStatus.ACTIVE, OffsetDateTime.now(clock), true, OffsetDateTime.now(clock), OffsetDateTime.now(clock)
        );
        when(tanks.findByIdForUpdate(tank.id())).thenReturn(Optional.of(tank));
        when(tanks.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(adjustments.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var adj = service.adjustStock(tank.id(), new BigDecimal("-50.000"), "Evaporation loss", null, "admin");

        assertThat(adj.quantityDeltaLiters()).isEqualByComparingTo("-50.000");
        verify(movements).save(argThat(m -> m.movementType() == BunkerMovementType.ADJUSTMENT_OUT &&
                m.quantityLiters().compareTo(new BigDecimal("50.000")) == 0 &&
                m.resultingBalanceLiters().compareTo(new BigDecimal("1950.000")) == 0));
    }

    @Test
    void shouldRecordDipReadingWithPositiveVariance() {
        var tank = new BunkerTank(
                UUID.randomUUID(), internalStation.id(), "BNK-01", "Depot Diesel 1", "DIESEL",
                new BigDecimal("10000.000"), new BigDecimal("5500.000"), new BigDecimal("1000.000"),
                BunkerTankStatus.ACTIVE, OffsetDateTime.now(clock), true, OffsetDateTime.now(clock), OffsetDateTime.now(clock)
        );
        when(tanks.findById(tank.id())).thenReturn(Optional.of(tank));
        when(dipReadings.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var reading = service.recordDipReading(tank.id(), new BigDecimal("5600.000"), "Calibration check", "admin");

        assertThat(reading.physicalQuantityLiters()).isEqualByComparingTo("5600.000");
        assertThat(reading.bookQuantityAtMeasurement()).isEqualByComparingTo("5500.000");
        assertThat(reading.varianceQuantityLiters()).isEqualByComparingTo("100.000");
        verify(tanks, never()).save(any());
    }

    @Test
    void shouldRejectDipReadingWithNegativeQuantity() {
        var tank = new BunkerTank(
                UUID.randomUUID(), internalStation.id(), "BNK-01", "Depot Diesel 1", "DIESEL",
                new BigDecimal("10000.000"), new BigDecimal("5500.000"), new BigDecimal("1000.000"),
                BunkerTankStatus.ACTIVE, OffsetDateTime.now(clock), true, OffsetDateTime.now(clock), OffsetDateTime.now(clock)
        );
        when(tanks.findById(tank.id())).thenReturn(Optional.of(tank));

        assertThatThrownBy(() -> service.recordDipReading(tank.id(), new BigDecimal("-10.000"), "Negative dip", "admin"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Physical dip quantity must be non-negative");
    }

    @Test
    void shouldAdjustStockPositiveDeltaWithAdjustmentIn() {
        var tank = new BunkerTank(
                UUID.randomUUID(), internalStation.id(), "BNK-01", "Depot Diesel 1", "DIESEL",
                new BigDecimal("10000.000"), new BigDecimal("5500.000"), new BigDecimal("1000.000"),
                BunkerTankStatus.ACTIVE, OffsetDateTime.now(clock), true, OffsetDateTime.now(clock), OffsetDateTime.now(clock)
        );
        when(tanks.findByIdForUpdate(tank.id())).thenReturn(Optional.of(tank));
        when(tanks.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(adjustments.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var adj = service.adjustStock(tank.id(), new BigDecimal("100.000"), "Found surplus", null, "admin");

        assertThat(adj.quantityDeltaLiters()).isEqualByComparingTo("100.000");
        verify(tanks).save(argThat(t -> t.currentStockLiters().compareTo(new BigDecimal("5600.000")) == 0));
        verify(movements).save(argThat(m -> m.movementType() == BunkerMovementType.ADJUSTMENT_IN &&
                m.quantityLiters().compareTo(new BigDecimal("100.000")) == 0 &&
                m.resultingBalanceLiters().compareTo(new BigDecimal("5600.000")) == 0));
    }

    @Test
    void shouldRejectAdjustmentResultingInNegativeStock() {
        var tank = new BunkerTank(
                UUID.randomUUID(), internalStation.id(), "BNK-01", "Depot Diesel 1", "DIESEL",
                new BigDecimal("10000.000"), new BigDecimal("500.000"), new BigDecimal("1000.000"),
                BunkerTankStatus.ACTIVE, OffsetDateTime.now(clock), true, OffsetDateTime.now(clock), OffsetDateTime.now(clock)
        );
        when(tanks.findByIdForUpdate(tank.id())).thenReturn(Optional.of(tank));

        assertThatThrownBy(() -> service.adjustStock(tank.id(), new BigDecimal("-600.000"), "Excessive deduction", null, "admin"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Adjustment would result in negative stock");
        verify(movements, never()).save(any());
    }

    @Test
    void shouldRejectAdjustmentExceedingCapacity() {
        var tank = new BunkerTank(
                UUID.randomUUID(), internalStation.id(), "BNK-01", "Depot Diesel 1", "DIESEL",
                new BigDecimal("10000.000"), new BigDecimal("9500.000"), new BigDecimal("1000.000"),
                BunkerTankStatus.ACTIVE, OffsetDateTime.now(clock), true, OffsetDateTime.now(clock), OffsetDateTime.now(clock)
        );
        when(tanks.findByIdForUpdate(tank.id())).thenReturn(Optional.of(tank));

        assertThatThrownBy(() -> service.adjustStock(tank.id(), new BigDecimal("600.000"), "Overflow adjustment", null, "admin"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Adjustment would exceed tank capacity");
        verify(movements, never()).save(any());
    }

    @Test
    void shouldRejectAdjustmentWithZeroDelta() {
        assertThatThrownBy(() -> service.adjustStock(UUID.randomUUID(), BigDecimal.ZERO, "Zero adjustment", null, "admin"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Adjustment delta cannot be zero");
    }

    @Test
    void shouldRejectAdjustmentWithBlankReason() {
        assertThatThrownBy(() -> service.adjustStock(UUID.randomUUID(), new BigDecimal("50.000"), "  ", null, "admin"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Adjustment reason is required");
    }

    @Test
    void shouldPerformInterTankTransfer() {
        UUID sourceId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID destId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        var source = new BunkerTank(
                sourceId, internalStation.id(), "BNK-01", "Source Tank", "DIESEL",
                new BigDecimal("10000.000"), new BigDecimal("5000.000"), new BigDecimal("1000.000"),
                BunkerTankStatus.ACTIVE, OffsetDateTime.now(clock), true, OffsetDateTime.now(clock), OffsetDateTime.now(clock)
        );
        var dest = new BunkerTank(
                destId, internalStation.id(), "BNK-02", "Dest Tank", "DIESEL",
                new BigDecimal("10000.000"), new BigDecimal("2000.000"), new BigDecimal("1000.000"),
                BunkerTankStatus.ACTIVE, OffsetDateTime.now(clock), true, OffsetDateTime.now(clock), OffsetDateTime.now(clock)
        );

        when(tanks.findByIdForUpdate(sourceId)).thenReturn(Optional.of(source));
        when(tanks.findByIdForUpdate(destId)).thenReturn(Optional.of(dest));
        when(tanks.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var cmd = new BunkerTankUseCase.TransferCommand(sourceId, destId, new BigDecimal("1500.000"), "Balancing inventory");
        service.transfer(cmd, "admin");

        verify(tanks).save(argThat(t -> t.id().equals(sourceId) && t.currentStockLiters().compareTo(new BigDecimal("3500.000")) == 0));
        verify(tanks).save(argThat(t -> t.id().equals(destId) && t.currentStockLiters().compareTo(new BigDecimal("3500.000")) == 0));
        verify(movements).save(argThat(m -> m.tankId().equals(sourceId) && m.movementType() == BunkerMovementType.TRANSFER_OUT && m.quantityLiters().compareTo(new BigDecimal("1500.000")) == 0));
        verify(movements).save(argThat(m -> m.tankId().equals(destId) && m.movementType() == BunkerMovementType.TRANSFER_IN && m.quantityLiters().compareTo(new BigDecimal("1500.000")) == 0));
    }
}
