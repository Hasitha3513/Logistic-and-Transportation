package com.transportlogistics.app.fleet.application.service;

import com.transportlogistics.app.fleet.VehicleAllocationAvailability;
import com.transportlogistics.app.fleet.application.ports.in.VehicleAvailabilityUseCase;
import com.transportlogistics.app.fleet.application.ports.out.VehicleDocumentRepository;
import com.transportlogistics.app.fleet.application.ports.out.VehicleRepository;
import com.transportlogistics.app.fleet.domain.model.Vehicle;
import com.transportlogistics.app.fleet.domain.model.VehicleAvailability;
import com.transportlogistics.app.fleet.domain.model.VehicleDocument;
import com.transportlogistics.app.fleet.domain.model.VehicleDocumentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.transportlogistics.app.fleet.domain.model.VehicleAvailability.Code.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class VehicleAvailabilityServiceTest {
    private VehicleRepository vehicles;
    private VehicleDocumentRepository documents;
    private VehicleAllocationAvailability allocations;
    private VehicleAvailabilityService service;
    private UUID vehicleId;
    private UUID typeId;
    private OffsetDateTime from;
    private OffsetDateTime to;

    @BeforeEach
    void setUp() {
        vehicles = mock(VehicleRepository.class);
        documents = mock(VehicleDocumentRepository.class);
        allocations = mock(VehicleAllocationAvailability.class);
        service = new VehicleAvailabilityService(vehicles, documents, allocations);
        vehicleId = UUID.randomUUID();
        typeId = UUID.randomUUID();
        from = OffsetDateTime.parse("2026-02-01T08:00:00Z");
        to = OffsetDateTime.parse("2026-02-01T10:00:00Z");
        givenVehicle(vehicle(true, "AVAILABLE", typeId, 1000d));
        when(documents.findVisibleByVehicleId(vehicleId)).thenReturn(List.of());
    }

    @Test
    void eligibleVehicleHasNoReasons() {
        var result = evaluate(typeId, 900d);
        assertTrue(result.available());
        assertTrue(result.reasons().isEmpty());
    }

    @Test
    void rejectsInactiveVehicle() {
        givenVehicle(vehicle(false, "AVAILABLE", typeId, 1000d));
        assertRejected(INACTIVE);
    }

    @Test
    void rejectsGenericOperationallyUnavailableStatus() {
        givenVehicle(vehicle(true, "IN_USE", typeId, 1000d));
        assertRejected(OPERATIONALLY_UNAVAILABLE);
    }

    @Test
    void rejectsBrokenDownVehicle() {
        givenVehicle(vehicle(true, "BROKEN_DOWN", typeId, 1000d));
        assertRejected(BROKEN_DOWN);
    }

    @Test
    void rejectsOutOfServiceVehicle() {
        givenVehicle(vehicle(true, "OUT_OF_SERVICE", typeId, 1000d));
        assertRejected(OUT_OF_SERVICE);
    }

    @Test
    void rejectsMaintenanceBlockedVehicle() {
        givenVehicle(vehicle(true, "UNDER_MAINTENANCE", typeId, 1000d));
        assertRejected(MAINTENANCE_BLOCKED);
    }

    @Test
    void rejectsExpiredMandatoryDocument() {
        when(documents.findVisibleByVehicleId(vehicleId)).thenReturn(List.of(expiredMandatoryDocument()));
        assertRejected(MANDATORY_DOCUMENT_EXPIRED);
    }

    @Test
    void rejectsInactiveMandatoryDocument() {
        when(documents.findVisibleByVehicleId(vehicleId)).thenReturn(List.of(inactiveMandatoryDocument()));
        assertRejected(MANDATORY_DOCUMENT_INVALID);
    }

    @Test
    void rejectsVehicleTypeMismatch() {
        var result = evaluate(UUID.randomUUID(), null);
        assertFalse(result.available());
        assertTrue(result.hasReason(VEHICLE_TYPE_MISMATCH));
    }

    @Test
    void rejectsInsufficientOrUnknownCapacity() {
        var insufficient = evaluate(null, 1001d);
        assertTrue(insufficient.hasReason(INSUFFICIENT_CAPACITY));

        givenVehicle(vehicle(true, "AVAILABLE", typeId, null));
        assertTrue(evaluate(null, 1d).hasReason(INSUFFICIENT_CAPACITY));
    }

    @Test
    void rejectsOverlappingAllocationAndPassesExclusionToTripLookup() {
        var excludeTripId = UUID.randomUUID();
        when(allocations.hasOverlap(vehicleId, from, to, excludeTripId)).thenReturn(true);

        var result = service.evaluate(new VehicleAvailabilityUseCase.Query(vehicleId, from, to, null, null,
                excludeTripId));

        assertTrue(result.hasReason(OVERLAPPING_ALLOCATION));
        verify(allocations).hasOverlap(vehicleId, from, to, excludeTripId);
    }

    @Test
    void returnsEveryApplicableReasonInsteadOfStoppingAtFirst() {
        givenVehicle(vehicle(false, "BROKEN_DOWN", UUID.randomUUID(), 100d));
        when(documents.findVisibleByVehicleId(vehicleId)).thenReturn(List.of(expiredMandatoryDocument()));
        when(allocations.hasOverlap(eq(vehicleId), any(), any(), isNull())).thenReturn(true);

        var result = evaluate(typeId, 1000d);

        assertEquals(6, result.reasons().size());
        assertTrue(result.hasReason(INACTIVE));
        assertTrue(result.hasReason(BROKEN_DOWN));
        assertTrue(result.hasReason(MANDATORY_DOCUMENT_EXPIRED));
        assertTrue(result.hasReason(VEHICLE_TYPE_MISMATCH));
        assertTrue(result.hasReason(INSUFFICIENT_CAPACITY));
        assertTrue(result.hasReason(OVERLAPPING_ALLOCATION));
    }

    private void assertRejected(VehicleAvailability.Code code) {
        var result = evaluate(null, null);
        assertFalse(result.available());
        assertTrue(result.hasReason(code));
    }

    private VehicleAvailability evaluate(UUID requiredType, Double requiredCapacity) {
        return service.evaluate(new VehicleAvailabilityUseCase.Query(vehicleId, from, to, requiredType,
                requiredCapacity, null));
    }

    private void givenVehicle(Vehicle vehicle) {
        when(vehicles.findById(vehicleId)).thenReturn(Optional.of(vehicle));
    }

    private Vehicle vehicle(boolean active, String status, UUID vehicleTypeId, Double capacity) {
        return new Vehicle(vehicleId, "REG-1", null, null, UUID.randomUUID(), vehicleTypeId, null, null,
                2025, "COMPANY_OWNED", status, null, null, capacity, active);
    }

    private VehicleDocument expiredMandatoryDocument() {
        var now = OffsetDateTime.parse("2026-01-01T00:00:00Z");
        return new VehicleDocument(UUID.randomUUID(), vehicleId, "INSURANCE", "POL-1",
                LocalDate.of(2025, 1, 1), LocalDate.of(2026, 1, 31), null, true,
                VehicleDocumentStatus.ACTIVE, true, now, now, "alice", "alice");
    }

    private VehicleDocument inactiveMandatoryDocument() {
        var now = OffsetDateTime.parse("2026-01-01T00:00:00Z");
        return new VehicleDocument(UUID.randomUUID(), vehicleId, "INSURANCE", "POL-2",
                LocalDate.of(2025, 1, 1), LocalDate.of(2027, 1, 1), null, true,
                VehicleDocumentStatus.INACTIVE, false, now, now, "alice", "alice");
    }
}
