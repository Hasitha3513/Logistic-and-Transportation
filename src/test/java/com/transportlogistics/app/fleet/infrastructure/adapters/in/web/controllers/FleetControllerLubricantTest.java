package com.transportlogistics.app.fleet.infrastructure.adapters.in.web.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transportlogistics.app.fleet.application.ports.in.LubricantLogUseCase;
import com.transportlogistics.app.fleet.domain.model.FluidType;
import com.transportlogistics.app.fleet.domain.model.LubricantLog;
import com.transportlogistics.app.fleet.domain.model.MeasurementUnit;
import com.transportlogistics.app.fleet.infrastructure.adapters.in.web.dto.request.LubricantLogRequest;
import com.transportlogistics.app.fleet.infrastructure.adapters.in.web.mappers.FleetWebMapperImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class FleetControllerLubricantTest {

    private MockMvc mockMvc;
    private LubricantLogUseCase lubricantLogs;
    private ObjectMapper objectMapper;
    private UUID vehicleId;

    @BeforeEach
    void setUp() {
        lubricantLogs = mock(LubricantLogUseCase.class);
        var controller = new FleetController(
                null, null, null, null, null, null,
                null, null, null, null, null, null,
                null, null,
                lubricantLogs,
                new FleetWebMapperImpl()
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper().findAndRegisterModules();
        vehicleId = UUID.randomUUID();
    }

    @Test
    @DisplayName("GET /vehicles/{vehicleId}/lubricant-logs returns list of logs")
    void listLubricantLogs() throws Exception {
        var log = new LubricantLog(
                UUID.randomUUID(), vehicleId, FluidType.ENGINE_OIL, new BigDecimal("10.00"), MeasurementUnit.LITRE,
                OffsetDateTime.now(), 50000.0, 1000.0, null, "Shell", "LUB-01", "Oil change",
                true, OffsetDateTime.now(), OffsetDateTime.now(), "admin", "admin"
        );
        when(lubricantLogs.list(eq(vehicleId), any(), any(), any())).thenReturn(List.of(log));

        mockMvc.perform(get("/vehicles/{vehicleId}/lubricant-logs", vehicleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fluidType").value("ENGINE_OIL"))
                .andExpect(jsonPath("$[0].quantity").value(10.00))
                .andExpect(jsonPath("$[0].unit").value("LITRE"));
    }

    @Test
    @DisplayName("POST /vehicles/{vehicleId}/lubricant-logs creates new lubricant log")
    void createLubricantLog() throws Exception {
        var log = new LubricantLog(
                UUID.randomUUID(), vehicleId, FluidType.ENGINE_OIL, new BigDecimal("10.00"), MeasurementUnit.LITRE,
                OffsetDateTime.now(), 50000.0, 1000.0, null, "Shell", "LUB-01", "Oil change",
                true, OffsetDateTime.now(), OffsetDateTime.now(), "system", "system"
        );
        when(lubricantLogs.create(eq(vehicleId), any(), any())).thenReturn(log);

        var request = new LubricantLogRequest(
                "ENGINE_OIL",
                new BigDecimal("10.00"),
                "LITRE",
                OffsetDateTime.now(),
                50000.0,
                1000.0,
                null,
                "Shell",
                "LUB-01",
                "Oil change"
        );

        mockMvc.perform(post("/vehicles/{vehicleId}/lubricant-logs", vehicleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fluidType").value("ENGINE_OIL"))
                .andExpect(jsonPath("$.quantity").value(10.00));
    }
}
