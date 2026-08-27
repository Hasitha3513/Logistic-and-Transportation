package com.transportlogistics.app.fleet.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.fleet.application.ports.out.DriverRepository;
import com.transportlogistics.app.fleet.domain.model.Driver;
import com.transportlogistics.app.fleet.domain.model.DriverViolation;
import com.transportlogistics.app.fleet.domain.model.DriverViolationType;
import com.transportlogistics.app.fleet.domain.model.FinePaymentStatus;
import com.transportlogistics.app.fleet.domain.model.ViolationSeverity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DirtiesContext
class DriverViolationPersistenceIntegrationTest {

    @Autowired
    private DriverViolationPersistenceAdapter adapter;

    @Autowired
    private DriverRepository drivers;

    @Autowired
    private JdbcTemplate jdbc;

    private final UUID driverId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        jdbc.update("DELETE FROM trip");
        jdbc.update("DELETE FROM driver_medical_record");
        jdbc.update("DELETE FROM driver_drug_test");
        jdbc.update("DELETE FROM driver_violation");
        jdbc.update("DELETE FROM driver_exception");
        jdbc.update("DELETE FROM driver_license");
        jdbc.update("DELETE FROM driver");

        var driver = new Driver(
                driverId, "EMP-VIOLATION-1", "Michael", "Scott", "+1234567890", "michael@example.test",
                "AVAILABLE", true
        );
        drivers.save(driver);
    }

    @Test
    void persistsAndQueriesViolations() {
        var violationId = UUID.randomUUID();
        var date = OffsetDateTime.of(2026, 9, 1, 14, 30, 0, 0, ZoneOffset.UTC);
        var now = OffsetDateTime.now(ZoneOffset.UTC);

        var violation = new DriverViolation(
                violationId, driverId, null, DriverViolationType.SPEEDING, ViolationSeverity.MODERATE,
                date, 3, new BigDecimal("150.00"), FinePaymentStatus.UNPAID, null, null,
                "Route 9", "Excess speed", now, now, "tester", "tester"
        );

        var saved = adapter.save(violation);
        assertThat(saved).isNotNull();
        assertThat(saved.id()).isEqualTo(violationId);

        var found = adapter.findById(violationId);
        assertThat(found).isPresent();
        assertThat(found.get().violationType()).isEqualTo(DriverViolationType.SPEEDING);
        assertThat(found.get().fineAmount()).isEqualByComparingTo("150.00");

        var list = adapter.findByDriverId(driverId);
        assertThat(list).hasSize(1);
        assertThat(list.get(0).id()).isEqualTo(violationId);

        var dateFiltered = adapter.findByDriverIdAndViolationDateBetween(
                driverId,
                date.minusDays(1),
                date.plusDays(1)
        );
        assertThat(dateFiltered).hasSize(1);
    }
}
