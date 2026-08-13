package com.transportlogistics.app.fleet.application.service;

import com.transportlogistics.app.fleet.application.ports.in.VehicleDocumentUseCase;
import com.transportlogistics.app.fleet.application.ports.out.VehicleDocumentRepository;
import com.transportlogistics.app.fleet.application.ports.out.VehicleRepository;
import com.transportlogistics.app.fleet.domain.model.Vehicle;
import com.transportlogistics.app.fleet.domain.model.VehicleDocument;
import com.transportlogistics.app.fleet.domain.model.VehicleDocumentStatus;
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

class VehicleDocumentServiceTest {
    private VehicleRepository vehicles;
    private VehicleDocumentRepository documents;
    private VehicleDocumentService service;
    private UUID vehicleId;

    @BeforeEach
    void setUp() {
        vehicles = mock(VehicleRepository.class);
        documents = mock(VehicleDocumentRepository.class);
        service = new VehicleDocumentService(vehicles, documents);
        vehicleId = UUID.randomUUID();
        when(vehicles.findById(vehicleId)).thenReturn(Optional.of(vehicle(vehicleId)));
        when(documents.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void parentVehicleMustExist() {
        var missing = UUID.randomUUID();
        var command = createCommand("INSURANCE", "POL-1");

        assertThrows(NotFoundException.class, () -> service.create(missing, command, "alice"));
        verify(documents, never()).save(any());
    }

    @Test
    void duplicateActiveTypeAndNumberIsRejectedCaseInsensitively() {
        when(documents.activeDuplicateExists(eq(vehicleId), eq("INSURANCE"), eq("POL-1"), isNull()))
                .thenReturn(true);

        var error = assertThrows(IllegalArgumentException.class,
                () -> service.create(vehicleId, createCommand("insurance", "POL-1"), "alice"));

        assertTrue(error.getMessage().contains("already exists"));
    }

    @Test
    void deleteSoftDeletesAndPreservesAuditHistory() {
        var existing = document(VehicleDocumentStatus.ACTIVE, true);
        when(documents.findById(existing.id())).thenReturn(Optional.of(existing));

        service.delete(vehicleId, existing.id(), "bob");

        var captor = ArgumentCaptor.forClass(VehicleDocument.class);
        verify(documents).save(captor.capture());
        assertEquals(VehicleDocumentStatus.DELETED, captor.getValue().status());
        assertFalse(captor.getValue().active());
        assertEquals(existing.createdAt(), captor.getValue().createdAt());
        assertEquals("alice", captor.getValue().createdBy());
        assertEquals("bob", captor.getValue().updatedBy());
    }

    private VehicleDocumentUseCase.CreateCommand createCommand(String type, String number) {
        return new VehicleDocumentUseCase.CreateCommand(type, number, LocalDate.of(2025, 1, 1),
                LocalDate.of(2027, 1, 1), null, true, null, null);
    }

    private VehicleDocument document(VehicleDocumentStatus status, boolean active) {
        var now = OffsetDateTime.parse("2026-01-01T00:00:00Z");
        return new VehicleDocument(UUID.randomUUID(), vehicleId, "INSURANCE", "POL-1",
                LocalDate.of(2025, 1, 1), LocalDate.of(2027, 1, 1), null, true, status, active,
                now, now, "alice", "alice");
    }

    private Vehicle vehicle(UUID id) {
        return new Vehicle(id, "REG-1", null, null, UUID.randomUUID(), UUID.randomUUID(), null, null,
                2025, "COMPANY_OWNED", "AVAILABLE", null, null, 1000d, true);
    }
}
