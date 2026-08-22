package com.transportlogistics.app.fleet.application.service;

import com.transportlogistics.app.fleet.domain.model.*;
import com.transportlogistics.app.notification.OperationalNotificationEvent;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

final class FleetOperationalNotificationEvents {
    static final String D30 = "D30";
    static final String EXPIRED = "EXPIRED";
    static final String MAINTENANCE_DUE_24H = "DUE_24H";

    private FleetOperationalNotificationEvents() {}

    static OperationalNotificationEvent maintenanceDue(MaintenanceSchedule schedule, Vehicle vehicle,
                                                        OffsetDateTime occurredAt) {
        String milestone = MAINTENANCE_DUE_24H;
        return event("VEHICLE_MAINTENANCE_DUE", schedule.id(),
            schedule.scheduledStart() + "|" + milestone, "VEHICLE", vehicle.id(),
            OperationalNotificationEvent.Severity.WARNING, "Vehicle maintenance due",
            "Scheduled maintenance is due within 24 hours", occurredAt,
            metadata(
                "vehicleId", vehicle.id().toString(),
                "vehicleRegistration", vehicle.registrationNumber(),
                "maintenanceType", schedule.maintenanceType(),
                "scheduledStart", schedule.scheduledStart().toString(),
                "scheduledEnd", schedule.scheduledEnd().toString(),
                "milestone", milestone,
                "catalogueMilestone", milestone));
    }

    static OperationalNotificationEvent vehicleDocument(VehicleDocument document, Vehicle vehicle,
                                                         String milestone, OffsetDateTime occurredAt) {
        var severity = EXPIRED.equals(milestone)
            ? OperationalNotificationEvent.Severity.CRITICAL
            : OperationalNotificationEvent.Severity.WARNING;
        return event("VEHICLE_DOCUMENT_EXPIRING", document.id(), document.expiryDate() + "|" + milestone,
            "VEHICLE", vehicle.id(), severity, "Vehicle document expiring",
            "Mandatory vehicle document requires compliance attention", occurredAt,
            metadata(
                "vehicleId", vehicle.id().toString(),
                "vehicleRegistration", vehicle.registrationNumber(),
                "documentId", document.id().toString(),
                "documentType", document.documentType(),
                "documentNumber", document.documentNumber(),
                "expiryDate", document.expiryDate().toString(),
                "milestone", milestone,
                "catalogueMilestone", milestone));
    }

    static OperationalNotificationEvent driverException(DriverException exception, Driver driver,
                                                         String transition, OffsetDateTime occurredAt) {
        var severity = exception.exceptionType() == DriverExceptionType.DISCIPLINARY_SUSPENSION
            || exception.exceptionType() == DriverExceptionType.MEDICAL_EMERGENCY
            ? OperationalNotificationEvent.Severity.CRITICAL
            : OperationalNotificationEvent.Severity.WARNING;
        Map<String, String> values = metadata(
            "driverId", driver.id().toString(),
            "driverName", driverName(driver),
            "exceptionId", exception.id().toString(),
            "exceptionType", exception.exceptionType().name(),
            "startTime", exception.startTime().toString(),
            "endTime", exception.endTime().toString(),
            "transition", transition);
        if (exception.reason() != null && !exception.reason().isBlank()) values.put("reason", exception.reason().trim());
        return event("DRIVER_EXCEPTION_RECORDED", exception.id(), transition, "DRIVER", driver.id(), severity,
            "Blocking driver exception recorded", "Driver availability has been blocked", occurredAt, values);
    }

    static OperationalNotificationEvent driverMedical(DriverMedicalRecord record, Driver driver,
                                                       String milestone, OffsetDateTime occurredAt) {
        var severity = EXPIRED.equals(milestone)
            ? OperationalNotificationEvent.Severity.CRITICAL
            : OperationalNotificationEvent.Severity.WARNING;
        return event("DRIVER_MEDICAL_EXPIRING", record.id(), record.validUntil() + "|" + milestone,
            "DRIVER", driver.id(), severity, "Driver medical fitness expiring",
            "Driver medical fitness record requires compliance attention", occurredAt,
            metadata(
                "driverId", driver.id().toString(),
                "driverName", driverName(driver),
                "medicalRecordId", record.id().toString(),
                "validUntil", record.validUntil().toString(),
                "fitnessStatus", record.fitnessStatus().name(),
                "milestone", milestone,
                "catalogueMilestone", milestone));
    }

    static OperationalNotificationEvent drugTestFailed(DriverDrugTest test, Driver driver,
                                                       OffsetDateTime occurredAt) {
        String transition = "BLOCKING_POSITIVE";
        return event("DRIVER_DRUG_TEST_FAILED", test.id(), transition, "DRIVER", driver.id(),
            OperationalNotificationEvent.Severity.CRITICAL, "Driver drug test failed",
            "A positive driver drug-test result is blocking availability", occurredAt,
            metadata(
                "driverId", driver.id().toString(),
                "driverName", driverName(driver),
                "drugTestId", test.id().toString(),
                "resultDate", test.resultDate().toString(),
                "testType", test.testType().name(),
                "transition", transition));
    }

    static OperationalNotificationEvent driverLicense(DriverLicense license, Driver driver,
                                                       String milestone, OffsetDateTime occurredAt) {
        var severity = EXPIRED.equals(milestone)
            ? OperationalNotificationEvent.Severity.CRITICAL
            : OperationalNotificationEvent.Severity.WARNING;
        return event("DRIVER_LICENSE_EXPIRING", license.id(), license.expiryDate() + "|" + milestone,
            "DRIVER", driver.id(), severity, "Driver licence expiring",
            "Driver licence requires compliance attention", occurredAt,
            metadata(
                "driverId", driver.id().toString(),
                "driverName", driverName(driver),
                "licenseId", license.id().toString(),
                "licenseNumber", license.licenseNumber(),
                "licenseClass", license.licenseClass(),
                "expiryDate", license.expiryDate().toString(),
                "milestone", milestone,
                "catalogueMilestone", milestone));
    }

    static String expiryMilestone(LocalDate expiryDate, LocalDate businessDate) {
        return expiryDate.isAfter(businessDate) ? D30 : EXPIRED;
    }

    static UUID stableId(String eventType, UUID sourceId, String milestone) {
        String value = eventType + "|" + sourceId + "|" + milestone;
        return UUID.nameUUIDFromBytes(value.getBytes(StandardCharsets.UTF_8));
    }

    private static OperationalNotificationEvent event(String type, UUID sourceId, String milestone,
                                                      String aggregateType, UUID aggregateId,
                                                      OperationalNotificationEvent.Severity severity,
                                                      String title, String message, OffsetDateTime occurredAt,
                                                      Map<String, String> metadata) {
        return new OperationalNotificationEvent(stableId(type, sourceId, milestone), type, aggregateType,
            aggregateId, severity, title, message, occurredAt, metadata);
    }

    private static String driverName(Driver driver) {
        String name = ((driver.firstName() == null ? "" : driver.firstName()) + " "
            + (driver.lastName() == null ? "" : driver.lastName())).trim();
        return name.isBlank() ? driver.employeeNumber() : name;
    }

    private static Map<String, String> metadata(String... pairs) {
        Map<String, String> result = new LinkedHashMap<>();
        for (int index = 0; index < pairs.length; index += 2) {
            String value = pairs[index + 1];
            if (value != null) result.put(pairs[index], value);
        }
        return result;
    }
}
