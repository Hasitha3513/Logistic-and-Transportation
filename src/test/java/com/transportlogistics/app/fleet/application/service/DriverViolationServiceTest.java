package com.transportlogistics.app.fleet.application.service;

import com.transportlogistics.app.fleet.application.ports.in.DriverViolationUseCase;
import com.transportlogistics.app.fleet.application.ports.out.DriverRepository;
import com.transportlogistics.app.fleet.application.ports.out.DriverViolationRepository;
import com.transportlogistics.app.fleet.domain.model.*;
import com.transportlogistics.app.shared.domain.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DriverViolationServiceTest {

    @Mock
    private DriverViolationRepository violations;

    @Mock
    private DriverRepository drivers;

    private DriverViolationService service;
    private final UUID driverId = UUID.randomUUID();
    private final Driver testDriver = new Driver(driverId, "EMP-001", "John", "Doe", "555-1234", "john@test.com", "AVAILABLE", true);

    @BeforeEach
    void setUp() {
        service = new DriverViolationService(violations, drivers);
    }

    @Test
    void recordsViolationSuccessfully() {
        when(drivers.findById(driverId)).thenReturn(Optional.of(testDriver));
        when(violations.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var command = new DriverViolationUseCase.RecordCommand(
                driverId,
                null,
                DriverViolationType.SPEEDING,
                ViolationSeverity.MODERATE,
                OffsetDateTime.now().minusHours(2),
                3,
                new BigDecimal("120.00"),
                "Highway 1",
                "Speeding 15km/h over",
                "officer1"
        );

        var result = service.recordViolation(command);
        assertNotNull(result);
        assertEquals(driverId, result.driverId());
        assertEquals(DriverViolationType.SPEEDING, result.violationType());
        assertEquals(3, result.penaltyPoints());
        assertEquals(FinePaymentStatus.UNPAID, result.paymentStatus());
        verify(violations).save(any(DriverViolation.class));
    }

    @Test
    void recordViolationFailsIfDriverNotFound() {
        when(drivers.findById(driverId)).thenReturn(Optional.empty());

        var command = new DriverViolationUseCase.RecordCommand(
                driverId,
                null,
                DriverViolationType.SPEEDING,
                ViolationSeverity.MINOR,
                OffsetDateTime.now(),
                1,
                BigDecimal.TEN,
                null,
                null,
                "admin"
        );

        assertThrows(NotFoundException.class, () -> service.recordViolation(command));
        verify(violations, never()).save(any());
    }

    @Test
    void listsAndGetsViolations() {
        var violation = DriverViolation.record(driverId, null, DriverViolationType.RED_LIGHT, ViolationSeverity.MAJOR,
                OffsetDateTime.now(), 4, new BigDecimal("200.00"), "City", "Note", "admin");

        when(drivers.findById(driverId)).thenReturn(Optional.of(testDriver));
        when(violations.findByDriverId(driverId)).thenReturn(List.of(violation));
        when(violations.findById(violation.id())).thenReturn(Optional.of(violation));

        var list = service.listViolations(driverId);
        assertEquals(1, list.size());

        var fetched = service.getViolation(driverId, violation.id());
        assertEquals(violation.id(), fetched.id());
    }

    @Test
    void paysAndWaivesFine() {
        var violation = DriverViolation.record(driverId, null, DriverViolationType.LOGBOOK_VIOLATION, ViolationSeverity.MINOR,
                OffsetDateTime.now(), 2, new BigDecimal("100.00"), "Depot", "Note", "admin");

        when(drivers.findById(driverId)).thenReturn(Optional.of(testDriver));
        when(violations.findById(violation.id())).thenReturn(Optional.of(violation));
        when(violations.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var payCommand = new DriverViolationUseCase.PayCommand(driverId, violation.id(), OffsetDateTime.now(), "TXN-1234", "accountant");
        var paid = service.payFine(payCommand);

        assertEquals(FinePaymentStatus.PAID, paid.paymentStatus());
        assertEquals("TXN-1234", paid.paymentReference());
    }
}
