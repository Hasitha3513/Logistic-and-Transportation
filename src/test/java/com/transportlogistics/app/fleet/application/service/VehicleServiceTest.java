package com.transportlogistics.app.fleet.application.service;

import com.transportlogistics.app.fleet.application.ports.out.VehicleDocumentRepository;
import com.transportlogistics.app.fleet.application.ports.out.VehicleRepository;
import com.transportlogistics.app.fleet.domain.model.Vehicle;
import com.transportlogistics.app.fleet.domain.model.VehicleDocument;
import com.transportlogistics.app.fleet.domain.model.VehicleDocumentStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class VehicleServiceTest {
    @Test
    void expiredMandatoryDocumentMakesVehicleUnavailable() {
        var vehicles = mock(VehicleRepository.class);
        var documents = mock(VehicleDocumentRepository.class);
        var vehicleId = UUID.randomUUID();
        when(vehicles.findById(vehicleId)).thenReturn(Optional.of(vehicle(vehicleId)));
        when(documents.findActiveByVehicleId(vehicleId)).thenReturn(List.of(expiredDocument(vehicleId)));
        var service = new VehicleService(vehicles, documents);

        var result = service.availability(vehicleId, LocalDate.of(2026, 1, 1));

        assertFalse(result.available());
        assertEquals("MANDATORY_DOCUMENT_EXPIRED", result.reason());
        assertThrows(IllegalArgumentException.class,
                () -> service.assertAvailableForDispatch(vehicleId, LocalDate.of(2026, 1, 1)));
    }

    private Vehicle vehicle(UUID id) {
        return new Vehicle(id, "REG-1", null, null, UUID.randomUUID(), UUID.randomUUID(), null, null,
                2025, "COMPANY_OWNED", "AVAILABLE", null, null, 1000d, true);
    }

    private VehicleDocument expiredDocument(UUID vehicleId) {
        var now = OffsetDateTime.parse("2026-01-01T00:00:00Z");
        return new VehicleDocument(UUID.randomUUID(), vehicleId, "INSURANCE", "POL-1",
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31), null, true,
                VehicleDocumentStatus.ACTIVE, true, now, now, "alice", "alice");
    }
}
