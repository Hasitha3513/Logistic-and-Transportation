package com.transportlogistics.app.fleet.infrastructure.adapters.in.web.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transportlogistics.app.fleet.application.ports.in.DriverExceptionUseCase;
import com.transportlogistics.app.fleet.domain.model.DriverException;
import com.transportlogistics.app.fleet.domain.model.DriverExceptionStatus;
import com.transportlogistics.app.fleet.domain.model.DriverExceptionType;
import com.transportlogistics.app.fleet.infrastructure.adapters.in.web.dto.request.DriverExceptionActionRequest;
import com.transportlogistics.app.fleet.infrastructure.adapters.in.web.dto.request.DriverExceptionRequest;
import com.transportlogistics.app.fleet.infrastructure.adapters.in.web.mappers.FleetWebMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class FleetControllerDriverExceptionTest {

    private MockMvc mockMvc;
    private DriverExceptionUseCase driverExceptions;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final UUID driverId = UUID.randomUUID();
    private final UUID exceptionId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        driverExceptions = mock(DriverExceptionUseCase.class);
        var mapper = Mappers.getMapper(FleetWebMapper.class);

        var controller = new FleetController(
                mock(com.transportlogistics.app.fleet.application.ports.in.DriverUseCase.class),
                mock(com.transportlogistics.app.fleet.application.ports.in.DriverAvailabilityUseCase.class),
                mock(com.transportlogistics.app.fleet.application.ports.in.DriverLicenseUseCase.class),
                driverExceptions,
                mock(com.transportlogistics.app.fleet.application.ports.in.VehicleUseCase.class),
                mock(com.transportlogistics.app.fleet.application.ports.in.VehicleAvailabilityUseCase.class),
                mock(com.transportlogistics.app.fleet.application.ports.in.VehicleCategoryUseCase.class),
                mock(com.transportlogistics.app.fleet.application.ports.in.VehicleTypeUseCase.class),
                mock(com.transportlogistics.app.fleet.application.ports.in.VehicleDocumentUseCase.class),
                mock(com.transportlogistics.app.fleet.application.ports.in.MaintenanceScheduleUseCase.class),
                mapper
        );

        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void createsDriverExceptionSuccessfully() throws Exception {
        var start = OffsetDateTime.of(2026, 9, 1, 8, 0, 0, 0, ZoneOffset.UTC);
        var end = OffsetDateTime.of(2026, 9, 1, 16, 0, 0, 0, ZoneOffset.UTC);
        var request = new DriverExceptionRequest(
                "LEAVE", start, end, "Personal leave", "Handover to Dave"
        );

        var exception = new DriverException(
                exceptionId, driverId, DriverExceptionType.LEAVE, start, end, DriverExceptionStatus.SCHEDULED,
                "Personal leave", "Handover to Dave", OffsetDateTime.now(ZoneOffset.UTC),
                OffsetDateTime.now(ZoneOffset.UTC), "admin", "admin"
        );

        when(driverExceptions.create(eq(driverId), any(), any())).thenReturn(exception);

        mockMvc.perform(post("/drivers/" + driverId + "/exceptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(exceptionId.toString()))
                .andExpect(jsonPath("$.exceptionType").value("LEAVE"))
                .andExpect(jsonPath("$.status").value("SCHEDULED"));
    }

    @Test
    void listsDriverExceptions() throws Exception {
        var start = OffsetDateTime.of(2026, 9, 1, 8, 0, 0, 0, ZoneOffset.UTC);
        var end = OffsetDateTime.of(2026, 9, 1, 16, 0, 0, 0, ZoneOffset.UTC);
        var exception = new DriverException(
                exceptionId, driverId, DriverExceptionType.DISCIPLINARY_SUSPENSION, start, end, DriverExceptionStatus.ACTIVE,
                "Speeding violation", null, OffsetDateTime.now(ZoneOffset.UTC),
                OffsetDateTime.now(ZoneOffset.UTC), "admin", "admin"
        );

        when(driverExceptions.list(driverId)).thenReturn(List.of(exception));

        mockMvc.perform(get("/drivers/" + driverId + "/exceptions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(exceptionId.toString()))
                .andExpect(jsonPath("$[0].driverId").value(driverId.toString()))
                .andExpect(jsonPath("$[0].exceptionType").value("DISCIPLINARY_SUSPENSION"));
    }

    @Test
    void getsDriverExceptionById() throws Exception {
        var start = OffsetDateTime.of(2026, 9, 1, 8, 0, 0, 0, ZoneOffset.UTC);
        var end = OffsetDateTime.of(2026, 9, 1, 16, 0, 0, 0, ZoneOffset.UTC);
        var exception = new DriverException(
                exceptionId, driverId, DriverExceptionType.MEDICAL_EMERGENCY, start, end, DriverExceptionStatus.SCHEDULED,
                "Hospital admission", null, OffsetDateTime.now(ZoneOffset.UTC),
                OffsetDateTime.now(ZoneOffset.UTC), "admin", "admin"
        );

        when(driverExceptions.get(driverId, exceptionId)).thenReturn(exception);

        mockMvc.perform(get("/drivers/" + driverId + "/exceptions/" + exceptionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(exceptionId.toString()))
                .andExpect(jsonPath("$.exceptionType").value("MEDICAL_EMERGENCY"));
    }

    @Test
    void cancelsDriverException() throws Exception {
        var start = OffsetDateTime.of(2026, 9, 1, 8, 0, 0, 0, ZoneOffset.UTC);
        var end = OffsetDateTime.of(2026, 9, 1, 16, 0, 0, 0, ZoneOffset.UTC);
        var actionRequest = new DriverExceptionActionRequest("Driver recovered");
        var exception = new DriverException(
                exceptionId, driverId, DriverExceptionType.LEAVE, start, end, DriverExceptionStatus.CANCELLED,
                "Leave cancelled", "Driver recovered", OffsetDateTime.now(ZoneOffset.UTC),
                OffsetDateTime.now(ZoneOffset.UTC), "admin", "admin"
        );

        when(driverExceptions.cancel(eq(driverId), eq(exceptionId), eq("Driver recovered"), any()))
                .thenReturn(exception);

        mockMvc.perform(post("/drivers/" + driverId + "/exceptions/" + exceptionId + "/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(actionRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void completesDriverException() throws Exception {
        var start = OffsetDateTime.of(2026, 9, 1, 8, 0, 0, 0, ZoneOffset.UTC);
        var end = OffsetDateTime.of(2026, 9, 1, 16, 0, 0, 0, ZoneOffset.UTC);
        var actionRequest = new DriverExceptionActionRequest("Leave finished");
        var exception = new DriverException(
                exceptionId, driverId, DriverExceptionType.LEAVE, start, end, DriverExceptionStatus.COMPLETED,
                "Annual leave", "Leave finished", OffsetDateTime.now(ZoneOffset.UTC),
                OffsetDateTime.now(ZoneOffset.UTC), "admin", "admin"
        );

        when(driverExceptions.complete(eq(driverId), eq(exceptionId), eq("Leave finished"), any()))
                .thenReturn(exception);

        mockMvc.perform(post("/drivers/" + driverId + "/exceptions/" + exceptionId + "/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(actionRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }
}
