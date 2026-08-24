package com.transportlogistics.app.fleet.application.service;

import com.transportlogistics.app.fleet.application.ports.in.DriverMedicalRecordUseCase;
import com.transportlogistics.app.fleet.application.ports.out.DriverMedicalRecordRepository;
import com.transportlogistics.app.fleet.application.ports.out.DriverRepository;
import com.transportlogistics.app.fleet.domain.model.Driver;
import com.transportlogistics.app.fleet.domain.model.DriverMedicalRecord;
import com.transportlogistics.app.fleet.domain.model.DriverMedicalStatus;
import com.transportlogistics.app.fleet.domain.model.VisionTestStatus;
import com.transportlogistics.app.shared.domain.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DriverMedicalRecordServiceTest {

    @Mock
    private DriverRepository drivers;

    @Mock
    private DriverMedicalRecordRepository medicalRecords;

    private DriverMedicalRecordService service;
    private UUID driverId;
    private Driver driver;

    @BeforeEach
    void setUp() {
        service = new DriverMedicalRecordService(drivers, medicalRecords);
        driverId = UUID.randomUUID();
        driver = new Driver(driverId, "EMP-001", "John", "Doe", "+1234567890", "john@example.com", "AVAILABLE", true);
    }

    @Test
    @DisplayName("Should create medical record for existing driver")
    void shouldCreateMedicalRecord() {
        when(drivers.findByIdForUpdate(driverId)).thenReturn(Optional.of(driver));
        when(medicalRecords.save(any(DriverMedicalRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        var command = new DriverMedicalRecordUseCase.CreateCommand(
                LocalDate.of(2026, 1, 15),
                LocalDate.of(2026, 1, 15),
                LocalDate.of(2027, 1, 15),
                DriverMedicalStatus.FIT,
                VisionTestStatus.PASSED,
                null,
                "Dr. Clinic",
                "CERT-001",
                "Fit"
        );

        var result = service.create(driverId, command, "admin");

        assertNotNull(result);
        assertEquals(driverId, result.driverId());
        assertEquals(DriverMedicalStatus.FIT, result.fitnessStatus());
        assertEquals("admin", result.createdBy());

        verify(medicalRecords).save(any(DriverMedicalRecord.class));
    }

    @Test
    @DisplayName("Should throw NotFoundException when driver does not exist on create")
    void shouldThrowWhenDriverNotFoundOnCreate() {
        when(drivers.findByIdForUpdate(driverId)).thenReturn(Optional.empty());

        var command = new DriverMedicalRecordUseCase.CreateCommand(
                LocalDate.of(2026, 1, 15),
                LocalDate.of(2026, 1, 15),
                LocalDate.of(2027, 1, 15),
                DriverMedicalStatus.FIT,
                VisionTestStatus.PASSED,
                null,
                "Dr. Clinic",
                "CERT-001",
                null
        );

        assertThrows(NotFoundException.class, () -> service.create(driverId, command, "admin"));
    }

    @Test
    @DisplayName("Should list medical records for driver")
    void shouldListMedicalRecords() {
        when(drivers.findById(driverId)).thenReturn(Optional.of(driver));
        var record = new DriverMedicalRecord(
                UUID.randomUUID(), driverId, LocalDate.of(2026, 1, 15), LocalDate.of(2026, 1, 15), LocalDate.of(2027, 1, 15),
                DriverMedicalStatus.FIT, VisionTestStatus.PASSED, null, "Dr. Clinic", "CERT-001", "Fit",
                true, java.time.OffsetDateTime.now(), java.time.OffsetDateTime.now(), "admin", "admin"
        );
        when(medicalRecords.findByDriverId(driverId)).thenReturn(List.of(record));

        var list = service.list(driverId);

        assertEquals(1, list.size());
        verify(medicalRecords).findByDriverId(driverId);
    }
}
