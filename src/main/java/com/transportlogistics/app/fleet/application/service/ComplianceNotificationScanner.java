package com.transportlogistics.app.fleet.application.service;

import com.transportlogistics.app.fleet.application.ports.out.*;
import com.transportlogistics.app.fleet.vehiclemaster.ports.outbound.VehicleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;

public class ComplianceNotificationScanner {
    private static final Logger log = LoggerFactory.getLogger(ComplianceNotificationScanner.class);
    private final VehicleDocumentRepository documents;
    private final VehicleRepository vehicles;
    private final DriverMedicalRecordRepository medicalRecords;
    private final DriverLicenseRepository licenses;
    private final DriverRepository drivers;
    private final FleetOperationalNotificationPublisher publisher;
    private final Clock clock;
    private final ZoneId businessZone;

    public ComplianceNotificationScanner(VehicleDocumentRepository documents, VehicleRepository vehicles,
                                         DriverMedicalRecordRepository medicalRecords,
                                         DriverLicenseRepository licenses, DriverRepository drivers,
                                         FleetOperationalNotificationPublisher publisher, Clock clock,
                                         ZoneId businessZone) {
        this.documents = documents;
        this.vehicles = vehicles;
        this.medicalRecords = medicalRecords;
        this.licenses = licenses;
        this.drivers = drivers;
        this.publisher = publisher;
        this.clock = clock;
        this.businessZone = businessZone;
    }

    public void scan() {
        LocalDate businessDate = LocalDate.now(clock.withZone(businessZone));
        LocalDate cutoff = businessDate.plusDays(30);
        OffsetDateTime occurredAt = OffsetDateTime.now(clock);
        scanDocuments(businessDate, cutoff, occurredAt);
        scanMedical(businessDate, cutoff, occurredAt);
        scanLicenses(businessDate, cutoff, occurredAt);
    }

    private void scanDocuments(LocalDate businessDate, LocalDate cutoff, OffsetDateTime occurredAt) {
        for (var document : documents.findActiveMandatoryExpiringBy(cutoff)) {
            if (!document.active() || !document.mandatoryForDispatch() || document.expiryDate() == null
                || document.expiryDate().isAfter(cutoff)) continue;
            try {
                vehicles.findById(document.vehicleId()).ifPresent(vehicle -> publisher.publish(
                    FleetOperationalNotificationEvents.vehicleDocument(document, vehicle,
                        FleetOperationalNotificationEvents.expiryMilestone(document.expiryDate(), businessDate),
                        occurredAt)));
            } catch (RuntimeException exception) {
                log.error("Vehicle document notification publication failed for document {}", document.id(), exception);
            }
        }
    }

    private void scanMedical(LocalDate businessDate, LocalDate cutoff, OffsetDateTime occurredAt) {
        for (var record : medicalRecords.findActiveFitExpiringBy(cutoff)) {
            if (!record.active() || !record.isFit() || record.validUntil().isAfter(cutoff)) continue;
            try {
                drivers.findById(record.driverId()).ifPresent(driver -> publisher.publish(
                    FleetOperationalNotificationEvents.driverMedical(record, driver,
                        FleetOperationalNotificationEvents.expiryMilestone(record.validUntil(), businessDate),
                        occurredAt)));
            } catch (RuntimeException exception) {
                log.error("Driver medical notification publication failed for record {}", record.id(), exception);
            }
        }
    }

    private void scanLicenses(LocalDate businessDate, LocalDate cutoff, OffsetDateTime occurredAt) {
        for (var license : licenses.findActiveExpiringBy(cutoff)) {
            if (!license.isActiveForAssignment() || license.expiryDate().isAfter(cutoff)) continue;
            try {
                drivers.findById(license.driverId()).ifPresent(driver -> publisher.publish(
                    FleetOperationalNotificationEvents.driverLicense(license, driver,
                        FleetOperationalNotificationEvents.expiryMilestone(license.expiryDate(), businessDate),
                        occurredAt)));
            } catch (RuntimeException exception) {
                log.error("Driver licence notification publication failed for licence {}", license.id(), exception);
            }
        }
    }
}
