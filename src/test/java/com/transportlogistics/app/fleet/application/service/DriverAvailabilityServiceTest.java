package com.transportlogistics.app.fleet.application.service;

import com.transportlogistics.app.fleet.DriverAssignmentAvailability;
import com.transportlogistics.app.fleet.application.ports.in.DriverAvailabilityUseCase;
import com.transportlogistics.app.fleet.application.ports.out.DriverExceptionRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class DriverAvailabilityServiceTest {
    private DriverRepository drivers;
    private DriverLicenseRepository licenses;
    private DriverAssignmentAvailability assignments;
    private DriverExceptionRepository driverExceptions;
    private DriverAvailabilityService service;
    private UUID driverId;
    private OffsetDateTime from;
    private OffsetDateTime to;

    @BeforeEach
    void setUp() {
        drivers = mock(DriverRepository.class);
        licenses = mock(DriverLicenseRepository.class);
        assignments = mock(DriverAssignmentAvailability.class);
        driverExceptions = mock(DriverExceptionRepository.class);
        service = new DriverAvailabilityService(drivers, licenses, assignments, driverExceptions);
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
    void validLicenseIsNotInvalidatedByDifferentExpiredLicense() {
        when(licenses.findActiveByDriverId(driverId)).thenReturn(List.of(
                validLicense("B"),
                license("B", from.toLocalDate().minusYears(1), from.toLocalDate().minusDays(1))));

        var result = evaluate("B", null);

        assertTrue(result.available());
        assertTrue(result.reasons().isEmpty());
    }

    @Test
    void compatibleExpiredLicenseAndIncompatibleValidLicenseAreIneligible() {
        when(licenses.findActiveByDriverId(driverId)).thenReturn(List.of(
                license("B", from.toLocalDate().minusYears(1), from.toLocalDate().minusDays(1)),
                validLicense("C")));

        var result = evaluate("B", null);

        assertFalse(result.available());
        assertTrue(result.hasReason(LICENSE_EXPIRED));
        assertTrue(result.hasReason(REQUIRED_LICENSE_CLASS_MISSING));
    }

    @Test
    void multipleValidLicensesAreEligibleWhenOneMatchesRequiredClass() {
        when(licenses.findActiveByDriverId(driverId)).thenReturn(List.of(
                validLicense("C"), validLicense("B")));

        var result = evaluate("B", null);

        assertTrue(result.available());
    }

    @Test
    void matchingLicenseThatExpiresDuringRequestedPeriodIsIneligible() {
        to = from.plusDays(2);
        when(licenses.findActiveByDriverId(driverId)).thenReturn(List.of(
                license("B", from.toLocalDate().minusYears(1), from.toLocalDate().plusDays(1))));

        var result = evaluate("B", null);

        assertFalse(result.available());
        assertTrue(result.hasReason(LICENSE_EXPIRED));
        assertTrue(result.hasReason(REQUIRED_LICENSE_CLASS_MISSING));
    }

    @Test
    void nonActiveMatchingLicenseDoesNotInvalidateValidMatchingLicense() {
        when(licenses.findActiveByDriverId(driverId)).thenReturn(List.of(
                inactiveLicense("B"), validLicense("B")));

        var result = evaluate("B", null);

        assertTrue(result.available());
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
    void rejectsDriverWithOverlappingDriverException() {
        when(driverExceptions.hasOverlappingException(eq(driverId), eq(from), eq(to), anyList()))
                .thenReturn(true);

        var result = evaluate("B", null);

        assertFalse(result.available());
        assertTrue(result.hasReason(DRIVER_EXCEPTION_BLOCKED));
    }

    @Test
    void allowsDriverWhenDriverExceptionDoesNotOverlap() {
        when(driverExceptions.hasOverlappingException(eq(driverId), eq(from), eq(to), anyList()))
                .thenReturn(false);

        var result = evaluate("B", null);

        assertTrue(result.available());
        assertFalse(result.hasReason(DRIVER_EXCEPTION_BLOCKED));
    }

    @Test
    void returnsAllApplicableReasons() {
        givenDriver(false, "ON_LEAVE");
        when(licenses.findActiveByDriverId(driverId)).thenReturn(List.of());
        when(assignments.hasOverlap(driverId, from, to, null)).thenReturn(true);
        when(driverExceptions.hasOverlappingException(eq(driverId), eq(from), eq(to), anyList()))
                .thenReturn(true);

        var result = evaluate("C", null);

        assertEquals(6, result.reasons().size());
        assertTrue(result.hasReason(INACTIVE));
        assertTrue(result.hasReason(OPERATIONALLY_UNAVAILABLE));
        assertTrue(result.hasReason(LICENSE_MISSING));
        assertTrue(result.hasReason(REQUIRED_LICENSE_CLASS_MISSING));
        assertTrue(result.hasReason(OVERLAPPING_ASSIGNMENT));
        assertTrue(result.hasReason(DRIVER_EXCEPTION_BLOCKED));
    }

    @Test
    void rejectsMedicallyUnfitDriver() {
        var medicalRecords = mock(com.transportlogistics.app.fleet.application.ports.out.DriverMedicalRecordRepository.class);
        var serviceWithMedical = new DriverAvailabilityService(drivers, licenses, assignments, driverExceptions, medicalRecords, null);

        var unfitRecord = new com.transportlogistics.app.fleet.domain.model.DriverMedicalRecord(
                UUID.randomUUID(), driverId, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 1), LocalDate.of(2027, 1, 1),
                com.transportlogistics.app.fleet.domain.model.DriverMedicalStatus.UNFIT,
                com.transportlogistics.app.fleet.domain.model.VisionTestStatus.FAILED,
                "Cardiac issue", "Dr. A", "REF-1", null, true, OffsetDateTime.now(), OffsetDateTime.now(), "admin", "admin"
        );
        when(medicalRecords.findLatestByDriverId(driverId)).thenReturn(Optional.of(unfitRecord));

        var result = serviceWithMedical.evaluate(new DriverAvailabilityUseCase.Query(driverId, from, to, "B", null));
        assertFalse(result.available());
        assertTrue(result.hasReason(MEDICALLY_UNFIT));
    }

    @Test
    void rejectsDriverWithExpiredMedicalFitness() {
        var medicalRecords = mock(com.transportlogistics.app.fleet.application.ports.out.DriverMedicalRecordRepository.class);
        var serviceWithMedical = new DriverAvailabilityService(drivers, licenses, assignments, driverExceptions, medicalRecords, null);

        var expiredRecord = new com.transportlogistics.app.fleet.domain.model.DriverMedicalRecord(
                UUID.randomUUID(), driverId, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 1), from.toLocalDate().minusDays(1),
                com.transportlogistics.app.fleet.domain.model.DriverMedicalStatus.FIT,
                com.transportlogistics.app.fleet.domain.model.VisionTestStatus.PASSED,
                null, "Dr. A", "REF-2", null, true, OffsetDateTime.now(), OffsetDateTime.now(), "admin", "admin"
        );
        when(medicalRecords.findLatestByDriverId(driverId)).thenReturn(Optional.of(expiredRecord));

        var result = serviceWithMedical.evaluate(new DriverAvailabilityUseCase.Query(driverId, from, to, "B", null));
        assertFalse(result.available());
        assertTrue(result.hasReason(MEDICAL_FITNESS_EXPIRED));
    }

    @Test
    void rejectsDriverWithPositiveUnclearedDrugTest() {
        var drugTests = mock(com.transportlogistics.app.fleet.application.ports.out.DriverDrugTestRepository.class);
        var serviceWithDrugTest = new DriverAvailabilityService(drivers, licenses, assignments, driverExceptions, null, drugTests);

        var positiveTest = new com.transportlogistics.app.fleet.domain.model.DriverDrugTest(
                UUID.randomUUID(), driverId, com.transportlogistics.app.fleet.domain.model.DrugTestType.RANDOM,
                LocalDate.of(2026, 1, 1), OffsetDateTime.now(), LocalDate.of(2026, 1, 2),
                com.transportlogistics.app.fleet.domain.model.DrugTestResult.POSITIVE,
                com.transportlogistics.app.fleet.domain.model.DrugTestStatus.COMPLETED,
                "LabCorp", "REF-3", "Positive THC", true, null, true, OffsetDateTime.now(), OffsetDateTime.now(), "admin", "admin"
        );
        when(drugTests.findLatestByDriverId(driverId)).thenReturn(Optional.of(positiveTest));

        var result = serviceWithDrugTest.evaluate(new DriverAvailabilityUseCase.Query(driverId, from, to, "B", null));
        assertFalse(result.available());
        assertTrue(result.hasReason(RETURN_TO_DUTY_CLEARANCE_REQUIRED));
    }

    @Test
    void allowsDriverWithClearedDrugTest() {
        var drugTests = mock(com.transportlogistics.app.fleet.application.ports.out.DriverDrugTestRepository.class);
        var serviceWithDrugTest = new DriverAvailabilityService(drivers, licenses, assignments, driverExceptions, null, drugTests);

        var clearedTest = new com.transportlogistics.app.fleet.domain.model.DriverDrugTest(
                UUID.randomUUID(), driverId, com.transportlogistics.app.fleet.domain.model.DrugTestType.RANDOM,
                LocalDate.of(2026, 1, 1), OffsetDateTime.now(), LocalDate.of(2026, 1, 2),
                com.transportlogistics.app.fleet.domain.model.DrugTestResult.POSITIVE,
                com.transportlogistics.app.fleet.domain.model.DrugTestStatus.COMPLETED,
                "LabCorp", "REF-3", "Positive THC", true, OffsetDateTime.now(), true, OffsetDateTime.now(), OffsetDateTime.now(), "admin", "admin"
        );
        when(drugTests.findLatestByDriverId(driverId)).thenReturn(Optional.of(clearedTest));

        var result = serviceWithDrugTest.evaluate(new DriverAvailabilityUseCase.Query(driverId, from, to, "B", null));
        assertTrue(result.available());
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

    private DriverLicense inactiveLicense(String licenseClass) {
        var now = OffsetDateTime.parse("2026-01-01T00:00:00Z");
        return new DriverLicense(UUID.randomUUID(), driverId, "DL-" + UUID.randomUUID(), licenseClass,
                from.toLocalDate().minusYears(1), to.toLocalDate().plusYears(1), DriverLicenseStatus.INACTIVE,
                false, now, now, "tester", "tester");
    }
}
