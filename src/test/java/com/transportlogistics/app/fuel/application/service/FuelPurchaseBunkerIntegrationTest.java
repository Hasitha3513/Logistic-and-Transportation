package com.transportlogistics.app.fuel.application.service;

import com.transportlogistics.app.fuel.application.ports.in.FuelPurchaseUseCase;
import com.transportlogistics.app.fuel.application.ports.out.BunkerStockLedgerRepository;
import com.transportlogistics.app.fuel.application.ports.out.BunkerTankRepository;
import com.transportlogistics.app.fuel.application.ports.out.FuelStationRepository;
import com.transportlogistics.app.fuel.domain.model.*;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import static com.transportlogistics.app.support.ReferenceFixtures.locations;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class FuelPurchaseBunkerIntegrationTest {

    @Autowired private FuelPurchaseUseCase fuelPurchases;
    @Autowired private BunkerTankRepository bunkerTanks;
    @Autowired private BunkerStockLedgerRepository bunkerMovements;
    @Autowired private FuelStationRepository stations;
    @Autowired private JdbcTemplate jdbc;

    private UUID userId;
    private UUID vendorId;
    private UUID internalStationId;
    private UUID externalStationId;
    private UUID tankId;

    @BeforeEach
    void setUp() {
        var now = OffsetDateTime.parse("2026-08-18T10:00:00Z");
        userId = UUID.randomUUID();
        vendorId = UUID.randomUUID();
        internalStationId = UUID.randomUUID();
        externalStationId = UUID.randomUUID();
        tankId = UUID.randomUUID();

        jdbc.update("INSERT INTO app_user (id, username, email, password_hash, first_name, last_name, active, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                userId, "fuel.purchaser", "purchaser@example.test", "hash", "Fuel", "Purchaser", true, now, now);

        jdbc.update("INSERT INTO vendor (id, code, name, active) VALUES (?, ?, ?, ?)",
                vendorId, "VND-BULK-01", "Bulk Petroleum Supplier", true);

        var loc1 = UUID.randomUUID();
        var loc2 = UUID.randomUUID();
        locations(jdbc, loc1, loc2);

        stations.save(new FuelStation(internalStationId, "STN-BNK-BULK", "Main Depot Station", FuelStationType.INTERNAL, true, null, loc1));
        stations.save(new FuelStation(externalStationId, "STN-EXT-COMM", "Commercial Vendor Station", FuelStationType.EXTERNAL, true, vendorId, loc2));

        bunkerTanks.save(new BunkerTank(
                tankId,
                internalStationId,
                "BNK-BULK-DSL-01",
                "Bulk Diesel Tank 1",
                "DIESEL",
                new BigDecimal("10000.000"),
                new BigDecimal("2000.000"),
                new BigDecimal("500.000"),
                BunkerTankStatus.ACTIVE,
                now,
                true,
                now,
                now
        ));
    }

    @Test
    void shouldCreditBunkerStockUponInternalPurchaseReceipt() {
        var createCmd = new FuelPurchaseUseCase.Command(
                vendorId,
                internalStationId,
                "DIESEL",
                LocalDate.parse("2026-08-18"),
                "INV-BULK-001",
                LocalDate.parse("2026-08-18"),
                new BigDecimal("5000.000"),
                new BigDecimal("300.00"),
                new BigDecimal("15.00"),
                BigDecimal.ZERO,
                "LKR",
                "Bulk tank replenishment"
        );

        var draft = fuelPurchases.create(createCmd, "fuel.purchaser");
        assertEquals(FuelPurchaseStatus.DRAFT, draft.status());

        var submitted = fuelPurchases.submit(draft.id(), "fuel.purchaser");
        assertEquals(FuelPurchaseStatus.SUBMITTED, submitted.status());

        var approved = fuelPurchases.approve(submitted.id(), "Approved for delivery", "fuel.purchaser");
        assertEquals(FuelPurchaseStatus.APPROVED, approved.status());

        var receiveCmd = new FuelPurchaseUseCase.ReceiptCommand(
                new BigDecimal("4800.000"),
                OffsetDateTime.parse("2026-08-18T11:00:00Z"),
                internalStationId,
                "DN-BULK-999",
                "Received into Tank 1"
        );

        var received = fuelPurchases.receive(approved.id(), receiveCmd, "fuel.purchaser");
        assertEquals(FuelPurchaseStatus.RECEIVED, received.status());
        assertEquals(new BigDecimal("4800.000"), received.receivedQuantity());
        assertEquals(new BigDecimal("-200.0000"), received.quantityVariance());

        var updatedTank = bunkerTanks.findById(tankId).orElseThrow();
        assertEquals(0, new BigDecimal("6800.000").compareTo(updatedTank.currentStockLiters()));

        assertTrue(bunkerMovements.existsByTankIdAndReference(tankId, BunkerReferenceType.FUEL_PURCHASE, received.id()));
        var movements = bunkerMovements.findByTankIdPaged(tankId, 0, 10);
        assertEquals(1, movements.size());
        var movement = movements.get(0);
        assertEquals(BunkerMovementType.PURCHASE_RECEIPT, movement.movementType());
        assertEquals(BunkerReferenceType.FUEL_PURCHASE, movement.referenceType());
        assertEquals(received.id(), movement.referenceId());
        assertEquals(0, new BigDecimal("4800.000").compareTo(movement.quantityLiters()));
        assertEquals(0, new BigDecimal("6800.000").compareTo(movement.resultingBalanceLiters()));
    }

    @Test
    void shouldRejectInternalPurchaseReceiptWhenCapacityExceeded() {
        var tank = bunkerTanks.findById(tankId).orElseThrow();
        bunkerTanks.save(tank.withStock(new BigDecimal("8000.000")));

        var createCmd = new FuelPurchaseUseCase.Command(
                vendorId,
                internalStationId,
                "DIESEL",
                LocalDate.parse("2026-08-18"),
                "INV-BULK-002",
                LocalDate.parse("2026-08-18"),
                new BigDecimal("5000.000"),
                new BigDecimal("300.00"),
                new BigDecimal("15.00"),
                BigDecimal.ZERO,
                "LKR",
                "Overfilling attempt"
        );

        var draft = fuelPurchases.create(createCmd, "fuel.purchaser");
        var submitted = fuelPurchases.submit(draft.id(), "fuel.purchaser");
        var approved = fuelPurchases.approve(submitted.id(), "Approved", "fuel.purchaser");

        var receiveCmd = new FuelPurchaseUseCase.ReceiptCommand(
                new BigDecimal("3000.000"),
                OffsetDateTime.parse("2026-08-18T11:00:00Z"),
                internalStationId,
                "DN-BULK-998",
                "Will exceed 10,000L capacity"
        );

        var ex = assertThrows(BusinessRuleException.class, () ->
                fuelPurchases.receive(approved.id(), receiveCmd, "fuel.purchaser"));
        assertEquals("BUNKER_CAPACITY_EXCEEDED", ex.code());

        var tankAfter = bunkerTanks.findById(tankId).orElseThrow();
        assertEquals(0, new BigDecimal("8000.000").compareTo(tankAfter.currentStockLiters()));

        assertFalse(bunkerMovements.existsByTankIdAndReference(tankId, BunkerReferenceType.FUEL_PURCHASE, approved.id()));
    }

    @Test
    void shouldBypassBunkerInventoryForExternalStationReceipt() {
        var createCmd = new FuelPurchaseUseCase.Command(
                vendorId,
                externalStationId,
                "DIESEL",
                LocalDate.parse("2026-08-18"),
                "INV-EXT-001",
                LocalDate.parse("2026-08-18"),
                new BigDecimal("1000.000"),
                new BigDecimal("300.00"),
                new BigDecimal("15.00"),
                BigDecimal.ZERO,
                "LKR",
                "Commercial station direct dispensing"
        );

        var draft = fuelPurchases.create(createCmd, "fuel.purchaser");
        var submitted = fuelPurchases.submit(draft.id(), "fuel.purchaser");
        var approved = fuelPurchases.approve(submitted.id(), "Approved", "fuel.purchaser");

        var receiveCmd = new FuelPurchaseUseCase.ReceiptCommand(
                new BigDecimal("1000.000"),
                OffsetDateTime.parse("2026-08-18T11:00:00Z"),
                externalStationId,
                "DN-EXT-001",
                "Direct commercial receipt"
        );

        var received = fuelPurchases.receive(approved.id(), receiveCmd, "fuel.purchaser");
        assertEquals(FuelPurchaseStatus.RECEIVED, received.status());

        var tankAfter = bunkerTanks.findById(tankId).orElseThrow();
        assertEquals(0, new BigDecimal("2000.000").compareTo(tankAfter.currentStockLiters()));
        assertEquals(0, bunkerMovements.countByTankId(tankId));
    }
}