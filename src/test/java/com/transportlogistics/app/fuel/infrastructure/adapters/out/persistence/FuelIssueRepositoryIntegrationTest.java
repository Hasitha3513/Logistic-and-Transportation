package com.transportlogistics.app.fuel.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.fleet.vehiclemaster.ports.outbound.VehicleRepository;
import com.transportlogistics.app.fleet.vehiclemaster.domain.model.Vehicle;
import com.transportlogistics.app.fuel.application.ports.in.FuelIssueUseCase;
import com.transportlogistics.app.fuel.application.ports.out.FuelIssueHistoryRepository;
import com.transportlogistics.app.fuel.application.ports.out.FuelIssueRepository;
import com.transportlogistics.app.fuel.application.ports.out.FuelLimitPolicyRepository;
import com.transportlogistics.app.fuel.application.ports.out.FuelStationRepository;
import com.transportlogistics.app.fuel.application.ports.out.FuelVoucherGenerator;
import com.transportlogistics.app.fuel.domain.model.FuelIssue;
import com.transportlogistics.app.fuel.domain.model.FuelIssueHistory;
import com.transportlogistics.app.fuel.domain.model.FuelIssueStatus;
import com.transportlogistics.app.fuel.domain.model.FuelStation;
import com.transportlogistics.app.fuel.domain.model.FuelStationType;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static com.transportlogistics.app.support.ReferenceFixtures.vehicleHierarchy;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class FuelIssueRepositoryIntegrationTest {
    @Autowired FuelIssueRepository issues;
    @Autowired FuelIssueHistoryRepository history;
    @Autowired FuelStationRepository stations;
    @Autowired FuelLimitPolicyRepository limits;
    @Autowired FuelVoucherGenerator vouchers;
    @Autowired VehicleRepository vehicles;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager entityManager;

    @Test
    void persistsSearchesAndLocksIssueWithAppendOnlyHistory() {
        var references = references();
        var now = OffsetDateTime.parse("2026-08-15T08:00:00Z");
        var issue = new FuelIssue(UUID.randomUUID(), vouchers.next(2026), references.vehicleId, null, null,
                "DIESEL", new BigDecimal("45.500"), new BigDecimal("2.0000"), new BigDecimal("91.00"),
                references.stationId, new BigDecimal("1100"), null, now, FuelIssueStatus.DRAFT,
                references.userId, null, null, "Repository test", now, now);
        var saved = issues.save(issue);
        history.save(new FuelIssueHistory(UUID.randomUUID(), saved.id(), null, FuelIssueStatus.DRAFT, "CREATED",
                references.userId, "repository.test", "Created", now));

        var page = issues.search(new FuelIssueUseCase.SearchQuery(0, 10, references.vehicleId, null,
                FuelIssueStatus.DRAFT, now.toLocalDate(), now.toLocalDate(), saved.voucherNumber()));

        assertEquals(1, page.totalElements());
        assertEquals(saved.id(), page.content().getFirst().id());
        assertEquals(saved.id(), issues.findByIdForUpdate(saved.id()).orElseThrow().id());
        assertEquals("CREATED", history.findByFuelIssueId(saved.id()).getFirst().action());
        assertTrue(issues.existsByVoucherNumber(saved.voucherNumber()));
    }

    @Test
    void databaseEnforcesVoucherUniqueness() {
        var references = references();
        var now = OffsetDateTime.parse("2026-08-15T08:00:00Z");
        var voucher = vouchers.next(2026);
        issues.save(issue(UUID.randomUUID(), voucher, references, now));
        issues.save(issue(UUID.randomUUID(), voucher, references, now));
        assertThrows(RuntimeException.class, entityManager::flush);
    }

    @Test
    void voucherSequenceProducesUniqueFormattedNumbersAndPolicyIsApplicable() {
        var references = references();
        jdbc.update("INSERT INTO fuel_limit_policy (id, vehicle_id, maximum_quantity_per_issue, active) VALUES (?, ?, ?, ?)",
                UUID.randomUUID(), references.vehicleId, new BigDecimal("75"), true);
        var first = vouchers.next(2026);
        var second = vouchers.next(2026);
        assertTrue(first.matches("FUEL-2026-\\d{6}"));
        assertNotEquals(first, second);
        assertEquals(new BigDecimal("75.000"), limits.findApplicable(references.vehicleId).getFirst().maximumQuantityPerIssue());
    }

    private FuelIssue issue(UUID id, String voucher, References references, OffsetDateTime now) {
        return new FuelIssue(id, voucher, references.vehicleId, null, null, "DIESEL", new BigDecimal("10"),
                null, null, references.stationId, null, null, now, FuelIssueStatus.DRAFT, references.userId,
                null, null, null, now, now);
    }

    private References references() {
        var userId = UUID.randomUUID();
        jdbc.update("INSERT INTO app_user (id, username, email, password_hash, first_name, last_name, active, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                userId, "fuel-" + userId, userId + "@example.test", "not-used", "Fuel", "Tester", true,
                OffsetDateTime.now(), OffsetDateTime.now());
        var vehicleId = UUID.randomUUID();
        var vehicle = new Vehicle(vehicleId, "FUEL-REG-" + vehicleId, null, null, UUID.randomUUID(), UUID.randomUUID(),
                "Test", "Truck", 2026, "COMPANY_OWNED", "AVAILABLE", 1000d, 10d, 5000d, true);
        vehicleHierarchy(jdbc, vehicle);
        vehicles.save(vehicle);
        entityManager.flush();
        var stationId = UUID.randomUUID();
        stations.save(new FuelStation(stationId, "ST-" + stationId, "Test Fuel Station",
                FuelStationType.INTERNAL, true, null, null));
        return new References(userId, vehicleId, stationId);
    }

    private record References(UUID userId, UUID vehicleId, UUID stationId) {
    }
}
