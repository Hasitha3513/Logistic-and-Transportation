package com.transportlogistics.app.fleet.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.fleet.application.ports.out.DriverLicenseRepository;
import com.transportlogistics.app.fleet.application.ports.out.DriverRepository;
import com.transportlogistics.app.fleet.domain.model.Driver;
import com.transportlogistics.app.fleet.domain.model.DriverLicense;
import com.transportlogistics.app.fleet.domain.model.DriverLicenseStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class DriverLicenseRepositoryIntegrationTest {
    @Autowired DriverRepository drivers;
    @Autowired DriverLicenseRepository licenses;

    @Test
    void persistsQueriesEnforcesNumberLookupAndRetainsSoftDeletedLicense() {
        var driverId = UUID.randomUUID();
        drivers.save(new Driver(driverId, "EMP-" + driverId, "Alex", "Driver", null, null, "AVAILABLE", true));
        var now = OffsetDateTime.now();
        var licenseId = UUID.randomUUID();
        var saved = licenses.save(new DriverLicense(licenseId, driverId, "DL-900", "B",
                LocalDate.of(2025, 1, 1), LocalDate.of(2027, 1, 1), DriverLicenseStatus.ACTIVE, true,
                now, now, "tester", "tester"));

        assertEquals(licenseId, saved.id());
        assertEquals(1, licenses.findVisibleByDriverId(driverId).size());
        assertEquals(1, licenses.findActiveByDriverId(driverId).size());
        assertTrue(licenses.licenseNumberExists("dl-900", null));

        licenses.save(new DriverLicense(saved.id(), saved.driverId(), saved.licenseNumber(), saved.licenseClass(),
                saved.issueDate(), saved.expiryDate(), DriverLicenseStatus.DELETED, false, saved.createdAt(),
                OffsetDateTime.now(), saved.createdBy(), "tester"));

        assertTrue(licenses.findVisibleByDriverId(driverId).isEmpty());
        assertTrue(licenses.findActiveByDriverId(driverId).isEmpty());
        assertEquals(DriverLicenseStatus.DELETED, licenses.findById(licenseId).orElseThrow().status());
        assertTrue(licenses.licenseNumberExists("DL-900", null));
    }
}
