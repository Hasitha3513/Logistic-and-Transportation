package com.transportlogistics.app.fleet.application.service;

import com.transportlogistics.app.fleet.DriverAssignmentAvailability;
import com.transportlogistics.app.fleet.application.ports.in.DriverAvailabilityUseCase;
import com.transportlogistics.app.fleet.application.ports.out.DriverLicenseRepository;
import com.transportlogistics.app.fleet.application.ports.out.DriverRepository;
import com.transportlogistics.app.fleet.domain.model.Driver;
import com.transportlogistics.app.fleet.domain.model.DriverAvailability;
import com.transportlogistics.app.fleet.domain.model.DriverLicense;
import com.transportlogistics.app.fleet.domain.model.DriverLicenseStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.transportlogistics.app.fleet.domain.model.DriverAvailability.Code.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DriverAvailabilityServiceTest {
    private DriverRepository drivers;
    private DriverLicenseRepository licenses;
    private DriverAssignmentAvailability assignments;
    private DriverAvailabilityService service;
    private UUID driverId;
    private OffsetDateTime from;
    private OffsetDateTime to;

    @BeforeEach
    void setUp() {
        drivers = mock(DriverRepository.class);
        licenses = mock(DriverLicenseRepository.class);
        assignments = mock(DriverAssignmentAvailability.class);
        service = new DriverAvailabilityService(drivers, licenses, assignments);
        driverId = UUID.randomUUID();
        from = OffsetDateTime.parse("2026-02-01T08:00:00Z");
        to = OffsetDateTime.parse("2026-02-01T10:00:00Z");
        givenDriver(true, "AVAILABLE");
        when(licenses.findActiveByDriverId(driverId)).thenReturn(List.of(validLicense("B")));
    }

    @Test
    void eligibleDriverHasNoReasons() {
        var result = evaluate("B", null);
        assertTrue(result.available());
        assertTrue(result.reasons().isEmpty());
    }

    @Test
    void rejectsInactiveDriver() {
        givenDriver(false, "AVAILABLE");
        assertRejected(INACTIVE);
    }

    @Test
    void rejectsOperationallyUnavailableDriver() {
        givenDriver(true, "ON_LEAVE");
        assertRejected(OPERATIONALLY_UNAVAILABLE);
    }

    @Test
    void rejectsDriverWithoutActiveLicense() {
        when(licenses.findActiveByDriverId(driverId)).thenReturn(List.of());
        assertRejected(LICENSE_MISSING);
    }

    @Test
    void rejectsLicenseNotYetValidAtRequestedStart() {
        when(licenses.findActiveByDriverId(driverId)).thenReturn(List.of(
                license("B", from.toLocalDate().plusDays(1), to.toLocalDate().plusYears(1))));
        assertRejected(LICENSE_NOT_YET_VALID);
    }

    @Test
    void rejectsLicenseExpiringBeforeRequestedEnd() {
        when(licenses.findActiveByDriverId(driverId)).thenReturn(List.of(
                license("B", from.toLocalDate().minusYears(1), to.toLocalDate().minusDays(1))));
        assertRejected(LICENSE_EXPIRED);
    }

    @Test
    void rejectsMissingRequiredLicenseClass() {
        var result = evaluate("C", null);
        assertFalse(result.available());
        assertTrue(result.hasReason(REQUIRED_LICENSE_CLASS_MISSING));
    }

    @Test
    void rejectsOverlappingAssignmentAndPassesExclusion() {
        var excludeTripId = UUID.randomUUID();
        when(assignments.hasOverlap(driverId, from, to, excludeTripId)).thenReturn(true);

        var result = evaluate("B", excludeTripId);

        assertTrue(result.hasReason(OVERLAPPING_ASSIGNMENT));
        verify(assignments).hasOverlap(driverId, from, to, excludeTripId);
    }

    @Test
    void returnsAllApplicableReasons() {
        givenDriver(false, "ON_LEAVE");
        when(licenses.findActiveByDriverId(driverId)).thenReturn(List.of());
        when(assignments.hasOverlap(driverId, from, to, null)).thenReturn(true);

        var result = evaluate("C", null);

        assertEquals(5, result.reasons().size());
        assertTrue(result.hasReason(INACTIVE));
        assertTrue(result.hasReason(OPERATIONALLY_UNAVAILABLE));
        assertTrue(result.hasReason(LICENSE_MISSING));
        assertTrue(result.hasReason(REQUIRED_LICENSE_CLASS_MISSING));
        assertTrue(result.hasReason(OVERLAPPING_ASSIGNMENT));
    }

    private void assertRejected(DriverAvailability.Code code) {
        var result = evaluate(null, null);
        assertFalse(result.available());
        assertTrue(result.hasReason(code));
    }

    private DriverAvailability evaluate(String requiredClass, UUID excludeTripId) {
        return service.evaluate(new DriverAvailabilityUseCase.Query(driverId, from, to, requiredClass,
                excludeTripId));
    }

    private void givenDriver(boolean active, String status) {
        when(drivers.findById(driverId)).thenReturn(Optional.of(
                new Driver(driverId, "EMP-1", "Alex", "Driver", null, null, status, active)));
    }

    private DriverLicense validLicense(String licenseClass) {
        return license(licenseClass, from.toLocalDate().minusYears(1), to.toLocalDate().plusYears(1));
    }

    private DriverLicense license(String licenseClass, LocalDate issueDate, LocalDate expiryDate) {
        var now = OffsetDateTime.parse("2026-01-01T00:00:00Z");
        return new DriverLicense(UUID.randomUUID(), driverId, "DL-" + UUID.randomUUID(), licenseClass,
                issueDate, expiryDate, DriverLicenseStatus.ACTIVE, true, now, now, "tester", "tester");
    }
}
