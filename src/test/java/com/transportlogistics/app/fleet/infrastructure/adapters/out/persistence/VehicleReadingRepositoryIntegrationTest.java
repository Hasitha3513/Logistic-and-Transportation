package com.transportlogistics.app.fleet.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.fleet.application.ports.in.VehicleReadingUseCase;
import com.transportlogistics.app.fleet.application.ports.out.VehicleReadingRepository;
import com.transportlogistics.app.fleet.vehiclemaster.ports.outbound.VehicleRepository;
import com.transportlogistics.app.fleet.vehiclemaster.domain.model.Vehicle;
import com.transportlogistics.app.fleet.domain.model.VehicleReading;
import com.transportlogistics.app.fleet.domain.model.VehicleReadingSourceType;
import com.transportlogistics.app.fleet.domain.model.VehicleReadingType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static com.transportlogistics.app.support.ReferenceFixtures.vehicleHierarchy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class VehicleReadingRepositoryIntegrationTest {
    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-08-16T08:00:00Z");

    @Autowired VehicleRepository vehicles;
    @Autowired VehicleReadingRepository readings;
    @Autowired JdbcTemplate jdbc;

    @Test
    void persistsAndQueriesChronologicalNeighborsLatestAndPage() {
        var vehicle = vehicle();
        var actor = actor();
        vehicleHierarchy(jdbc, vehicle);
        vehicles.save(vehicle);
        var first = readings.save(reading(vehicle.id(), actor, "10000", NOW.minusDays(2), "first"));
        var latest = readings.save(reading(vehicle.id(), actor, "10200", NOW, "latest"));

        assertEquals(first.id(), readings.findPreviousEffective(vehicle.id(), VehicleReadingType.ODOMETER, 0,
                NOW.minusDays(1)).orElseThrow().id());
        assertEquals(latest.id(), readings.findNextEffective(vehicle.id(), VehicleReadingType.ODOMETER, 0,
                NOW.minusDays(1)).orElseThrow().id());
        assertEquals(latest.id(), readings.findLatestEffective(vehicle.id(), VehicleReadingType.ODOMETER, 0)
                .orElseThrow().id());
        assertEquals(first.id(), readings.findByIdempotencyKey("first").orElseThrow().id());

        var page = readings.search(new VehicleReadingUseCase.SearchQuery(vehicle.id(),
                VehicleReadingType.ODOMETER, VehicleReadingSourceType.MANUAL, NOW.minusDays(3), NOW, 0, 1));
        assertEquals(1, page.content().size());
        assertEquals(2, page.totalElements());
        assertEquals(2, page.totalPages());
        assertTrue(page.content().getFirst().recordedAt().isAfter(first.recordedAt()));
    }

    private VehicleReading reading(UUID vehicleId, UUID actor, String value, OffsetDateTime at, String key) {
        return new VehicleReading(UUID.randomUUID(), vehicleId, VehicleReadingType.ODOMETER,
                new BigDecimal(value).setScale(3), VehicleReadingType.ODOMETER.unit(), 0,
                VehicleReadingSourceType.MANUAL, null, at, NOW, actor, null, null, key, null, NOW);
    }

    private Vehicle vehicle() {
        return new Vehicle(UUID.randomUUID(), "READ-" + UUID.randomUUID(), null, null, UUID.randomUUID(),
                UUID.randomUUID(), "Maker", "Model", 2026, "OWNED", "AVAILABLE", null, null, 1000d, true);
    }

    private UUID actor() {
        var id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO app_user
                    (id, username, email, password_hash, first_name, last_name, active, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, id, "reading-" + id, id + "@test.local", "unused", "Reading", "Tester", true, NOW, NOW);
        return id;
    }
}
