package com.transportlogistics.app.postgresql;

import com.transportlogistics.app.fuel.application.ports.in.BunkerTankUseCase;
import com.transportlogistics.app.fuel.application.ports.in.FuelIssueUseCase;
import com.transportlogistics.app.fuel.application.ports.in.FuelPurchaseUseCase;
import com.transportlogistics.app.fuel.application.ports.out.*;
import com.transportlogistics.app.fuel.domain.model.*;
import com.transportlogistics.app.support.PostgreSqlIntegrationTest;
import com.transportlogistics.app.support.ReferenceFixtures;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.math.BigDecimal;
import java.net.Socket;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@Tag("postgres")
@EnabledIf("postgresAvailable")
class BunkerPostgresConcurrencyIntegrationTest extends PostgreSqlIntegrationTest {

    private static boolean postgresAvailable() {
        if (POSTGRES != null && POSTGRES.isRunning()) {
            return true;
        }
        try (var conn = java.sql.DriverManager.getConnection(
                configuredJdbcUrl(), configuredDatabaseUsername(), configuredDatabasePassword())) {
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    @DynamicPropertySource
    static void configurePostgresCredentials(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.username", BunkerPostgresConcurrencyIntegrationTest::configuredDatabaseUsername);
        registry.add("spring.datasource.password", BunkerPostgresConcurrencyIntegrationTest::configuredDatabasePassword);
    }

    @Autowired private Flyway flyway;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private BunkerTankUseCase bunkerTanks;
    @Autowired private BunkerTankRepository tanks;
    @Autowired private BunkerStockLedgerRepository movements;
    @Autowired private FuelIssueUseCase fuelIssues;
    @Autowired private FuelPurchaseUseCase fuelPurchases;
    @Autowired private FuelStationRepository stations;

    private final UUID actorId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private final OffsetDateTime now = OffsetDateTime.parse("2026-08-18T10:00:00Z");

    @BeforeEach
    void resetDatabase() {
        flyway.clean();
        flyway.migrate();
        jdbc.update("INSERT INTO app_user (id, username, email, password_hash, first_name, last_name, active, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT (id) DO NOTHING",
                actorId, "admin", "admin@example.test", "hash", "Admin", "User", true, now, now);
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void concurrentFuelIssuesCannotOverdrawBunkerStock() throws Exception {
        var stationId = UUID.randomUUID();
        var locId = UUID.randomUUID();
        ReferenceFixtures.locations(jdbc, locId);
        stations.save(new FuelStation(stationId, "STN-BNK-CONC-1", "Depot 1", FuelStationType.INTERNAL, true, null, locId));

        var tankId = UUID.randomUUID();
        tanks.save(new BunkerTank(
                tankId, stationId, "BNK-CONC-01", "Diesel 1", "DIESEL",
                new BigDecimal("10000.000"), new BigDecimal("100.000"), new BigDecimal("10.000"),
                BunkerTankStatus.ACTIVE, now, true, now, now
        ));

        var vehicleId1 = UUID.randomUUID();
        var vehicleId2 = UUID.randomUUID();
        ReferenceFixtures.vehicleReference(jdbc, vehicleId1);
        ReferenceFixtures.vehicleReference(jdbc, vehicleId2);

        var draft1 = fuelIssues.create(new FuelIssueUseCase.CreateCommand(
                vehicleId1, null, null, "DIESEL", new BigDecimal("80.000"), new BigDecimal("300.00"),
                stationId, new BigDecimal("1000.000"), null, now, "Issue 1"
        ), "admin");
        var submitted1 = fuelIssues.submit(draft1.id(), "admin");
        var issue1 = fuelIssues.authorize(submitted1.id(), "Approved", "admin");

        var draft2 = fuelIssues.create(new FuelIssueUseCase.CreateCommand(
                vehicleId2, null, null, "DIESEL", new BigDecimal("50.000"), new BigDecimal("300.00"),
                stationId, new BigDecimal("1000.000"), null, now, "Issue 2"
        ), "admin");
        var submitted2 = fuelIssues.submit(draft2.id(), "admin");
        var issue2 = fuelIssues.authorize(submitted2.id(), "Approved", "admin");

        var executor = Executors.newFixedThreadPool(2);
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        var exceptions = Collections.synchronizedList(new ArrayList<Throwable>());
        var successCount = new ConcurrentLinkedQueue<UUID>();

        Runnable task1 = () -> {
            ready.countDown();
            try {
                start.await();
                fuelIssues.issue(issue1.id(), "admin");
                successCount.add(issue1.id());
            } catch (Throwable t) {
                exceptions.add(t);
            }
        };

        Runnable task2 = () -> {
            ready.countDown();
            try {
                start.await();
                fuelIssues.issue(issue2.id(), "admin");
                successCount.add(issue2.id());
            } catch (Throwable t) {
                exceptions.add(t);
            }
        };

        executor.submit(task1);
        executor.submit(task2);

        ready.await();
        start.countDown();
        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));

        // Invariant: Exactly one issue can succeed; the other must be rejected
        assertEquals(1, successCount.size(), "Exactly one fuel issue should succeed due to stock limits");
        assertEquals(1, exceptions.size(), "Exactly one fuel issue should fail with INSUFFICIENT_BUNKER_STOCK");

        var tank = tanks.findById(tankId).orElseThrow();
        assertTrue(tank.currentStockLiters().compareTo(BigDecimal.ZERO) >= 0, "Tank stock must never be negative");

        if (successCount.contains(issue1.id())) {
            assertEquals(0, new BigDecimal("20.000").compareTo(tank.currentStockLiters()));
        } else {
            assertEquals(0, new BigDecimal("50.000").compareTo(tank.currentStockLiters()));
        }

        var dbMovements = movements.findByTankIdPaged(tankId, 0, 10);
        assertEquals(1, dbMovements.size(), "Exactly one FUEL_ISSUE ledger movement should exist");
        assertEquals(BunkerMovementType.FUEL_ISSUE, dbMovements.get(0).movementType());
        assertEquals(0, tank.currentStockLiters().compareTo(dbMovements.get(0).resultingBalanceLiters()));
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void concurrentPurchaseReceiptsCannotExceedTankCapacity() throws Exception {
        var stationId = UUID.randomUUID();
        var locId = UUID.randomUUID();
        ReferenceFixtures.locations(jdbc, locId);
        stations.save(new FuelStation(stationId, "STN-BNK-CONC-2", "Depot 2", FuelStationType.INTERNAL, true, null, locId));

        var tankId = UUID.randomUUID();
        tanks.save(new BunkerTank(
                tankId, stationId, "BNK-CONC-02", "Diesel 2", "DIESEL",
                new BigDecimal("10000.000"), new BigDecimal("8000.000"), new BigDecimal("10.000"),
                BunkerTankStatus.ACTIVE, now, true, now, now
        ));

        var vendorId = UUID.randomUUID();
        jdbc.update("INSERT INTO vendor (id, code, name, active) VALUES (?, ?, ?, ?)",
                vendorId, "VND-BULK-1", "Bulk Fuel Supplier", true);

        var draft1 = fuelPurchases.create(new FuelPurchaseUseCase.Command(
                vendorId, stationId, "DIESEL", LocalDate.now(), "INV-001", LocalDate.now(),
                new BigDecimal("1500.000"), new BigDecimal("1.50"), BigDecimal.ZERO, BigDecimal.ZERO, "USD", "Purchase 1"
        ), "admin");
        var submitted1 = fuelPurchases.submit(draft1.id(), "admin");
        var purchase1 = fuelPurchases.approve(submitted1.id(), "Approved", "admin");

        var draft2 = fuelPurchases.create(new FuelPurchaseUseCase.Command(
                vendorId, stationId, "DIESEL", LocalDate.now(), "INV-002", LocalDate.now(),
                new BigDecimal("1500.000"), new BigDecimal("1.50"), BigDecimal.ZERO, BigDecimal.ZERO, "USD", "Purchase 2"
        ), "admin");
        var submitted2 = fuelPurchases.submit(draft2.id(), "admin");
        var purchase2 = fuelPurchases.approve(submitted2.id(), "Approved", "admin");

        var executor = Executors.newFixedThreadPool(2);
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        var exceptions = Collections.synchronizedList(new ArrayList<Throwable>());
        var successCount = new ConcurrentLinkedQueue<UUID>();

        executor.submit(() -> {
            ready.countDown();
            try {
                start.await();
                fuelPurchases.receive(purchase1.id(), new FuelPurchaseUseCase.ReceiptCommand(new BigDecimal("1500.000"), now, stationId, "DN-1", "Rec 1"), "admin");
                successCount.add(purchase1.id());
            } catch (Throwable t) {
                exceptions.add(t);
            }
        });

        executor.submit(() -> {
            ready.countDown();
            try {
                start.await();
                fuelPurchases.receive(purchase2.id(), new FuelPurchaseUseCase.ReceiptCommand(new BigDecimal("1500.000"), now, stationId, "DN-2", "Rec 2"), "admin");
                successCount.add(purchase2.id());
            } catch (Throwable t) {
                exceptions.add(t);
            }
        });

        ready.await();
        start.countDown();
        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));

        assertEquals(1, successCount.size(), "Exactly one purchase receipt should succeed due to capacity limits");
        assertEquals(1, exceptions.size(), "The second purchase receipt must fail with BUNKER_CAPACITY_EXCEEDED");

        var tank = tanks.findById(tankId).orElseThrow();
        assertEquals(0, new BigDecimal("9500.000").compareTo(tank.currentStockLiters()), "Final stock must be exactly 9,500 L");

        var dbMovements = movements.findByTankIdPaged(tankId, 0, 10);
        assertEquals(1, dbMovements.size(), "Exactly one PURCHASE_RECEIPT movement should be committed");
        assertEquals(BunkerMovementType.PURCHASE_RECEIPT, dbMovements.get(0).movementType());
        assertEquals(0, new BigDecimal("9500.000").compareTo(dbMovements.get(0).resultingBalanceLiters()));
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void duplicatePurchaseReceiptAttemptIsSafelyIdempotent() throws Exception {
        var stationId = UUID.randomUUID();
        var locId = UUID.randomUUID();
        ReferenceFixtures.locations(jdbc, locId);
        stations.save(new FuelStation(stationId, "STN-BNK-CONC-3", "Depot 3", FuelStationType.INTERNAL, true, null, locId));

        var tankId = UUID.randomUUID();
        tanks.save(new BunkerTank(
                tankId, stationId, "BNK-CONC-03", "Diesel 3", "DIESEL",
                new BigDecimal("10000.000"), new BigDecimal("5000.000"), new BigDecimal("10.000"),
                BunkerTankStatus.ACTIVE, now, true, now, now
        ));

        var vendorId = UUID.randomUUID();
        jdbc.update("INSERT INTO vendor (id, code, name, active) VALUES (?, ?, ?, ?)",
                vendorId, "VND-BULK-2", "Bulk Supplier", true);

        var draft = fuelPurchases.create(new FuelPurchaseUseCase.Command(
                vendorId, stationId, "DIESEL", LocalDate.now(), "INV-003", LocalDate.now(),
                new BigDecimal("1000.000"), new BigDecimal("1.50"), BigDecimal.ZERO, BigDecimal.ZERO, "USD", "Purchase 3"
        ), "admin");
        var submitted = fuelPurchases.submit(draft.id(), "admin");
        var purchase = fuelPurchases.approve(submitted.id(), "Approved", "admin");

        var executor = Executors.newFixedThreadPool(2);
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        var successCount = new ConcurrentLinkedQueue<Boolean>();

        Runnable task = () -> {
            ready.countDown();
            try {
                start.await();
                fuelPurchases.receive(purchase.id(), new FuelPurchaseUseCase.ReceiptCommand(new BigDecimal("1000.000"), now, stationId, "DN-DUP", "Concurrent receive"), "admin");
                successCount.add(true);
            } catch (Throwable t) {
                // Expected failure for the losing thread
            }
        };

        executor.submit(task);
        executor.submit(task);

        ready.await();
        start.countDown();
        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));

