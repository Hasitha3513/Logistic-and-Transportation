package com.transportlogistics.app.fleet.application.service;

import com.transportlogistics.app.fleet.domain.model.*;
import com.transportlogistics.app.fleet.vehiclemaster.domain.model.Vehicle;
import com.transportlogistics.app.notification.OperationalNotificationEvent;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FleetOperationalNotificationEventsTest {
    private final OffsetDateTime now = OffsetDateTime.parse("2026-08-22T00:00:00Z");
    private final LocalDate today = LocalDate.of(2026, 8, 22);
    private final Vehicle vehicle = new Vehicle(UUID.randomUUID(), "WP-ABC-1234", "C", "E", null,
        null, "Maker", "Model", 2024, "OWNED", "AVAILABLE", 1d, 1d, 1000d, true);
    private final Driver driver = new Driver(UUID.randomUUID(), "EMP-1", "Jane", "Doe", "1",
        "jane@example.test", "AVAILABLE", true);

    @Test void maintenanceUsesStableScheduleStartMilestoneAndRequiredMetadata() {
        UUID id = UUID.randomUUID();
        var schedule = maintenance(id, now.plusHours(12));
        var first = FleetOperationalNotificationEvents.maintenanceDue(schedule, vehicle, now);
        var repeated = FleetOperationalNotificationEvents.maintenanceDue(schedule, vehicle, now.plusMinutes(30));
        var changed = FleetOperationalNotificationEvents.maintenanceDue(maintenance(id, now.plusHours(13)), vehicle, now);

        assertThat(first.eventId()).isEqualTo(repeated.eventId()).isNotEqualTo(changed.eventId());
        assertEvent(first, "VEHICLE_MAINTENANCE_DUE", OperationalNotificationEvent.Severity.WARNING,
            "vehicleId", "vehicleRegistration", "maintenanceType", "scheduledStart", "scheduledEnd", "milestone");
    }

    @Test void documentMilestonesAreDistinctAndMapWarningThenCritical() {
        var document = new VehicleDocument(UUID.randomUUID(), vehicle.id(), "INSURANCE", "DOC-1",
            today.minusYears(1), today.plusDays(20), null, true, VehicleDocumentStatus.ACTIVE, true,
            now, now, "admin", "admin");
        var d30 = FleetOperationalNotificationEvents.vehicleDocument(document, vehicle, "D30", now);
        var repeated = FleetOperationalNotificationEvents.vehicleDocument(document, vehicle, "D30", now.plusHours(1));
        var expired = FleetOperationalNotificationEvents.vehicleDocument(document, vehicle, "EXPIRED", now);

        assertThat(d30.eventId()).isEqualTo(repeated.eventId()).isNotEqualTo(expired.eventId());
        assertEvent(d30, "VEHICLE_DOCUMENT_EXPIRING", OperationalNotificationEvent.Severity.WARNING,
            "vehicleId", "vehicleRegistration", "documentId", "documentType", "documentNumber", "expiryDate", "milestone");
        assertThat(expired.severity()).isEqualTo(OperationalNotificationEvent.Severity.CRITICAL);
    }

    @Test void exceptionUsesActualCriticalCategoriesAndCompleteMetadata() {
        var exception = new DriverException(UUID.randomUUID(), driver.id(), DriverExceptionType.MEDICAL_EMERGENCY,
            now, now.plusHours(2), DriverExceptionStatus.SCHEDULED, "Emergency", null, now, now, "a", "a");
        var event = FleetOperationalNotificationEvents.driverException(exception, driver, "BLOCKING_SCHEDULED", now);
        assertEvent(event, "DRIVER_EXCEPTION_RECORDED", OperationalNotificationEvent.Severity.CRITICAL,
            "driverId", "driverName", "exceptionId", "exceptionType", "startTime", "endTime", "reason");
        assertThat(event.eventId()).isEqualTo(FleetOperationalNotificationEvents.driverException(
            exception, driver, "BLOCKING_SCHEDULED", now.plusMinutes(1)).eventId());
    }

    @Test void medicalDrugTestAndLicenseMatchFrozenMetadataAndSeverity() {
        var medical = new DriverMedicalRecord(UUID.randomUUID(), driver.id(), today, today.minusYears(1),
            today.plusDays(30), DriverMedicalStatus.FIT, VisionTestStatus.PASSED, null, null, null, null,
            true, now, now, "a", "a");
        var medicalEvent = FleetOperationalNotificationEvents.driverMedical(medical, driver, "D30", now);
        assertEvent(medicalEvent, "DRIVER_MEDICAL_EXPIRING", OperationalNotificationEvent.Severity.WARNING,
            "driverId", "driverName", "medicalRecordId", "validUntil", "fitnessStatus", "milestone");

        var drug = new DriverDrugTest(UUID.randomUUID(), driver.id(), DrugTestType.RANDOM, today, now,
            today, DrugTestResult.POSITIVE, DrugTestStatus.COMPLETED, null, null, null, true, null, true,
            now, now, "a", "a");
        var drugEvent = FleetOperationalNotificationEvents.drugTestFailed(drug, driver, now);
        assertEvent(drugEvent, "DRIVER_DRUG_TEST_FAILED", OperationalNotificationEvent.Severity.CRITICAL,
            "driverId", "driverName", "drugTestId", "resultDate", "testType");

        var license = new DriverLicense(UUID.randomUUID(), driver.id(), "B123", "B", today.minusYears(1),
            today, DriverLicenseStatus.ACTIVE, true, now, now, "a", "a");
        var licenseEvent = FleetOperationalNotificationEvents.driverLicense(license, driver, "EXPIRED", now);
        assertEvent(licenseEvent, "DRIVER_LICENSE_EXPIRING", OperationalNotificationEvent.Severity.CRITICAL,
            "driverId", "driverName", "licenseId", "licenseNumber", "licenseClass", "expiryDate", "milestone");
    }

    private MaintenanceSchedule maintenance(UUID id, OffsetDateTime start) {
        return new MaintenanceSchedule(id, vehicle.id(), "SERVICE", start, start.plusHours(2),
            MaintenanceStatus.SCHEDULED, null, null, null, now, now, "a", "a");
    }

    private void assertEvent(OperationalNotificationEvent event, String type,
                             OperationalNotificationEvent.Severity severity, String... keys) {
        assertThat(event.eventType()).isEqualTo(type);
        assertThat(event.severity()).isEqualTo(severity);
        assertThat(event.aggregateId()).isNotNull();
        assertThat(event.occurredAt()).isNotNull();
        assertThat(event.metadata().keySet()).containsAll(Set.of(keys));
        assertThat(event.metadata().values()).allSatisfy(value -> assertThat(value).isNotBlank());
    }
}
