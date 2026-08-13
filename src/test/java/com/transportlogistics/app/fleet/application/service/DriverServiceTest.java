package com.transportlogistics.app.fleet.application.service;

import com.transportlogistics.app.fleet.application.ports.out.DriverLicenseRepository;
import com.transportlogistics.app.fleet.application.ports.out.DriverRepository;
import com.transportlogistics.app.fleet.domain.model.Driver;
import com.transportlogistics.app.fleet.domain.model.DriverLicense;
import com.transportlogistics.app.fleet.domain.model.DriverLicenseStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DriverServiceTest {
    @Test
    void expiredActiveLicenseMakesDriverUnavailable() {
        var context = context(List.of(license("B", LocalDate.of(2025, 12, 31))));

        var result = context.service.availability(context.driverId, null, LocalDate.of(2026, 1, 1));

        assertFalse(result.available());
        assertEquals("LICENSE_EXPIRED", result.reason());
    }

    @Test
    void requiredLicenseClassMustBeActiveAndValidAtAssignmentDate() {
        var context = context(List.of(license("B", LocalDate.of(2027, 1, 1))));

        assertTrue(context.service.availability(context.driverId, "B", LocalDate.of(2026, 1, 1)).available());
        assertEquals("REQUIRED_LICENSE_CLASS_MISSING_OR_EXPIRED",
                context.service.availability(context.driverId, "C", LocalDate.of(2026, 1, 1)).reason());
        assertThrows(IllegalArgumentException.class,
                () -> context.service.assertAvailableForAssignment(context.driverId, "C", LocalDate.of(2026, 1, 1)));
    }

    private Context context(List<DriverLicense> driverLicenses) {
        var drivers = mock(DriverRepository.class);
        var licenses = mock(DriverLicenseRepository.class);
        var driverId = driverLicenses.getFirst().driverId();
        when(drivers.findById(driverId)).thenReturn(Optional.of(
                new Driver(driverId, "EMP-1", "Alex", "Driver", null, null, "AVAILABLE", true)));
        when(licenses.findActiveByDriverId(driverId)).thenReturn(driverLicenses);
        return new Context(driverId, new DriverService(drivers, licenses));
    }

    private DriverLicense license(String licenseClass, LocalDate expiryDate) {
        var now = OffsetDateTime.parse("2026-01-01T00:00:00Z");
        return new DriverLicense(UUID.randomUUID(), UUID.randomUUID(), "DL-" + licenseClass, licenseClass,
                LocalDate.of(2025, 1, 1), expiryDate, DriverLicenseStatus.ACTIVE, true, now, now, "tester", "tester");
    }

    private record Context(UUID driverId, DriverService service) {
    }
}
