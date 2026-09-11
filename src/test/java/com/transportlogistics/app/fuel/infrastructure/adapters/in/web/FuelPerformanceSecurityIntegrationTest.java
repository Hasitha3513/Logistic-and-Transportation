package com.transportlogistics.app.fuel.infrastructure.adapters.in.web;

import com.transportlogistics.app.fuel.FuelPerformanceQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpMethod;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.operations.sla.enabled=false")
@AutoConfigureMockMvc
@Import(FuelPerformanceSecurityIntegrationTest.TestBeans.class)
class FuelPerformanceSecurityIntegrationTest {
    private static final UUID ID = UUID.fromString("37000000-0000-0000-0000-000000000037");
    @Autowired MockMvc mvc;
    @Autowired FuelPerformanceQuery query;

    @TestConfiguration(proxyBeanMethods = false)
    static class TestBeans {
        @Bean @Primary FuelPerformanceQuery fuelPerformanceSecurityQuery() {
            return org.mockito.Mockito.mock(FuelPerformanceQuery.class);
        }
    }

    @BeforeEach
    void setup() {
        when(query.vehicles(any(), anyInt(), anyInt(), anyString(), anyString()))
                .thenReturn(new FuelPerformanceQuery.Page<>(List.of(), 0, 20, 0, 0));
        when(query.drivers(any(), anyInt(), anyInt(), anyString(), anyString()))
                .thenReturn(new FuelPerformanceQuery.Page<>(List.of(), 0, 20, 0, 0));
        when(query.trends(any())).thenReturn(List.of());
    }

    @Test
    @WithMockUser(authorities = {"FUEL_ISSUE_VIEW", "FUEL_COST_VIEW", "REPORT_VIEW"})
    void rawFuelAndReportPermissionsCannotAccessAnyEffectiveOrLiteralRoute() throws Exception {
        for (boolean literal : List.of(false, true)) {
            for (String path : paths()) expect(HttpMethod.GET, path, literal, 403);
        }
    }

    @Test
    @WithMockUser(authorities = "FUEL_PERFORMANCE_VIEW")
    void dedicatedPermissionAllowsAllSixLiteralReadRoutesButNoWritesOrExport() throws Exception {
        for (String path : paths()) expect(HttpMethod.GET, path, true, 200);
        expect(HttpMethod.POST, "/v1/fuel/performance/summary", true, 403);
        expect(HttpMethod.GET, "/v1/fuel/performance/export", true, 404);
    }

    private static List<String> paths() {
        return List.of("/v1/fuel/performance/summary", "/v1/fuel/performance/vehicles",
                "/v1/fuel/performance/vehicles/" + ID, "/v1/fuel/performance/drivers",
                "/v1/fuel/performance/drivers/" + ID, "/v1/fuel/performance/trends");
    }

    private void expect(HttpMethod method, String path, boolean literal, int expected) throws Exception {
        String uri = literal ? "/api" + path : path;
        MockHttpServletRequestBuilder builder = request(method, uri);
        if (literal) builder.contextPath("/api");
        mvc.perform(builder).andExpect(status().is(expected));
    }
}
