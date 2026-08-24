package com.transportlogistics.app.fleet.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.fleet.domain.model.Driver;
import com.transportlogistics.app.fleet.domain.model.DriverMedicalRecord;
import com.transportlogistics.app.fleet.domain.model.DriverMedicalStatus;
import com.transportlogistics.app.fleet.domain.model.VisionTestStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({DriverMedicalRecordPersistenceAdapter.class, DriverPersistenceAdapter.class})
class DriverMedicalPersistenceIntegrationTest {

    @Autowired
    private DriverMedicalRecordPersistenceAdapter adapter;

    @Autowired
    private DriverPersistenceAdapter driverAdapter;

    @Test
    @DisplayName("Should save and retrieve driver medical records")
    void shouldSaveAndRetrieveMedicalRecord() {
        var driverId = UUID.randomUUID();
        var driver = new Driver(driverId, "EMP-MED-" + driverId.toString().substring(0, 6), "Jane", "Doc", null, null, "AVAILABLE", true);
        driverAdapter.save(driver);

        var now = OffsetDateTime.now();
        var record = new DriverMedicalRecord(
                UUID.randomUUID(), driverId, LocalDate.of(2026, 1, 10), LocalDate.of(2026, 1, 10), LocalDate.of(2027, 1, 10),
                DriverMedicalStatus.FIT, VisionTestStatus.PASSED, null, "Dr. Smith", "MED-CERT-123", "Cleared",
                true, now, now, "tester", "tester"
        );

        var saved = adapter.save(record);
        assertNotNull(saved);

        var list = adapter.findByDriverId(driverId);
        assertEquals(1, list.size());
        assertEquals("MED-CERT-123", list.get(0).certificateReference());

        var latest = adapter.findLatestByDriverId(driverId);
        assertTrue(latest.isPresent());
        assertEquals(DriverMedicalStatus.FIT, latest.get().fitnessStatus());
    }
}
