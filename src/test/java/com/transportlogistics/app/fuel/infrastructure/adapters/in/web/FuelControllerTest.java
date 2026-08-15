package com.transportlogistics.app.fuel.infrastructure.adapters.in.web;

import com.transportlogistics.app.fuel.application.ports.in.FuelIssueUseCase;
import com.transportlogistics.app.fuel.application.ports.in.FuelStationUseCase;
import com.transportlogistics.app.fuel.domain.model.FuelIssue;
import com.transportlogistics.app.fuel.domain.model.FuelIssueStatus;
import com.transportlogistics.app.fuel.domain.model.FuelStation;
import com.transportlogistics.app.fuel.domain.model.FuelStationType;
import com.transportlogistics.app.shared.web.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
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
        mvc = MockMvcBuilders.standaloneSetup(new FuelController(issues, stations))
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
                        .content(requestBody("0")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        verifyNoInteractions(issues);
    }

    @Test
    void exposesServerPaginationAndFilters() throws Exception {
        when(issues.search(any())).thenReturn(new FuelIssueUseCase.PageResult<>(List.of(issue()), 1, 10, 12, 2));
        mvc.perform(get("/fuel-issues").param("page", "1").param("limit", "10")
                        .param("status", "DRAFT").param("voucherNumber", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].voucherNumber").value("FUEL-2026-000001"))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.totalElements").value(12));
        verify(issues).search(argThat(query -> query.page() == 1 && query.limit() == 10
                && query.status() == FuelIssueStatus.DRAFT && query.voucherNumber().equals("2026")));
    }

    @Test
    void delegatesExplicitAuthorizeAction() throws Exception {
        when(issues.authorize(any(), eq("Approved"), eq("manager"))).thenReturn(issue());
        mvc.perform(post("/fuel-issues/{id}/authorize", UUID.randomUUID()).principal(() -> "manager")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"comment\":\"Approved\"}"))
                .andExpect(status().isOk());
        verify(issues).authorize(any(), eq("Approved"), eq("manager"));
    }

    private String requestBody(String quantity) {
        return """
                {"vehicleId":"%s","fuelType":"DIESEL","quantity":%s,
                 "stationId":"%s","odometer":1000,"issueDateTime":"2026-08-15T08:00:00Z"}
                """.formatted(vehicleId, quantity, stationId);
    }

    private FuelIssue issue() {
        var now = OffsetDateTime.parse("2026-08-15T08:00:00Z");
        return new FuelIssue(UUID.randomUUID(), "FUEL-2026-000001", vehicleId, null, null, "DIESEL",
                new BigDecimal("25.500"), null, null, stationId, new BigDecimal("1000"), null, now,
                FuelIssueStatus.DRAFT, UUID.randomUUID(), null, null, null, now, now);
    }
}
