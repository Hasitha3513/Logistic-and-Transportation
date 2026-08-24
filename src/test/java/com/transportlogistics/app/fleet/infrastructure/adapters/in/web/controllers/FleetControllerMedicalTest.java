package com.transportlogistics.app.fleet.infrastructure.adapters.in.web.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transportlogistics.app.fleet.application.ports.in.DriverMedicalRecordUseCase;
import com.transportlogistics.app.fleet.domain.model.DriverMedicalRecord;
import com.transportlogistics.app.fleet.domain.model.DriverMedicalStatus;
import com.transportlogistics.app.fleet.domain.model.VisionTestStatus;
import com.transportlogistics.app.fleet.infrastructure.adapters.in.web.dto.request.DriverMedicalRecordRequest;
import com.transportlogistics.app.fleet.infrastructure.adapters.in.web.mappers.FleetWebMapperImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class FleetControllerMedicalTest {

    private MockMvc mockMvc;
    private DriverMedicalRecordUseCase medicalRecords;
    private ObjectMapper objectMapper;
    private UUID driverId;

    @BeforeEach
    void setUp() {
        medicalRecords = mock(DriverMedicalRecordUseCase.class);
        var controller = new FleetController(
                null, null, null, null, null, null,
                medicalRecords, null,
                null, null, null, null, null, null,
                new FleetWebMapperImpl()
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper().findAndRegisterModules();
        driverId = UUID.randomUUID();
    }

    @Test
    @DisplayName("GET /drivers/{driverId}/medical-records returns list of medical records")
    void listMedicalRecords() throws Exception {
        var record = new DriverMedicalRecord(
                UUID.randomUUID(), driverId, LocalDate.of(2026, 1, 15), LocalDate.of(2026, 1, 15), LocalDate.of(2027, 1, 15),
                DriverMedicalStatus.FIT, VisionTestStatus.PASSED, null, "Dr. A", "MED-01", "Fit",
                true, OffsetDateTime.now(), OffsetDateTime.now(), "admin", "admin"
        );
        when(medicalRecords.list(driverId)).thenReturn(List.of(record));

        mockMvc.perform(get("/drivers/{driverId}/medical-records", driverId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fitnessStatus").value("FIT"))
                .andExpect(jsonPath("$[0].certificateReference").value("MED-01"));
    }

    @Test
    @DisplayName("POST /drivers/{driverId}/medical-records creates new medical record")
    void createMedicalRecord() throws Exception {
        var record = new DriverMedicalRecord(
                UUID.randomUUID(), driverId, LocalDate.of(2026, 1, 15), LocalDate.of(2026, 1, 15), LocalDate.of(2027, 1, 15),
                DriverMedicalStatus.FIT, VisionTestStatus.PASSED, null, "Dr. A", "MED-01", "Fit",
                true, OffsetDateTime.now(), OffsetDateTime.now(), "system", "system"
        );
        when(medicalRecords.create(eq(driverId), any(), any())).thenReturn(record);

        var request = new DriverMedicalRecordRequest(
                LocalDate.of(2026, 1, 15),
                LocalDate.of(2026, 1, 15),
                LocalDate.of(2027, 1, 15),
                "FIT",
                "PASSED",
                null,
                "Dr. A",
                "MED-01",
                "Fit"
        );

        mockMvc.perform(post("/drivers/{driverId}/medical-records", driverId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fitnessStatus").value("FIT"));
    }
}
