package com.transportlogistics.app.fuel.infrastructure.adapters.in.web;

import com.transportlogistics.app.fuel.application.ports.in.FuelIssueUseCase;
import com.transportlogistics.app.fuel.application.ports.in.FuelStationUseCase;
import com.transportlogistics.app.fuel.domain.model.FuelIssue;
import com.transportlogistics.app.fuel.domain.model.FuelIssueStatus;
import com.transportlogistics.app.fuel.domain.model.FuelStation;
import com.transportlogistics.app.fuel.domain.model.FuelStationType;
import com.transportlogistics.app.fuel.infrastructure.adapters.in.web.controllers.FuelController;
import com.transportlogistics.app.fuel.infrastructure.adapters.in.web.mappers.FuelWebMapper;
import com.transportlogistics.app.shared.web.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class FuelControllerTest {
    private FuelIssueUseCase issues;
    private FuelStationUseCase stations;
    private MockMvc mvc;
    private UUID vehicleId;
    private UUID stationId;

    @BeforeEach
    void setUp() {
        issues = mock(FuelIssueUseCase.class);
        stations = mock(FuelStationUseCase.class);
        vehicleId = UUID.randomUUID();
        stationId = UUID.randomUUID();
        when(stations.get(stationId)).thenReturn(new FuelStation(stationId, "MAIN", "Main Depot",
                FuelStationType.INTERNAL, true, null, null));
        var mapper = Mappers.getMapper(FuelWebMapper.class);
        mvc = MockMvcBuilders.standaloneSetup(new FuelController(issues, stations, mapper))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void createsFuelIssueWithoutEditableStatus() throws Exception {
        when(issues.create(any(), eq("operator"))).thenReturn(issue());
        mvc.perform(post("/fuel-issues").principal(() -> "operator").contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody("25.500")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.voucherNumber").value("FUEL-2026-000001"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.vehicle.id").value(vehicleId.toString()))
                .andExpect(jsonPath("$.station.name").value("Main Depot"));
    }

    @Test
    void rejectsZeroQuantityBeforeUseCaseMutation() throws Exception {
        mvc.perform(post("/fuel-issues").principal(() -> "operator").contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody("0.000")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        verify(issues, never()).create(any(), any());
    }

    @Test
    void authorizationRequiresValidActorAndCommentBoundary() throws Exception {
        when(issues.authorize(any(), any(), eq("supervisor"))).thenReturn(issue());
        mvc.perform(post("/fuel-issues/{id}/authorize", UUID.randomUUID()).principal(() -> "supervisor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"ok to dispatch\"}"))
                .andExpect(status().isOk());
    }

    private FuelIssue issue() {
        return new FuelIssue(UUID.randomUUID(), "FUEL-2026-000001", vehicleId, null, null, "DIESEL",
                new BigDecimal("25.500"), new BigDecimal("350.00"), new BigDecimal("8925.00"), stationId,
                new BigDecimal("12000.00"), new BigDecimal("150.00"), OffsetDateTime.parse("2026-02-01T10:00:00Z"),
                FuelIssueStatus.DRAFT, UUID.randomUUID(), null, null, "notes", OffsetDateTime.now(), OffsetDateTime.now());
    }

    private String requestBody(String quantity) {
        return """
                {
                  "vehicleId": "%s",
                  "fuelType": "DIESEL",
                  "quantity": %s,
                  "unitPrice": 350.00,
                  "stationId": "%s",
                  "odometer": 12000.00,
                  "engineHours": 150.00,
                  "issueDateTime": "2026-02-01T10:00:00Z",
                  "notes": "standard issue"
                }
                """.formatted(vehicleId, quantity, stationId);
    }
}
