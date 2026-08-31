package com.transportlogistics.app.delivery.adapters.inbound.web.controllers;

import com.transportlogistics.app.delivery.DeliveryReportingQuery.*;
import com.transportlogistics.app.delivery.ports.inbound.DeliveryAnalyticsUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DeliveryAnalyticsControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DeliveryAnalyticsUseCase analyticsUseCase;

    @Test
    @DisplayName("GET /v1/deliveries/analytics/summary without authentication returns 401 Unauthorized")
    void summaryUnauthenticated() throws Exception {
        mockMvc.perform(get("/v1/deliveries/analytics/summary"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /v1/deliveries/analytics/summary with DELIVERY_VIEW only returns 403 Forbidden")
    @WithMockUser(authorities = {"DELIVERY_VIEW"})
    void summaryWithoutAnalyticsPermission() throws Exception {
        mockMvc.perform(get("/v1/deliveries/analytics/summary"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /v1/deliveries/analytics/summary with DELIVERY_ANALYTICS_VIEW returns 200 OK")
    @WithMockUser(authorities = {"DELIVERY_ANALYTICS_VIEW"})
    void summaryAuthorized() throws Exception {
        var stub = new DeliveryAnalyticsSummary(
                new Period(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31)),
                100, 10, 90, 80, 10,
                BigDecimal.valueOf(88.89), BigDecimal.valueOf(75.0),
                70, 10, BigDecimal.valueOf(87.5), BigDecimal.valueOf(12.5),
                BigDecimal.valueOf(35.5), 25, BigDecimal.valueOf(0.25),
                15, BigDecimal.valueOf(15.0), BigDecimal.valueOf(80.0),
                BigDecimal.valueOf(11.11)
        );

        when(analyticsUseCase.getSummary(any())).thenReturn(stub);

        mockMvc.perform(get("/v1/deliveries/analytics/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalOrders").value(100))
                .andExpect(jsonPath("$.orderSuccessRate").value(88.89))
                .andExpect(jsonPath("$.onTimeDeliveryRate").value(87.5));
    }

    @Test
    @DisplayName("GET /v1/deliveries/analytics/failures with DELIVERY_ANALYTICS_VIEW returns 200 OK")
    @WithMockUser(authorities = {"DELIVERY_ANALYTICS_VIEW"})
    void failuresAuthorized() throws Exception {
        when(analyticsUseCase.getFailureBreakdown(any())).thenReturn(List.of(
                new FailureReasonBreakdownItem("CUSTOMER_UNAVAILABLE", 5, BigDecimal.valueOf(50.0), 5, 0, 0)
        ));

        mockMvc.perform(get("/v1/deliveries/analytics/failures"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].failureReason").value("CUSTOMER_UNAVAILABLE"))
                .andExpect(jsonPath("$[0].count").value(5));
    }
}
