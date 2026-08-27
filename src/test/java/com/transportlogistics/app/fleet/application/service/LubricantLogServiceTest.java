package com.transportlogistics.app.fleet.application.service;

import com.transportlogistics.app.fleet.application.ports.in.LubricantLogUseCase;
import com.transportlogistics.app.fleet.application.ports.out.LubricantLogRepository;
import com.transportlogistics.app.fleet.vehiclemaster.ports.outbound.VehicleRepository;
import com.transportlogistics.app.fleet.domain.model.FluidType;
import com.transportlogistics.app.fleet.domain.model.LubricantLog;
import com.transportlogistics.app.fleet.domain.model.MeasurementUnit;
import com.transportlogistics.app.fleet.vehiclemaster.domain.model.Vehicle;
import com.transportlogistics.app.shared.domain.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
class LubricantLogServiceTest {

    @Mock
    private VehicleRepository vehicles;

    @Mock
    private LubricantLogRepository lubricantLogs;

    private LubricantLogService service;
    private UUID vehicleId;
    private Vehicle vehicle;

    @BeforeEach
    void setUp() {
        service = new LubricantLogService(vehicles, lubricantLogs);
        vehicleId = UUID.randomUUID();
        vehicle = new Vehicle(vehicleId, "WP-CAD-1234", "VIN123", "ENG123", UUID.randomUUID(), UUID.randomUUID(),
                "Isuzu", "NPR", 2022, "COMPANY_OWNED", "AVAILABLE", 45000.0, 1500.0, 5000.0, true);
    }

    @Test
    @DisplayName("Should create lubricant log for existing vehicle")
    void shouldCreateLubricantLog() {
        when(vehicles.findByIdForUpdate(vehicleId)).thenReturn(Optional.of(vehicle));
        when(lubricantLogs.save(any(LubricantLog.class))).thenAnswer(inv -> inv.getArgument(0));

        var command = new LubricantLogUseCase.CreateCommand(
                FluidType.ENGINE_OIL,
                new BigDecimal("12.50"),
                MeasurementUnit.LITRE,
                OffsetDateTime.now(),
                45000.0,
                1500.0,
                UUID.randomUUID(),
                "Mobil",
                "REF-001",
                "Routine oil change"
        );

        var result = service.create(vehicleId, command, "mechanic");

        assertNotNull(result);
        assertEquals(vehicleId, result.vehicleId());
        assertEquals(FluidType.ENGINE_OIL, result.fluidType());
        assertEquals(new BigDecimal("12.50"), result.quantity());
        assertEquals(MeasurementUnit.LITRE, result.unit());
        assertEquals("mechanic", result.createdBy());

        verify(lubricantLogs).save(any(LubricantLog.class));
    }

    @Test
    @DisplayName("Should throw NotFoundException when vehicle not found on create")
    void shouldThrowWhenVehicleNotFoundOnCreate() {
        when(vehicles.findByIdForUpdate(vehicleId)).thenReturn(Optional.empty());

        var command = new LubricantLogUseCase.CreateCommand(
                FluidType.ENGINE_OIL,
                new BigDecimal("12.50"),
                MeasurementUnit.LITRE,
                OffsetDateTime.now(),
                null, null, null, null, null, null
        );

        assertThrows(NotFoundException.class, () -> service.create(vehicleId, command, "mechanic"));
    }

    @Test
    @DisplayName("Should list lubricant logs for vehicle")
    void shouldListLubricantLogs() {
        when(vehicles.findById(vehicleId)).thenReturn(Optional.of(vehicle));
        var log = new LubricantLog(
                UUID.randomUUID(), vehicleId, FluidType.COOLANT, new BigDecimal("4.00"), MeasurementUnit.LITRE,
                OffsetDateTime.now(), null, null, null, null, null, null, true,
                OffsetDateTime.now(), OffsetDateTime.now(), "admin", "admin"
        );
        when(lubricantLogs.findByVehicleId(vehicleId)).thenReturn(List.of(log));

        var list = service.list(vehicleId, null, null, null);

        assertEquals(1, list.size());
        assertEquals(FluidType.COOLANT, list.get(0).fluidType());
    }

    @Test
    @DisplayName("Should list lubricant logs with filters")
    void shouldListLubricantLogsWithFilters() {
        when(vehicles.findById(vehicleId)).thenReturn(Optional.of(vehicle));
        var from = OffsetDateTime.now().minusDays(7);
        var to = OffsetDateTime.now();
        var log = new LubricantLog(
                UUID.randomUUID(), vehicleId, FluidType.BRAKE_FLUID, new BigDecimal("1.00"), MeasurementUnit.LITRE,
                OffsetDateTime.now(), null, null, null, null, null, null, true,
                OffsetDateTime.now(), OffsetDateTime.now(), "admin", "admin"
        );
        when(lubricantLogs.findByVehicleIdWithFilters(vehicleId, FluidType.BRAKE_FLUID, from, to))
                .thenReturn(List.of(log));

        var list = service.list(vehicleId, FluidType.BRAKE_FLUID, from, to);

        assertEquals(1, list.size());
        assertEquals(FluidType.BRAKE_FLUID, list.get(0).fluidType());
    }

    @Test
    @DisplayName("Should get single lubricant log by id")
    void shouldGetLubricantLogById() {
        var logId = UUID.randomUUID();
        var log = new LubricantLog(
                logId, vehicleId, FluidType.GEAR_OIL, new BigDecimal("2.50"), MeasurementUnit.LITRE,
                OffsetDateTime.now(), null, null, null, null, null, null, true,
                OffsetDateTime.now(), OffsetDateTime.now(), "admin", "admin"
        );
        when(lubricantLogs.findById(logId)).thenReturn(Optional.of(log));

        var result = service.get(vehicleId, logId);

        assertEquals(logId, result.id());
        assertEquals(FluidType.GEAR_OIL, result.fluidType());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when from date is after to date")
    void shouldThrowWhenFromDateAfterToDate() {
        when(vehicles.findById(vehicleId)).thenReturn(Optional.of(vehicle));
        var from = OffsetDateTime.now();
        var to = OffsetDateTime.now().minusDays(1);

        assertThrows(IllegalArgumentException.class, () -> service.list(vehicleId, null, from, to));
    }
}
