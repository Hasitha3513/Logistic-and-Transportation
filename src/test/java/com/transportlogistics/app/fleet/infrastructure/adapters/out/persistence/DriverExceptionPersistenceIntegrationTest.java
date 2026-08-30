package com.transportlogistics.app.fleet.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.fleet.application.ports.out.DriverRepository;
import com.transportlogistics.app.fleet.domain.model.Driver;
import com.transportlogistics.app.fleet.domain.model.DriverException;
import com.transportlogistics.app.fleet.domain.model.DriverExceptionStatus;
import com.transportlogistics.app.fleet.domain.model.DriverExceptionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DirtiesContext
class DriverExceptionPersistenceIntegrationTest {

    @Autowired
    private DriverExceptionPersistenceAdapter adapter;

    @Autowired
    private DriverRepository drivers;

    @Autowired
    private JdbcTemplate jdbc;

    private final UUID driverId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        jdbc.update("DELETE FROM trip_operational_event");
        jdbc.update("DELETE FROM trip_status_history");
        jdbc.update("DELETE FROM trip_dispatch");
        jdbc.update("DELETE FROM trip");
        jdbc.update("DELETE FROM driver_violation");
        jdbc.update("DELETE FROM driver_medical_record");
        jdbc.update("DELETE FROM driver_drug_test");
        jdbc.update("DELETE FROM driver_license");
        jdbc.update("DELETE FROM driver_exception");
        jdbc.update("DELETE FROM driver");

        var driver = new Driver(
                driverId, "EMP-PERSIST-1", "Jane", "Smith", "+1234567890", "jane@example.test",
                "AVAILABLE", true
        );
        drivers.save(driver);
    }

    @Test
    void persistsAndRetrievesDriverException() {
        var exceptionId = UUID.randomUUID();
        var start = OffsetDateTime.of(2026, 9, 1, 8, 0, 0, 0, ZoneOffset.UTC);
        var end = OffsetDateTime.of(2026, 9, 1, 16, 0, 0, 0, ZoneOffset.UTC);
        var now = OffsetDateTime.now(ZoneOffset.UTC);

        var exception = new DriverException(
                exceptionId, driverId, DriverExceptionType.LEAVE, start, end, DriverExceptionStatus.SCHEDULED,
                "Family wedding", "Approved", now, now, "tester", "tester"
        );

        var saved = adapter.save(exception);
        assertThat(saved).isNotNull();
        assertThat(saved.id()).isEqualTo(exceptionId);

        var retrieved = adapter.findById(exceptionId);
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().exceptionType()).isEqualTo(DriverExceptionType.LEAVE);
        assertThat(retrieved.get().status()).isEqualTo(DriverExceptionStatus.SCHEDULED);
        assertThat(retrieved.get().reason()).isEqualTo("Family wedding");

        var list = adapter.findByDriverId(driverId);
        assertThat(list).hasSize(1);
        assertThat(list.get(0).id()).isEqualTo(exceptionId);
    }

    @Test
    void testsOverlapQueryUnderVariousIntervalConditions() {
        var start = OffsetDateTime.of(2026, 9, 1, 10, 0, 0, 0, ZoneOffset.UTC);
        var end = OffsetDateTime.of(2026, 9, 1, 14, 0, 0, 0, ZoneOffset.UTC);
        var now = OffsetDateTime.now(ZoneOffset.UTC);

        var exception = new DriverException(
                UUID.randomUUID(), driverId, DriverExceptionType.LEAVE, start, end, DriverExceptionStatus.SCHEDULED,
                null, null, now, now, "tester", "tester"
        );
        adapter.save(exception);

        var blockingStatuses = List.of(DriverExceptionStatus.SCHEDULED, DriverExceptionStatus.ACTIVE);

        // 1. Boundary match: allocation ends exactly when exception starts [08:00, 10:00) -> NO OVERLAP
        assertThat(adapter.hasOverlappingException(
                driverId,
                OffsetDateTime.of(2026, 9, 1, 8, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2026, 9, 1, 10, 0, 0, 0, ZoneOffset.UTC),
                blockingStatuses
        )).isFalse();

        // 2. Boundary match: allocation starts exactly when exception ends [14:00, 18:00) -> NO OVERLAP
        assertThat(adapter.hasOverlappingException(
                driverId,
                OffsetDateTime.of(2026, 9, 1, 14, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2026, 9, 1, 18, 0, 0, 0, ZoneOffset.UTC),
                blockingStatuses
        )).isFalse();

        // 3. Partial overlap left: [09:00, 12:00) -> OVERLAP
        assertThat(adapter.hasOverlappingException(
                driverId,
                OffsetDateTime.of(2026, 9, 1, 9, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2026, 9, 1, 12, 0, 0, 0, ZoneOffset.UTC),
                blockingStatuses
        )).isTrue();

        // 4. Partial overlap right: [12:00, 16:00) -> OVERLAP
        assertThat(adapter.hasOverlappingException(
                driverId,
                OffsetDateTime.of(2026, 9, 1, 12, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2026, 9, 1, 16, 0, 0, 0, ZoneOffset.UTC),
                blockingStatuses
        )).isTrue();

        // 5. Non-blocking status: CANCELLED or COMPLETED -> NO OVERLAP
        var cancelled = new DriverException(
                UUID.randomUUID(), driverId, DriverExceptionType.DISCIPLINARY_SUSPENSION,
                OffsetDateTime.of(2026, 9, 2, 10, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2026, 9, 2, 14, 0, 0, 0, ZoneOffset.UTC),
                DriverExceptionStatus.CANCELLED, null, null, now, now, "tester", "tester"
        );
        adapter.save(cancelled);

        assertThat(adapter.hasOverlappingException(
                driverId,
                OffsetDateTime.of(2026, 9, 2, 11, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2026, 9, 2, 13, 0, 0, 0, ZoneOffset.UTC),
                blockingStatuses
        )).isFalse();
    }
}
