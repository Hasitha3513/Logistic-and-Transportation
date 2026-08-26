package com.transportlogistics.app.fleet.application.service;

import com.transportlogistics.app.fleet.application.ports.in.DriverLicenseUseCase;
import com.transportlogistics.app.fleet.application.ports.out.DriverLicenseRepository;
import com.transportlogistics.app.fleet.application.ports.out.DriverRepository;
import com.transportlogistics.app.fleet.domain.model.Driver;
import com.transportlogistics.app.fleet.domain.model.DriverLicense;
import com.transportlogistics.app.fleet.domain.model.DriverLicenseStatus;
import com.transportlogistics.app.shared.domain.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DriverLicenseServiceTest {
    private DriverRepository drivers;
    private DriverLicenseRepository licenses;
    private DriverLicenseService service;
    private UUID driverId;

    @BeforeEach
    void setUp() {
        drivers = mock(DriverRepository.class);
        licenses = mock(DriverLicenseRepository.class);
        service = new DriverLicenseService(drivers, licenses);
        driverId = UUID.randomUUID();
        when(drivers.findById(driverId)).thenReturn(Optional.of(driver(driverId)));
        when(licenses.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void driverMustExist() {
        assertThrows(NotFoundException.class,
                () -> service.create(UUID.randomUUID(), command("DL-1"), "alice"));
        verify(licenses, never()).save(any());
    }

    @Test
    void licenseNumberIsGloballyUniqueIncludingInactiveHistory() {
        when(licenses.licenseNumberExists("DL-1", null)).thenReturn(true);

        var error = assertThrows(IllegalArgumentException.class,
                () -> service.create(driverId, command("dl-1"), "alice"));

        assertEquals("License number already exists", error.getMessage());
    }

    @Test
    void deleteIsSoftAndPreservesAuditIdentity() {
        var existing = license();
        when(licenses.findById(existing.id())).thenReturn(Optional.of(existing));

        service.delete(driverId, existing.id(), "bob");

        var captor = ArgumentCaptor.forClass(DriverLicense.class);
        verify(licenses).save(captor.capture());
        assertEquals(DriverLicenseStatus.DELETED, captor.getValue().status());
        assertFalse(captor.getValue().active());
        assertEquals(existing.createdAt(), captor.getValue().createdAt());
        assertEquals("alice", captor.getValue().createdBy());
        assertEquals("bob", captor.getValue().updatedBy());
    }

    private DriverLicenseUseCase.CreateCommand command(String number) {
        return new DriverLicenseUseCase.CreateCommand(number, "B", LocalDate.of(2025, 1, 1),
                LocalDate.of(2027, 1, 1), null, null);
    }

    private DriverLicense license() {
        var now = OffsetDateTime.parse("2026-01-01T00:00:00Z");
        return new DriverLicense(UUID.randomUUID(), driverId, "DL-1", "B", LocalDate.of(2025, 1, 1),
                LocalDate.of(2027, 1, 1), DriverLicenseStatus.ACTIVE, true, now, now, "alice", "alice");
    }

    private Driver driver(UUID id) {
        return new Driver(id, "EMP-1", "Alex", "Driver", null, null, "AVAILABLE", true);
    }
}
