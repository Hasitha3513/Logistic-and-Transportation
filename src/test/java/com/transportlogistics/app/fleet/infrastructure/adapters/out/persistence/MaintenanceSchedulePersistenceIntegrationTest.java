package com.transportlogistics.app.fleet.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.fleet.vehiclemaster.ports.outbound.VehicleRepository;
import com.transportlogistics.app.fleet.domain.model.MaintenanceSchedule;
import com.transportlogistics.app.fleet.domain.model.MaintenanceStatus;
import com.transportlogistics.app.fleet.vehiclemaster.domain.model.Vehicle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static com.transportlogistics.app.support.ReferenceFixtures.vehicleHierarchy;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DirtiesContext
class MaintenanceSchedulePersistenceIntegrationTest {

    @Autowired
    private MaintenanceSchedulePersistenceAdapter adapter;

    @Autowired
    private VehicleRepository vehicles;

    @Autowired
    private JdbcTemplate jdbc;

    private final UUID vehicleId = UUID.randomUUID();
    private final UUID categoryId = UUID.randomUUID();
    private final UUID typeId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        jdbc.update("DELETE FROM load_plan_item_placement");
        jdbc.update("DELETE FROM load_plan");
        jdbc.update("DELETE FROM trip");
        jdbc.update("DELETE FROM vehicle_document");
        jdbc.update("DELETE FROM vehicle_reading");
        jdbc.update("DELETE FROM vehicle_meter_reset");
        jdbc.update("DELETE FROM maintenance_schedule");
        jdbc.update("DELETE FROM vehicle");

        var vehicle = new Vehicle(
                vehicleId, "WP-CAD-9999", "CHASSIS99", "ENG99", categoryId, typeId,
                "Maker", "Model", 2025, "OWNED", "AVAILABLE", 50000.0, 1200.0, 10000.0, true
        );
        vehicleHierarchy(jdbc, vehicle);
        vehicles.save(vehicle);
    }

    @Test
    void persistsAndRetrievesMaintenanceSchedule() {
        var scheduleId = UUID.randomUUID();
        var start = OffsetDateTime.of(2026, 9, 1, 8, 0, 0, 0, ZoneOffset.UTC);
        var end = OffsetDateTime.of(2026, 9, 1, 16, 0, 0, 0, ZoneOffset.UTC);
        var now = OffsetDateTime.now(ZoneOffset.UTC);

        var schedule = new MaintenanceSchedule(
                scheduleId, vehicleId, "Major Service", start, end, MaintenanceStatus.SCHEDULED,
                "Oil change and belt replacements", "Authorized Dealer", new BigDecimal("450.00"),
                now, now, "tester", "tester"
        );

        var saved = adapter.save(schedule);
        assertThat(saved).isNotNull();
        assertThat(saved.id()).isEqualTo(scheduleId);

        var retrieved = adapter.findById(scheduleId);
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().maintenanceType()).isEqualTo("Major Service");
        assertThat(retrieved.get().status()).isEqualTo(MaintenanceStatus.SCHEDULED);
        assertThat(retrieved.get().cost()).isEqualByComparingTo(new BigDecimal("450.00"));

        var list = adapter.findByVehicleId(vehicleId);
        assertThat(list).hasSize(1);
        assertThat(list.get(0).id()).isEqualTo(scheduleId);
    }

    @Test
    void testsOverlapQueryUnderVariousIntervalConditions() {
        var start = OffsetDateTime.of(2026, 9, 1, 10, 0, 0, 0, ZoneOffset.UTC);
        var end = OffsetDateTime.of(2026, 9, 1, 14, 0, 0, 0, ZoneOffset.UTC);
        var now = OffsetDateTime.now(ZoneOffset.UTC);

        var schedule = new MaintenanceSchedule(
                UUID.randomUUID(), vehicleId, "Service", start, end, MaintenanceStatus.SCHEDULED,
                null, null, null, now, now, "tester", "tester"
        );
        adapter.save(schedule);

        var blockingStatuses = List.of(MaintenanceStatus.SCHEDULED, MaintenanceStatus.IN_PROGRESS);

        // 1. Boundary match: allocation ends exactly when maintenance starts [08:00, 10:00) -> NO OVERLAP
        assertThat(adapter.hasOverlappingSchedule(
                vehicleId,
                OffsetDateTime.of(2026, 9, 1, 8, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2026, 9, 1, 10, 0, 0, 0, ZoneOffset.UTC),
                blockingStatuses
        )).isFalse();

        // 2. Boundary match: allocation starts exactly when maintenance ends [14:00, 18:00) -> NO OVERLAP
        assertThat(adapter.hasOverlappingSchedule(
                vehicleId,
                OffsetDateTime.of(2026, 9, 1, 14, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2026, 9, 1, 18, 0, 0, 0, ZoneOffset.UTC),
                blockingStatuses
        )).isFalse();

        // 3. Partial overlap start: [08:00, 12:00) -> OVERLAP
        assertThat(adapter.hasOverlappingSchedule(
                vehicleId,
                OffsetDateTime.of(2026, 9, 1, 8, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2026, 9, 1, 12, 0, 0, 0, ZoneOffset.UTC),
                blockingStatuses
        )).isTrue();

        // 4. Partial overlap end: [12:00, 16:00) -> OVERLAP
        assertThat(adapter.hasOverlappingSchedule(
                vehicleId,
                OffsetDateTime.of(2026, 9, 1, 12, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2026, 9, 1, 16, 0, 0, 0, ZoneOffset.UTC),
                blockingStatuses
        )).isTrue();

        // 5. Enclosed: [11:00, 13:00) -> OVERLAP
        assertThat(adapter.hasOverlappingSchedule(
                vehicleId,
                OffsetDateTime.of(2026, 9, 1, 11, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2026, 9, 1, 13, 0, 0, 0, ZoneOffset.UTC),
                blockingStatuses
        )).isTrue();

        // 6. Containing: [08:00, 18:00) -> OVERLAP
        assertThat(adapter.hasOverlappingSchedule(
                vehicleId,
                OffsetDateTime.of(2026, 9, 1, 8, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2026, 9, 1, 18, 0, 0, 0, ZoneOffset.UTC),
                blockingStatuses
        )).isTrue();
    }

    @Test
    void nonBlockingStatusesDoNotTriggerOverlap() {
        var start = OffsetDateTime.of(2026, 9, 1, 10, 0, 0, 0, ZoneOffset.UTC);
        var end = OffsetDateTime.of(2026, 9, 1, 14, 0, 0, 0, ZoneOffset.UTC);
        var now = OffsetDateTime.now(ZoneOffset.UTC);

        var cancelled = new MaintenanceSchedule(
                UUID.randomUUID(), vehicleId, "Service", start, end, MaintenanceStatus.CANCELLED,
                null, null, null, now, now, "tester", "tester"
        );
        adapter.save(cancelled);

        var blockingStatuses = List.of(MaintenanceStatus.SCHEDULED, MaintenanceStatus.IN_PROGRESS);

        assertThat(adapter.hasOverlappingSchedule(
                vehicleId,
                OffsetDateTime.of(2026, 9, 1, 11, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2026, 9, 1, 13, 0, 0, 0, ZoneOffset.UTC),
                blockingStatuses
        )).isFalse();
    }
}
