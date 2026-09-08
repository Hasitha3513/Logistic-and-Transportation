package com.transportlogistics.app.tracking;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.transportlogistics.app.tenancy.CurrentTenant;
import com.transportlogistics.app.tenancy.TenantExecutionContext;
import com.transportlogistics.app.tracking.ports.inbound.TrackingUseCase;
import com.transportlogistics.app.tracking.ports.outbound.TrackingStore;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "app.tracking.provider-secrets.FIXTURE=test-secret")
@AutoConfigureMockMvc
@Import(TrackingSecurityIntegrationTest.Stubs.class)
class TrackingSecurityIntegrationTest {
    private static final UUID TENANT = UUID.fromString("48000000-0000-0000-0000-000000000001");
    private static final UUID VEHICLE = UUID.fromString("48000000-0000-0000-0000-000000000002");

    @Autowired MockMvc mvc;
    @Autowired CurrentTenant currentTenant;
    @Autowired TrackingUseCase useCase;

    @TestConfiguration(proxyBeanMethods = false)
    static class Stubs {
        @Bean @Primary CurrentTenant trackingTestTenant() { return org.mockito.Mockito.mock(CurrentTenant.class); }
        @Bean @Primary TrackingUseCase trackingTestUseCase() { return org.mockito.Mockito.mock(TrackingUseCase.class); }
        @Bean @Primary TrackingStore trackingTestStore() { return org.mockito.Mockito.mock(TrackingStore.class); }
    }

    @BeforeEach
    void setup() {
        var context = new TenantExecutionContext(TENANT, UUID.randomUUID(), "tracking.operator", "tracking-test");
        when(currentTenant.current()).thenReturn(Optional.of(context));
        when(currentTenant.required()).thenReturn(context);
        when(useCase.vehicles(any(), anyInt(), anyInt(), any())).thenReturn(List.of());
        when(useCase.devices(any(), anyInt(), anyInt(), anyBoolean())).thenReturn(List.of());
    }

    @Test @WithMockUser(authorities = "RANDOM_AUTHORITY")
    void literalApiRoutesDenyUnrelatedAuthority() throws Exception {
        mvc.perform(get("/api/v1/tracking/vehicles").contextPath("/api")).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/tracking/vehicles/{id}/positions", VEHICLE).contextPath("/api")
                .param("from", Instant.now().minusSeconds(60).toString()).param("to", Instant.now().toString()))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/tracking/devices").contextPath("/api")
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test @WithMockUser(authorities = "TRACKING_VIEW")
    void viewCannotReadHistoryOrManageDevices() throws Exception {
        mvc.perform(get("/api/v1/tracking/vehicles").contextPath("/api")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/tracking/vehicles/{id}/positions", VEHICLE).contextPath("/api")
                .param("from", Instant.now().minusSeconds(60).toString()).param("to", Instant.now().toString()))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/tracking/devices").contextPath("/api")
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test @WithMockUser(authorities = "TRACKING_HISTORY_VIEW")
    void historyPermissionAllowsLiteralHistoryRoute() throws Exception {
        when(useCase.positions(any(), any(), any(), any(), anyString(), anyInt()))
                .thenReturn(new TrackingUseCase.HistoryPage(List.of(), null));
        mvc.perform(get("/api/v1/tracking/vehicles/{id}/positions", VEHICLE).contextPath("/api")
                .param("from", Instant.now().minusSeconds(60).toString()).param("to", Instant.now().toString())
                .param("cursor", "cursor"))
                .andExpect(status().isOk());
    }
}
