package com.transportlogistics.app.fleet.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.fleet.domain.model.Driver;
import com.transportlogistics.app.fleet.domain.model.DriverDrugTest;
import com.transportlogistics.app.fleet.domain.model.DrugTestResult;
import com.transportlogistics.app.fleet.domain.model.DrugTestStatus;
import com.transportlogistics.app.fleet.domain.model.DrugTestType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import({DriverDrugTestPersistenceAdapter.class, DriverPersistenceAdapter.class})
class DriverDrugTestPersistenceIntegrationTest {

    @Autowired
    private DriverDrugTestPersistenceAdapter adapter;

    @Autowired
    private DriverPersistenceAdapter driverAdapter;

    @Test
    @DisplayName("Should save and retrieve driver drug tests")
    void shouldSaveAndRetrieveDrugTest() {
        var driverId = UUID.randomUUID();
        var driver = new Driver(driverId, "EMP-DT-" + driverId.toString().substring(0, 6), "Mark", "Tester", null, null, "AVAILABLE", true);
        driverAdapter.save(driver);

        var now = OffsetDateTime.now();
        var test = new DriverDrugTest(
                UUID.randomUUID(), driverId, DrugTestType.RANDOM, LocalDate.of(2026, 3, 1), now, LocalDate.of(2026, 3, 2),
                DrugTestResult.NEGATIVE, DrugTestStatus.COMPLETED, "Quest", "DT-999", "Negative",
                false, null, true, now, now, "tester", "tester"
        );

        var saved = adapter.save(test);
        assertNotNull(saved);

        var list = adapter.findByDriverId(driverId);
        assertEquals(1, list.size());
        assertEquals(DrugTestResult.NEGATIVE, list.get(0).result());

        var latest = adapter.findLatestByDriverId(driverId);
        assertTrue(latest.isPresent());
        assertEquals("DT-999", latest.get().referenceNumber());
    }
}
