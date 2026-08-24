package com.transportlogistics.app.fuel.application.service;

import com.transportlogistics.app.fleet.vehiclemaster.ports.outbound.VehicleRepository;
import com.transportlogistics.app.fleet.vehiclemaster.domain.model.Vehicle;
import com.transportlogistics.app.fuel.application.ports.in.FuelIssueUseCase;
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
import java.time.OffsetDateTime;
import java.util.UUID;

import static com.transportlogistics.app.support.ReferenceFixtures.locations;
import static com.transportlogistics.app.support.ReferenceFixtures.vehicleHierarchy;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class FuelIssueBunkerIntegrationTest {

    @Autowired private FuelIssueUseCase fuelIssues;
    @Autowired private BunkerTankRepository bunkerTanks;
    @Autowired private BunkerStockLedgerRepository bunkerMovements;
    @Autowired private FuelStationRepository stations;
    @Autowired private VehicleRepository vehicles;
    @Autowired private JdbcTemplate jdbc;

    private UUID userId;
    private UUID stationId;
    private UUID vehicleId;
    private UUID tankId;

    @BeforeEach
    void setUp() {
        var now = OffsetDateTime.parse("2026-08-18T10:00:00Z");
        userId = UUID.randomUUID();
        stationId = UUID.randomUUID();
        vehicleId = UUID.randomUUID();
        tankId = UUID.randomUUID();

        jdbc.update("INSERT INTO app_user (id, username, email, password_hash, first_name, last_name, active, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                userId, "bunker.operator", "operator@example.test", "hash", "Bunker", "Operator", true, now, now);

        var vehicle = new Vehicle(vehicleId, "WP-BNK-9999", "CHASSIS-BNK", "ENG-BNK", UUID.randomUUID(),
                UUID.randomUUID(), "Isuzu", "NPR", 2024, "OWNED", "AVAILABLE", 5000.0, 100.0, 5000.0, true);
        vehicleHierarchy(jdbc, vehicle);
        vehicles.save(vehicle);

        var locationId = UUID.randomUUID();
        locations(jdbc, locationId);
        stations.save(new FuelStation(stationId, "STN-BNK-INT", "Internal Depot Point", FuelStationType.INTERNAL, true, null, locationId));

        bunkerTanks.save(new BunkerTank(
                tankId,
                stationId,
                "BNK-INT-DSL-01",
                "Internal Test Diesel Tank",
                "DIESEL",
                new BigDecimal("5000.000"),
                new BigDecimal("1000.000"),
                new BigDecimal("200.000"),
                BunkerTankStatus.ACTIVE,
                now,
                true,
                now,
                now
        ));
    }

    @Test
    void shouldDeductBunkerStockUponIssuance() {
        var createCmd = new FuelIssueUseCase.CreateCommand(
                vehicleId,
                null,
                null,
                "DIESEL",
                new BigDecimal("200.000"),
                new BigDecimal("310.0000"),
                stationId,
                new BigDecimal("5100.000"),
                null,
                OffsetDateTime.parse("2026-08-18T10:30:00Z"),
                "Operational internal fuel issue"
        );

        var draft = fuelIssues.create(createCmd, "bunker.operator");
        assertNotNull(draft.id());
        assertEquals(FuelIssueStatus.DRAFT, draft.status());

        var submitted = fuelIssues.submit(draft.id(), "bunker.operator");
        assertEquals(FuelIssueStatus.PENDING_AUTHORIZATION, submitted.status());

        var authorized = fuelIssues.authorize(submitted.id(), "Authorized for operations", "bunker.operator");
        assertEquals(FuelIssueStatus.AUTHORIZED, authorized.status());

        var issued = fuelIssues.issue(authorized.id(), "bunker.operator");
        assertEquals(FuelIssueStatus.ISSUED, issued.status());

        // Verify stock level reduced from 1000.000 to 800.000
        var tankAfter = bunkerTanks.findById(tankId).orElseThrow();
        assertEquals(0, new BigDecimal("800.000").compareTo(tankAfter.currentStockLiters()));

        // Verify ledger movement created
        var movements = bunkerMovements.findByTankIdPaged(tankId, 0, 10);
        assertFalse(movements.isEmpty());
        var movement = movements.get(0);
        assertEquals(BunkerMovementType.FUEL_ISSUE, movement.movementType());
        assertEquals(BunkerReferenceType.FUEL_ISSUE, movement.referenceType());
        assertEquals(issued.id(), movement.referenceId());
        assertEquals(0, new BigDecimal("200.000").compareTo(movement.quantityLiters()));
        assertEquals(0, new BigDecimal("800.000").compareTo(movement.resultingBalanceLiters()));
    }

    @Test
    void shouldRejectInternalIssueWhenStockIsInsufficient() {
        var createCmd = new FuelIssueUseCase.CreateCommand(
                vehicleId,
                null,
                null,
                "DIESEL",
                new BigDecimal("1500.000"),
                new BigDecimal("310.0000"),
                stationId,
                new BigDecimal("5100.000"),
                null,
                OffsetDateTime.parse("2026-08-18T10:30:00Z"),
                "Exceeds stock"
        );

        var ex = assertThrows(BusinessRuleException.class, () -> fuelIssues.create(createCmd, "bunker.operator"));
        assertEquals("INSUFFICIENT_BUNKER_STOCK", ex.code());
    }

    @Test
    void shouldRejectInternalIssueWhenNoActiveBunkerTankForFuelType() {
        var createCmd = new FuelIssueUseCase.CreateCommand(
                vehicleId,
                null,
                null,
                "PETROL_95",
                new BigDecimal("50.000"),
                new BigDecimal("370.0000"),
                stationId,
                new BigDecimal("5100.000"),
                null,
                OffsetDateTime.parse("2026-08-18T10:30:00Z"),
                "No tank"
        );

        var ex = assertThrows(BusinessRuleException.class, () -> fuelIssues.create(createCmd, "bunker.operator"));
        assertEquals("NO_ACTIVE_BUNKER_TANK", ex.code());
    }
}