        assertEquals(1, successCount.size(), "Only one receive execution can succeed for the same purchase");

        var tank = tanks.findById(tankId).orElseThrow();
        assertEquals(0, new BigDecimal("6000.000").compareTo(tank.currentStockLiters()), "Stock must be credited exactly once");

        var dbMovements = movements.findByTankIdPaged(tankId, 0, 10);
        assertEquals(1, dbMovements.size(), "Exactly one ledger movement must be recorded for this purchase");
        assertEquals(purchase.id(), dbMovements.get(0).referenceId());
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void duplicateFuelIssueAttemptIsSafelySerialized() throws Exception {
        var stationId = UUID.randomUUID();
        var locId = UUID.randomUUID();
        ReferenceFixtures.locations(jdbc, locId);
        stations.save(new FuelStation(stationId, "STN-BNK-CONC-4", "Depot 4", FuelStationType.INTERNAL, true, null, locId));

        var tankId = UUID.randomUUID();
        tanks.save(new BunkerTank(
                tankId, stationId, "BNK-CONC-04", "Diesel 4", "DIESEL",
                new BigDecimal("10000.000"), new BigDecimal("1000.000"), new BigDecimal("10.000"),
                BunkerTankStatus.ACTIVE, now, true, now, now
        ));

        var vehicleId = UUID.randomUUID();
        ReferenceFixtures.vehicleReference(jdbc, vehicleId);

        var draft = fuelIssues.create(new FuelIssueUseCase.CreateCommand(
                vehicleId, null, null, "DIESEL", new BigDecimal("100.000"), new BigDecimal("300.00"),
                stationId, new BigDecimal("1000.000"), null, now, "Issue 4"
        ), "admin");
        var submitted = fuelIssues.submit(draft.id(), "admin");
        var issue = fuelIssues.authorize(submitted.id(), "Approved", "admin");

        var executor = Executors.newFixedThreadPool(2);
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        var successCount = new ConcurrentLinkedQueue<Boolean>();

        Runnable task = () -> {
            ready.countDown();
            try {
                start.await();
                fuelIssues.issue(issue.id(), "admin");
                successCount.add(true);
            } catch (Throwable t) {
                // Expected failure for the second thread due to status transition lock
            }
        };

        executor.submit(task);
        executor.submit(task);

        ready.await();
        start.countDown();
        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));

        assertEquals(1, successCount.size(), "Only one issue invocation must succeed");

        var tank = tanks.findById(tankId).orElseThrow();
        assertEquals(0, new BigDecimal("900.000").compareTo(tank.currentStockLiters()), "Stock must be deducted exactly once");

        var dbMovements = movements.findByTankIdPaged(tankId, 0, 10);
        assertEquals(1, dbMovements.size(), "Exactly one ledger movement should exist");
        assertEquals(issue.id(), dbMovements.get(0).referenceId());
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void concurrentStockAdjustmentsCannotOverdrawStock() throws Exception {
        var stationId = UUID.randomUUID();
        var locId = UUID.randomUUID();
        ReferenceFixtures.locations(jdbc, locId);
        stations.save(new FuelStation(stationId, "STN-BNK-CONC-5", "Depot 5", FuelStationType.INTERNAL, true, null, locId));

        var tankId = UUID.randomUUID();
        tanks.save(new BunkerTank(
                tankId, stationId, "BNK-CONC-05", "Diesel 5", "DIESEL",
                new BigDecimal("10000.000"), new BigDecimal("1000.000"), new BigDecimal("10.000"),
                BunkerTankStatus.ACTIVE, now, true, now, now
        ));

        var executor = Executors.newFixedThreadPool(2);
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        var successDeltas = new ConcurrentLinkedQueue<BigDecimal>();

        executor.submit(() -> {
            ready.countDown();
            try {
                start.await();
                bunkerTanks.adjustStock(tankId, new BigDecimal("-700.000"), "Audit correction A", null, "admin");
                successDeltas.add(new BigDecimal("-700.000"));
            } catch (Throwable ignored) {
            }
        });

        executor.submit(() -> {
            ready.countDown();
            try {
                start.await();
                bunkerTanks.adjustStock(tankId, new BigDecimal("-500.000"), "Audit correction B", null, "admin");
                successDeltas.add(new BigDecimal("-500.000"));
            } catch (Throwable ignored) {
            }
        });

        ready.await();
        start.countDown();
        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));

        assertEquals(1, successDeltas.size(), "Only one adjustment can succeed because -700 + -500 exceeds 1000 L");

        var tank = tanks.findById(tankId).orElseThrow();
        if (successDeltas.contains(new BigDecimal("-700.000"))) {
            assertEquals(0, new BigDecimal("300.000").compareTo(tank.currentStockLiters()));
        } else {
            assertEquals(0, new BigDecimal("500.000").compareTo(tank.currentStockLiters()));
        }

        var dbMovements = movements.findByTankIdPaged(tankId, 0, 10);
        assertEquals(1, dbMovements.size());
        assertEquals(BunkerMovementType.ADJUSTMENT_OUT, dbMovements.get(0).movementType());
        assertEquals(0, tank.currentStockLiters().compareTo(dbMovements.get(0).resultingBalanceLiters()));
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void concurrentReceiptVsIssueSerializesConsistently() throws Exception {
        var stationId = UUID.randomUUID();
        var locId = UUID.randomUUID();
        ReferenceFixtures.locations(jdbc, locId);
        stations.save(new FuelStation(stationId, "STN-BNK-CONC-6", "Depot 6", FuelStationType.INTERNAL, true, null, locId));

        var tankId = UUID.randomUUID();
        tanks.save(new BunkerTank(
                tankId, stationId, "BNK-CONC-06", "Diesel 6", "DIESEL",
                new BigDecimal("10000.000"), new BigDecimal("1000.000"), new BigDecimal("10.000"),
                BunkerTankStatus.ACTIVE, now, true, now, now
        ));

        var vendorId = UUID.randomUUID();
        jdbc.update("INSERT INTO vendor (id, code, name, active) VALUES (?, ?, ?, ?)",
                vendorId, "VND-BULK-3", "Bulk Supplier", true);

        var draft = fuelPurchases.create(new FuelPurchaseUseCase.Command(
                vendorId, stationId, "DIESEL", LocalDate.now(), "INV-004", LocalDate.now(),
                new BigDecimal("1000.000"), new BigDecimal("1.50"), BigDecimal.ZERO, BigDecimal.ZERO, "USD", "Purchase 4"
        ), "admin");
        var submitted = fuelPurchases.submit(draft.id(), "admin");
        var purchase = fuelPurchases.approve(submitted.id(), "Approved", "admin");

        var vehicleId = UUID.randomUUID();
        ReferenceFixtures.vehicleReference(jdbc, vehicleId);

        var draftIssue = fuelIssues.create(new FuelIssueUseCase.CreateCommand(
                vehicleId, null, null, "DIESEL", new BigDecimal("800.000"), new BigDecimal("300.00"),
                stationId, new BigDecimal("1000.000"), null, now, "Issue 6"
        ), "admin");
        var submittedIssue = fuelIssues.submit(draftIssue.id(), "admin");
        var issue = fuelIssues.authorize(submittedIssue.id(), "Approved", "admin");

        var executor = Executors.newFixedThreadPool(2);
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);

        executor.submit(() -> {
            ready.countDown();
            try {
                start.await();
                fuelPurchases.receive(purchase.id(), new FuelPurchaseUseCase.ReceiptCommand(new BigDecimal("1000.000"), now, stationId, "DN-RCV", "Purchase receipt"), "admin");
            } catch (Throwable ignored) {
            }
        });

        executor.submit(() -> {
            ready.countDown();
            try {
                start.await();
                fuelIssues.issue(issue.id(), "admin");
            } catch (Throwable ignored) {
            }
        });

        ready.await();
        start.countDown();
        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));

        var tank = tanks.findById(tankId).orElseThrow();
        var dbMovements = movements.findByTankIdPaged(tankId, 0, 10);

        assertTrue(tank.currentStockLiters().compareTo(BigDecimal.ZERO) >= 0, "Tank stock must never be negative");
        assertTrue(tank.currentStockLiters().compareTo(new BigDecimal("10000.000")) <= 0, "Tank capacity must not be exceeded");

        // Stock conservation invariant: 1000 + 1000 - 800 = 1200 L
        assertEquals(0, new BigDecimal("1200.000").compareTo(tank.currentStockLiters()));
        assertEquals(2, dbMovements.size(), "Both operations should have committed ledger entries");
        assertEquals(0, tank.currentStockLiters().compareTo(dbMovements.get(0).resultingBalanceLiters()), "Latest ledger balance must match tank stock");
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void interTankTransfersInOppositeDirectionsDoNotDeadlock() throws Exception {
        var stationId = UUID.randomUUID();
        var locId = UUID.randomUUID();
        ReferenceFixtures.locations(jdbc, locId);
        stations.save(new FuelStation(stationId, "STN-BNK-CONC-7", "Depot 7", FuelStationType.INTERNAL, true, null, locId));

        var tankAId = UUID.fromString("10000000-0000-0000-0000-000000000001");
        var tankBId = UUID.fromString("20000000-0000-0000-0000-000000000002");

        tanks.save(new BunkerTank(
                tankAId, stationId, "BNK-A", "Tank A", "DIESEL",
                new BigDecimal("10000.000"), new BigDecimal("5000.000"), new BigDecimal("10.000"),
                BunkerTankStatus.ACTIVE, now, true, now, now
        ));

        tanks.save(new BunkerTank(
                tankBId, stationId, "BNK-B", "Tank B", "DIESEL",
                new BigDecimal("10000.000"), new BigDecimal("5000.000"), new BigDecimal("10.000"),
                BunkerTankStatus.ACTIVE, now, true, now, now
        ));

        var executor = Executors.newFixedThreadPool(2);
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        var successCount = new ConcurrentLinkedQueue<Boolean>();

        // Transfer 1: A -> B (1000 L)
        executor.submit(() -> {
            ready.countDown();
            try {
                start.await();
                bunkerTanks.transfer(new BunkerTankUseCase.TransferCommand(tankAId, tankBId, new BigDecimal("1000.000"), "A to B"), "admin");
                successCount.add(true);
            } catch (Throwable ignored) {
            }
        });

        // Transfer 2: B -> A (1000 L)
        executor.submit(() -> {
            ready.countDown();
            try {
                start.await();
                bunkerTanks.transfer(new BunkerTankUseCase.TransferCommand(tankBId, tankAId, new BigDecimal("1000.000"), "B to A"), "admin");
                successCount.add(true);
            } catch (Throwable ignored) {
            }
        });

        ready.await();
        start.countDown();
        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS), "Transfers must not deadlock");

        var tankA = tanks.findById(tankAId).orElseThrow();
        var tankB = tanks.findById(tankBId).orElseThrow();

        // Stock conservation invariant: Total fuel across both tanks must remain 10,000 L
        assertEquals(0, new BigDecimal("10000.000").compareTo(tankA.currentStockLiters().add(tankB.currentStockLiters())));

        var movementsA = movements.findByTankIdPaged(tankAId, 0, 10);
        var movementsB = movements.findByTankIdPaged(tankBId, 0, 10);

        assertEquals(2, movementsA.size());
        assertEquals(2, movementsB.size());
        assertEquals(2, successCount.size(), "Both transfers serialized and succeeded without deadlock");
    }
}
