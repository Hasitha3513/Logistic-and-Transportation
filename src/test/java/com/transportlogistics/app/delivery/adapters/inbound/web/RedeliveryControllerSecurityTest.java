package com.transportlogistics.app.delivery.adapters.inbound.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transportlogistics.app.delivery.domain.model.*;
import com.transportlogistics.app.delivery.ports.inbound.RedeliveryUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RedeliveryControllerSecurityTest {

    private static final UUID DELIVERY_ID = UUID.fromString("03cd51bf-7ae3-44bd-8202-817fef87341d");
    private static final UUID ATTEMPT_ID = UUID.fromString("11cd51bf-7ae3-44bd-8202-817fef87341d");

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @MockBean private RedeliveryUseCase useCase;

    @Test
    @DisplayName("Unauthenticated requests to redelivery endpoints return 401 Unauthorized")
    void unauthenticatedReturns401() throws Exception {
        mvc.perform(post("/v1/deliveries/{id}/redelivery/suggestions", DELIVERY_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/v1/deliveries/{id}/redelivery/schedule", DELIVERY_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"expectedVersion\":1,\"schedulingMethod\":\"AGENT_ASSISTED\",\"scheduledStartTime\":\"2026-09-01T10:00:00Z\",\"scheduledEndTime\":\"2026-09-01T14:00:00Z\"}"))
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/v1/deliveries/{id}/redelivery/reschedule", DELIVERY_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"expectedVersion\":2,\"scheduledStartTime\":\"2026-09-01T10:00:00Z\",\"scheduledEndTime\":\"2026-09-01T14:00:00Z\"}"))
                .andExpect(status().isUnauthorized());

        mvc.perform(get("/v1/deliveries/{id}/redelivery/history", DELIVERY_ID))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "UNAUTHORIZED_PERMISSION")
    @DisplayName("Authenticated user without redelivery permissions receives 403 Forbidden")
    void forbiddenWithoutPermission() throws Exception {
        mvc.perform(post("/v1/deliveries/{id}/redelivery/suggestions", DELIVERY_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isForbidden());

        mvc.perform(post("/v1/deliveries/{id}/redelivery/schedule", DELIVERY_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"expectedVersion\":1,\"schedulingMethod\":\"AGENT_ASSISTED\",\"scheduledStartTime\":\"2026-09-01T10:00:00Z\",\"scheduledEndTime\":\"2026-09-01T14:00:00Z\"}"))
                .andExpect(status().isForbidden());

        mvc.perform(post("/v1/deliveries/{id}/redelivery/reschedule", DELIVERY_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"expectedVersion\":2,\"scheduledStartTime\":\"2026-09-01T10:00:00Z\",\"scheduledEndTime\":\"2026-09-01T14:00:00Z\"}"))
                .andExpect(status().isForbidden());

        mvc.perform(get("/v1/deliveries/{id}/redelivery/history", DELIVERY_ID))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "DELIVERY_REDELIVERY_VIEW")
    @DisplayName("User with DELIVERY_REDELIVERY_VIEW can get suggestions and history but cannot schedule")
    void viewPermissionAllowsReadonly() throws Exception {
        when(useCase.getSuggestions(eq(DELIVERY_ID), any())).thenReturn(List.of());
        when(useCase.getHistory(DELIVERY_ID)).thenReturn(List.of());

        mvc.perform(post("/v1/deliveries/{id}/redelivery/suggestions", DELIVERY_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isOk());

        mvc.perform(get("/v1/deliveries/{id}/redelivery/history", DELIVERY_ID))
                .andExpect(status().isOk());

        mvc.perform(post("/v1/deliveries/{id}/redelivery/schedule", DELIVERY_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"expectedVersion\":1,\"schedulingMethod\":\"AGENT_ASSISTED\",\"scheduledStartTime\":\"2026-09-01T10:00:00Z\",\"scheduledEndTime\":\"2026-09-01T14:00:00Z\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "DELIVERY_REDELIVERY_SCHEDULE")
    @DisplayName("User with DELIVERY_REDELIVERY_SCHEDULE can schedule and reschedule redeliveries")
    void schedulePermissionAllowsMutations() throws Exception {
        OffsetDateTime now = OffsetDateTime.now();
        DeliveryRedeliverySchedule schedule = DeliveryRedeliverySchedule.createConfirmed(
                UUID.randomUUID(), UUID.randomUUID(), new DeliveryId(DELIVERY_ID), ATTEMPT_ID,
                RedeliverySchedulingMethod.AGENT_ASSISTED, null, null, null,
                now.plusHours(24), now.plusHours(28), "dispatcher", now
        );

        when(useCase.scheduleRedelivery(eq(DELIVERY_ID), any(), any())).thenReturn(schedule);
        when(useCase.reschedule(eq(DELIVERY_ID), any(), any())).thenReturn(schedule);

        mvc.perform(post("/v1/deliveries/{id}/redelivery/schedule", DELIVERY_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"expectedVersion\":1,\"schedulingMethod\":\"AGENT_ASSISTED\",\"scheduledStartTime\":\"2026-09-01T10:00:00Z\",\"scheduledEndTime\":\"2026-09-01T14:00:00Z\"}"))
                .andExpect(status().isCreated());

        mvc.perform(post("/v1/deliveries/{id}/redelivery/reschedule", DELIVERY_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"expectedVersion\":2,\"scheduledStartTime\":\"2026-09-01T10:00:00Z\",\"scheduledEndTime\":\"2026-09-01T14:00:00Z\"}"))
                .andExpect(status().isOk());
    }
}
