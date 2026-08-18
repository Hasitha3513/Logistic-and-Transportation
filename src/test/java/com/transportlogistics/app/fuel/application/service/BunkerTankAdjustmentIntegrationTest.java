package com.transportlogistics.app.fuel.application.service;

import com.transportlogistics.app.fuel.application.ports.in.BunkerTankUseCase;
import com.transportlogistics.app.fuel.application.ports.out.*;
import com.transportlogistics.app.fuel.domain.model.*;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static com.transportlogistics.app.support.ReferenceFixtures.locations;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class BunkerTankAdjustmentIntegrationTest {

    @Autowired private BunkerTankUseCase bunkerTanks;
    @Autowired private BunkerTankRepository tanks;
    @Autowired private BunkerStockLedgerRepository movements;
    @Autowired private DipReadingRepository dipReadings;
    @Autowired private StockAdjustmentRepository adjustments;
    @Autowired private FuelStationRepository stations;
    @Autowired private JdbcTemplate jdbc;

    private UUID userId;
    private UUID stationId;
    private UUID tank1Id;
    private UUID tank2Id;

    @BeforeEach
    void setUp() {
        var now = OffsetDateTime.parse("2026-08-18T10:00:00Z");
        userId = UUID.randomUUID();
        stationId = UUID.randomUUID();
        tank1Id = UUID.randomUUID();
        tank2Id = UUID.randomUUID();

        jdbc.update("INSERT INTO app_user (id, username, email, password_hash, first_name, last_name, active, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                userId, "bunker.manager", "manager@example.test", "hash", "Bunker", "Manager", true, now, now);

        var loc1 = UUID.randomUUID();
        locations(jdbc, loc1);

        stations.save(new FuelStation(stationId, "STN-BNK-ADJ", "Central Depot", FuelStationType.INTERNAL, true, null, loc1));

        tanks.save(new BunkerTank(
                tank1Id,
                stationId,
                "BNK-ADJ-DSL-01",
                "Central Diesel Tank 1",
                "DIESEL",
                new BigDecimal("10000.000"),
                new BigDecimal("5500.000"),
                new BigDecimal("1000.000"),
                BunkerTankStatus.ACTIVE,
                now,
                true,
                now,
                now
        ));

        tanks.save(new BunkerTank(
                tank2Id,
                stationId,
                "BNK-ADJ-DSL-02",
                "Central Diesel Tank 2",
                "DIESEL",
                new BigDecimal("10000.000"),
                new BigDecimal("2000.000"),
                new BigDecimal("1000.000"),
                BunkerTankStatus.ACTIVE,
                now,
                true,
                now,
                now
        ));
    }

    @Test
    void shouldRecordDipReadingWithoutChangingBookStock() {
        var reading = bunkerTanks.recordDipReading(tank1Id, new BigDecimal("5400.000"), "Daily physical sounding", "bunker.manager");

        assertNotNull(reading.id());
        assertEquals(tank1Id, reading.tankId());
        assertEquals(0, new BigDecimal("5400.000").compareTo(reading.physicalQuantityLiters()));
        assertEquals(0, new BigDecimal("5500.000").compareTo(reading.bookQuantityAtMeasurement()));
        assertEquals(0, new BigDecimal("-100.000").compareTo(reading.varianceQuantityLiters()));

        // Invariant: Dip reading alone does NOT modify book stock
        var tankAfter = tanks.findById(tank1Id).orElseThrow();
        assertEquals(0, new BigDecimal("5500.000").compareTo(tankAfter.currentStockLiters()));

        var savedDip = dipReadings.findByTankId(tank1Id);
        assertFalse(savedDip.isEmpty());
        assertEquals(1, savedDip.size());
    }

    @Test
    void shouldAdjustStockNegativeDeltaAndCreateAdjustmentOutMovement() {
        var dip = bunkerTanks.recordDipReading(tank1Id, new BigDecimal("5400.000"), "Physical dip", "bunker.manager");

        var adj = bunkerTanks.adjustStock(tank1Id, new BigDecimal("-100.000"), "Reconciling evaporation variance", dip.id(), "bunker.manager");

        assertNotNull(adj.id());
        assertEquals(tank1Id, adj.tankId());
        assertEquals(0, new BigDecimal("-100.000").compareTo(adj.quantityDeltaLiters()));
        assertEquals(dip.id(), adj.sourceDipReadingId());

        var tankAfter = tanks.findById(tank1Id).orElseThrow();
        assertEquals(0, new BigDecimal("5400.000").compareTo(tankAfter.currentStockLiters()));

        var tankMovements = movements.findByTankIdPaged(tank1Id, 0, 10);
        assertEquals(1, tankMovements.size());
        var movement = tankMovements.get(0);
        assertEquals(BunkerMovementType.ADJUSTMENT_OUT, movement.movementType());
        assertEquals(0, new BigDecimal("100.000").compareTo(movement.quantityLiters()));
        assertEquals(0, new BigDecimal("5400.000").compareTo(movement.resultingBalanceLiters()));
        assertEquals(BunkerReferenceType.MANUAL_ADJUSTMENT, movement.referenceType());
        assertEquals(adj.id(), movement.referenceId());
    }

    @Test
    void shouldAdjustStockPositiveDeltaAndCreateAdjustmentInMovement() {
        var adj = bunkerTanks.adjustStock(tank1Id, new BigDecimal("300.000"), "Found unrecorded receipt surplus", null, "bunker.manager");

        assertNotNull(adj.id());
        assertEquals(0, new BigDecimal("300.000").compareTo(adj.quantityDeltaLiters()));

        var tankAfter = tanks.findById(tank1Id).orElseThrow();
        assertEquals(0, new BigDecimal("5800.000").compareTo(tankAfter.currentStockLiters()));

        var tankMovements = movements.findByTankIdPaged(tank1Id, 0, 10);
        assertEquals(1, tankMovements.size());
        var movement = tankMovements.get(0);
        assertEquals(BunkerMovementType.ADJUSTMENT_IN, movement.movementType());
        assertEquals(0, new BigDecimal("300.000").compareTo(movement.quantityLiters()));
        assertEquals(0, new BigDecimal("5800.000").compareTo(movement.resultingBalanceLiters()));
    }

    @Test
    void shouldRejectAdjustmentResultingInNegativeStock() {
        var ex = assertThrows(BusinessRuleException.class, () ->
                bunkerTanks.adjustStock(tank1Id, new BigDecimal("-6000.000"), "Massive deduction", null, "bunker.manager"));
        assertEquals("INSUFFICIENT_BUNKER_STOCK", ex.code());

        var tankAfter = tanks.findById(tank1Id).orElseThrow();
        assertEquals(0, new BigDecimal("5500.000").compareTo(tankAfter.currentStockLiters()));
        assertEquals(0, movements.countByTankId(tank1Id));
    }

    @Test
    void shouldRejectAdjustmentExceedingCapacity() {
        var ex = assertThrows(BusinessRuleException.class, () ->
                bunkerTanks.adjustStock(tank1Id, new BigDecimal("5000.000"), "Overfill attempt", null, "bunker.manager"));
        assertEquals("BUNKER_CAPACITY_EXCEEDED", ex.code());

        var tankAfter = tanks.findById(tank1Id).orElseThrow();
        assertEquals(0, new BigDecimal("5500.000").compareTo(tankAfter.currentStockLiters()));
        assertEquals(0, movements.countByTankId(tank1Id));
    }

    @Test
    void shouldTransferFuelBetweenTanksAtomically() {
        var transferCmd = new BunkerTankUseCase.TransferCommand(
                tank1Id,
                tank2Id,
                new BigDecimal("1500.000"),
                "Routine inventory rebalance"
        );

        bunkerTanks.transfer(transferCmd, "bunker.manager");

        var sourceAfter = tanks.findById(tank1Id).orElseThrow();
        var destAfter = tanks.findById(tank2Id).orElseThrow();

        assertEquals(0, new BigDecimal("4000.000").compareTo(sourceAfter.currentStockLiters()));
        assertEquals(0, new BigDecimal("3500.000").compareTo(destAfter.currentStockLiters()));

        var sourceMovements = movements.findByTankIdPaged(tank1Id, 0, 10);
        assertEquals(1, sourceMovements.size());
        var outMovement = sourceMovements.get(0);
        assertEquals(BunkerMovementType.TRANSFER_OUT, outMovement.movementType());
        assertEquals(0, new BigDecimal("1500.000").compareTo(outMovement.quantityLiters()));
        assertEquals(0, new BigDecimal("4000.000").compareTo(outMovement.resultingBalanceLiters()));

        var destMovements = movements.findByTankIdPaged(tank2Id, 0, 10);
        assertEquals(1, destMovements.size());
        var inMovement = destMovements.get(0);
        assertEquals(BunkerMovementType.TRANSFER_IN, inMovement.movementType());
        assertEquals(0, new BigDecimal("1500.000").compareTo(inMovement.quantityLiters()));
        assertEquals(0, new BigDecimal("3500.000").compareTo(inMovement.resultingBalanceLiters()));
    }
}