package com.transportlogistics.app.fleet.application.service;

import com.transportlogistics.app.fleet.application.ports.out.*;
import com.transportlogistics.app.fleet.domain.model.*;
import com.transportlogistics.app.notification.OperationalNotificationEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.*;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ComplianceNotificationScannerTest {
    private final OffsetDateTime now = OffsetDateTime.parse("2026-08-21T20:00:00Z");
    private final LocalDate businessDate = LocalDate.of(2026, 8, 22);
    private final Clock clock = Clock.fixed(now.toInstant(), ZoneOffset.UTC);
    private final ZoneId zone = ZoneId.of("Asia/Colombo");
    private VehicleDocumentRepository documents;
    private VehicleRepository vehicles;
    private DriverMedicalRecordRepository medical;
    private DriverLicenseRepository licenses;
    private DriverRepository drivers;
    private FleetOperationalNotificationPublisher publisher;
    private UUID vehicleId;
    private UUID driverId;

    @BeforeEach void setUp() {
        documents = mock(VehicleDocumentRepository.class); vehicles = mock(VehicleRepository.class);
        medical = mock(DriverMedicalRecordRepository.class); licenses = mock(DriverLicenseRepository.class);
        drivers = mock(DriverRepository.class); publisher = mock(FleetOperationalNotificationPublisher.class);
        vehicleId = UUID.randomUUID(); driverId = UUID.randomUUID();
        when(vehicles.findById(vehicleId)).thenReturn(Optional.of(vehicle()));
        when(drivers.findById(driverId)).thenReturn(Optional.of(driver()));
    }

    @Test void publishesThreeComplianceFamiliesWithStableRepeatedMilestonesAndBusinessTimezone() {
        when(documents.findActiveMandatoryExpiringBy(businessDate.plusDays(30))).thenReturn(List.of(document(true, true)));
        when(medical.findActiveFitExpiringBy(businessDate.plusDays(30))).thenReturn(List.of(medical(true, DriverMedicalStatus.FIT)));
        when(licenses.findActiveExpiringBy(businessDate.plusDays(30))).thenReturn(List.of(license(true)));
        var scanner = scanner();

        scanner.scan(); scanner.scan();

        var captor = ArgumentCaptor.forClass(OperationalNotificationEvent.class);
        verify(publisher, times(6)).publish(captor.capture());
        var values = captor.getAllValues();
        assertThat(values.subList(0, 3)).extracting(OperationalNotificationEvent::eventType)
            .containsExactly("VEHICLE_DOCUMENT_EXPIRING", "DRIVER_MEDICAL_EXPIRING", "DRIVER_LICENSE_EXPIRING");
        assertThat(values.subList(0, 3)).extracting(OperationalNotificationEvent::eventId)
            .containsExactlyElementsOf(values.subList(3, 6).stream().map(OperationalNotificationEvent::eventId).toList());
        assertThat(values).allSatisfy(event -> assertThat(event.metadata())
            .containsEntry("milestone", "D30").containsEntry("catalogueMilestone", "D30"));
    }

    @Test void excludesInactiveNonMandatoryAndUnfitCandidatesDefensively() {
        when(documents.findActiveMandatoryExpiringBy(any())).thenReturn(List.of(document(false, true), document(true, false)));
        when(medical.findActiveFitExpiringBy(any())).thenReturn(List.of(medical(false, DriverMedicalStatus.FIT),
            medical(true, DriverMedicalStatus.UNFIT)));
        when(licenses.findActiveExpiringBy(any())).thenReturn(List.of(license(false)));

        scanner().scan();

        verifyNoInteractions(publisher);
    }

    @Test void expiredMilestoneIsCriticalAndOneFailureDoesNotAbortOtherFamilies() {
        when(documents.findActiveMandatoryExpiringBy(any())).thenReturn(List.of(document(true, true, businessDate)));
        when(medical.findActiveFitExpiringBy(any())).thenReturn(List.of(medical(true, DriverMedicalStatus.FIT, businessDate)));
        when(licenses.findActiveExpiringBy(any())).thenReturn(List.of(license(true, businessDate)));
        doThrow(new IllegalStateException("document failed")).doNothing().doNothing().when(publisher).publish(any());

        scanner().scan();

        var captor = ArgumentCaptor.forClass(OperationalNotificationEvent.class);
        verify(publisher, times(3)).publish(captor.capture());
        assertThat(captor.getAllValues()).allSatisfy(event -> {
            assertThat(event.severity()).isEqualTo(OperationalNotificationEvent.Severity.CRITICAL);
            assertThat(event.metadata()).containsEntry("milestone", "EXPIRED");
        });
    }

    private ComplianceNotificationScanner scanner() {
        return new ComplianceNotificationScanner(documents, vehicles, medical, licenses, drivers,
            publisher, clock, zone);
    }

    private VehicleDocument document(boolean active, boolean mandatory) {
        return document(active, mandatory, businessDate.plusDays(20));
    }

    private VehicleDocument document(boolean active, boolean mandatory, LocalDate expiry) {
        return new VehicleDocument(UUID.randomUUID(), vehicleId, "INSURANCE", "DOC-1",
            businessDate.minusYears(1), expiry, null, mandatory,
            active ? VehicleDocumentStatus.ACTIVE : VehicleDocumentStatus.INACTIVE, active,
            now, now, "a", "a");
    }

    private DriverMedicalRecord medical(boolean active, DriverMedicalStatus status) {
        return medical(active, status, businessDate.plusDays(20));
    }

    private DriverMedicalRecord medical(boolean active, DriverMedicalStatus status, LocalDate validUntil) {
        return new DriverMedicalRecord(UUID.randomUUID(), driverId, businessDate.minusDays(1),
            businessDate.minusYears(1), validUntil, status, VisionTestStatus.PASSED, null, null, null,
            null, active, now, now, "a", "a");
    }

    private DriverLicense license(boolean active) { return license(active, businessDate.plusDays(20)); }

    private DriverLicense license(boolean active, LocalDate expiry) {
        return new DriverLicense(UUID.randomUUID(), driverId, "B123", "B", businessDate.minusYears(1),
            expiry, active ? DriverLicenseStatus.ACTIVE : DriverLicenseStatus.INACTIVE, active,
            now, now, "a", "a");
    }

    private Vehicle vehicle() {
        return new Vehicle(vehicleId, "WP-ABC-1234", null, null, null, null, null, null, null, null,
            "AVAILABLE", null, null, null, true);
    }

    private Driver driver() {
        return new Driver(driverId, "EMP-1", "Jane", "Doe", null, null, "AVAILABLE", true);
    }
}
