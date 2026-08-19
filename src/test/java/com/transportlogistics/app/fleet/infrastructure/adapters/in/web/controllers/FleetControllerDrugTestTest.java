package com.transportlogistics.app.fleet.infrastructure.adapters.in.web.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transportlogistics.app.fleet.application.ports.in.DriverDrugTestUseCase;
import com.transportlogistics.app.fleet.domain.model.DriverDrugTest;
import com.transportlogistics.app.fleet.domain.model.DrugTestResult;
import com.transportlogistics.app.fleet.domain.model.DrugTestStatus;
import com.transportlogistics.app.fleet.domain.model.DrugTestType;
import com.transportlogistics.app.fleet.infrastructure.adapters.in.web.dto.request.DriverDrugTestRequest;
import com.transportlogistics.app.fleet.infrastructure.adapters.in.web.dto.request.DriverDrugTestResultRequest;
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

class FleetControllerDrugTestTest {

    private MockMvc mockMvc;
    private DriverDrugTestUseCase drugTests;
    private ObjectMapper objectMapper;
    private UUID driverId;

    @BeforeEach
    void setUp() {
        drugTests = mock(DriverDrugTestUseCase.class);
        var controller = new FleetController(
                null, null, null, null, null, null,
                null, drugTests,
                null, null, null, null, null, null,
                new FleetWebMapperImpl()
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper().findAndRegisterModules();
        driverId = UUID.randomUUID();
    }

    @Test
    @DisplayName("GET /drivers/{driverId}/drug-tests returns list of drug tests")
    void listDrugTests() throws Exception {
        var test = new DriverDrugTest(
                UUID.randomUUID(), driverId, DrugTestType.RANDOM, LocalDate.of(2026, 6, 1), null, null,
                DrugTestResult.PENDING, DrugTestStatus.SCHEDULED, "LabCorp", "REF-01", null,
                false, null, true, OffsetDateTime.now(), OffsetDateTime.now(), "admin", "admin"
        );
        when(drugTests.list(driverId)).thenReturn(List.of(test));

        mockMvc.perform(get("/drivers/{driverId}/drug-tests", driverId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].testType").value("RANDOM"))
                .andExpect(jsonPath("$[0].result").value("PENDING"));
    }

    @Test
    @DisplayName("POST /drivers/{driverId}/drug-tests schedules new test")
    void scheduleDrugTest() throws Exception {
        var test = new DriverDrugTest(
                UUID.randomUUID(), driverId, DrugTestType.SCHEDULED, LocalDate.of(2026, 6, 1), null, null,
                DrugTestResult.PENDING, DrugTestStatus.SCHEDULED, "LabCorp", "REF-01", "Pre-employment",
                false, null, true, OffsetDateTime.now(), OffsetDateTime.now(), "system", "system"
        );
        when(drugTests.schedule(eq(driverId), any(), any())).thenReturn(test);

        var request = new DriverDrugTestRequest(
                "SCHEDULED",
                LocalDate.of(2026, 6, 1),
                "LabCorp",
                "REF-01",
                "Pre-employment"
        );

        mockMvc.perform(post("/drivers/{driverId}/drug-tests", driverId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SCHEDULED"));
    }
}
