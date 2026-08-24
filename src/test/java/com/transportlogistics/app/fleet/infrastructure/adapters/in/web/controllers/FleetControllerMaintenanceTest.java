package com.transportlogistics.app.fleet.infrastructure.adapters.in.web.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transportlogistics.app.fleet.application.ports.in.MaintenanceScheduleUseCase;
import com.transportlogistics.app.fleet.domain.model.MaintenanceSchedule;
import com.transportlogistics.app.fleet.domain.model.MaintenanceStatus;
import com.transportlogistics.app.fleet.infrastructure.adapters.in.web.dto.request.MaintenanceActionRequest;
import com.transportlogistics.app.fleet.infrastructure.adapters.in.web.dto.request.MaintenanceScheduleRequest;
import com.transportlogistics.app.fleet.infrastructure.adapters.in.web.mappers.FleetWebMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
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

class FleetControllerMaintenanceTest {

    private MockMvc mockMvc;
    private MaintenanceScheduleUseCase maintenanceSchedules;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final UUID vehicleId = UUID.randomUUID();
    private final UUID scheduleId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        maintenanceSchedules = mock(MaintenanceScheduleUseCase.class);
        var mapper = Mappers.getMapper(FleetWebMapper.class);

        var controller = new FleetController(
                mock(com.transportlogistics.app.fleet.application.ports.in.DriverUseCase.class),
                mock(com.transportlogistics.app.fleet.application.ports.in.DriverAvailabilityUseCase.class),
                mock(com.transportlogistics.app.fleet.application.ports.in.DriverLicenseUseCase.class),
                mock(com.transportlogistics.app.fleet.vehiclemaster.ports.inbound.VehicleUseCase.class),
                mock(com.transportlogistics.app.fleet.application.ports.in.VehicleAvailabilityUseCase.class),
                mock(com.transportlogistics.app.fleet.application.ports.in.VehicleCategoryUseCase.class),
                mock(com.transportlogistics.app.fleet.application.ports.in.VehicleTypeUseCase.class),
                mock(com.transportlogistics.app.fleet.application.ports.in.VehicleDocumentUseCase.class),
                maintenanceSchedules,
                mapper
        );

        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void createsMaintenanceScheduleSuccessfully() throws Exception {
        var start = OffsetDateTime.of(2026, 9, 1, 8, 0, 0, 0, ZoneOffset.UTC);
        var end = OffsetDateTime.of(2026, 9, 1, 16, 0, 0, 0, ZoneOffset.UTC);
        var request = new MaintenanceScheduleRequest(
                "Preventive Service", start, end, "Brake service", "Garage A", new BigDecimal("250.00")
        );

        var schedule = new MaintenanceSchedule(
                scheduleId, vehicleId, "Preventive Service", start, end, MaintenanceStatus.SCHEDULED,
                "Brake service", "Garage A", new BigDecimal("250.00"), OffsetDateTime.now(ZoneOffset.UTC),
                OffsetDateTime.now(ZoneOffset.UTC), "admin", "admin"
        );

        when(maintenanceSchedules.create(eq(vehicleId), any(), any())).thenReturn(schedule);

        mockMvc.perform(post("/vehicles/" + vehicleId + "/maintenance-schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(scheduleId.toString()))
                .andExpect(jsonPath("$.maintenanceType").value("Preventive Service"))
                .andExpect(jsonPath("$.status").value("SCHEDULED"));
    }

    @Test
    void listsMaintenanceSchedulesForVehicle() throws Exception {
        var start = OffsetDateTime.of(2026, 9, 1, 8, 0, 0, 0, ZoneOffset.UTC);
        var end = OffsetDateTime.of(2026, 9, 1, 16, 0, 0, 0, ZoneOffset.UTC);
        var schedule = new MaintenanceSchedule(
                scheduleId, vehicleId, "Preventive Service", start, end, MaintenanceStatus.SCHEDULED,
                "Notes", "Garage A", new BigDecimal("200.00"), OffsetDateTime.now(ZoneOffset.UTC),
                OffsetDateTime.now(ZoneOffset.UTC), "admin", "admin"
        );

        when(maintenanceSchedules.list(vehicleId)).thenReturn(List.of(schedule));

        mockMvc.perform(get("/vehicles/" + vehicleId + "/maintenance-schedules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(scheduleId.toString()))
                .andExpect(jsonPath("$[0].vehicleId").value(vehicleId.toString()));
    }

    @Test
    void getsMaintenanceScheduleById() throws Exception {
        var start = OffsetDateTime.of(2026, 9, 1, 8, 0, 0, 0, ZoneOffset.UTC);
        var end = OffsetDateTime.of(2026, 9, 1, 16, 0, 0, 0, ZoneOffset.UTC);
        var schedule = new MaintenanceSchedule(
                scheduleId, vehicleId, "Preventive Service", start, end, MaintenanceStatus.SCHEDULED,
                "Notes", "Garage A", new BigDecimal("200.00"), OffsetDateTime.now(ZoneOffset.UTC),
                OffsetDateTime.now(ZoneOffset.UTC), "admin", "admin"
        );

        when(maintenanceSchedules.get(vehicleId, scheduleId)).thenReturn(schedule);

        mockMvc.perform(get("/vehicles/" + vehicleId + "/maintenance-schedules/" + scheduleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(scheduleId.toString()));
    }

    @Test
    void cancelsMaintenanceSchedule() throws Exception {
        var schedule = new MaintenanceSchedule(
                scheduleId, vehicleId, "Preventive Service",
                OffsetDateTime.now(ZoneOffset.UTC), OffsetDateTime.now(ZoneOffset.UTC).plusHours(2),
                MaintenanceStatus.CANCELLED, "Cancelled: bad weather", null, null,
                OffsetDateTime.now(ZoneOffset.UTC), OffsetDateTime.now(ZoneOffset.UTC), "admin", "admin"
        );

        when(maintenanceSchedules.cancel(eq(vehicleId), eq(scheduleId), any(), any())).thenReturn(schedule);

        var actionRequest = new MaintenanceActionRequest("bad weather");

        mockMvc.perform(post("/vehicles/" + vehicleId + "/maintenance-schedules/" + scheduleId + "/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(actionRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void completesMaintenanceSchedule() throws Exception {
        var schedule = new MaintenanceSchedule(
                scheduleId, vehicleId, "Preventive Service",
                OffsetDateTime.now(ZoneOffset.UTC), OffsetDateTime.now(ZoneOffset.UTC).plusHours(2),
                MaintenanceStatus.COMPLETED, "Completed: All passed", null, null,
                OffsetDateTime.now(ZoneOffset.UTC), OffsetDateTime.now(ZoneOffset.UTC), "admin", "admin"
        );

        when(maintenanceSchedules.complete(eq(vehicleId), eq(scheduleId), any(), any())).thenReturn(schedule);

        var actionRequest = new MaintenanceActionRequest("All passed");

        mockMvc.perform(post("/vehicles/" + vehicleId + "/maintenance-schedules/" + scheduleId + "/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(actionRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }
}
