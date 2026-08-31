package com.transportlogistics.app.delivery.adapters.inbound.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transportlogistics.app.delivery.domain.model.*;
import com.transportlogistics.app.delivery.ports.inbound.FailedDeliveryUseCase;
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
class FailedDeliveryControllerSecurityTest {

    private static final UUID DELIVERY_ID = UUID.fromString("03cd51bf-7ae3-44bd-8202-817fef87341d");
    private static final UUID ATTEMPT_ID = UUID.fromString("11cd51bf-7ae3-44bd-8202-817fef87341d");
    private static final UUID ESCALATION_ID = UUID.fromString("22cd51bf-7ae3-44bd-8202-817fef87341d");

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @MockBean private FailedDeliveryUseCase useCase;

    @Test
    @DisplayName("Unauthenticated requests to failed delivery endpoints return 401 Unauthorized")
    void unauthenticatedReturns401() throws Exception {
        mvc.perform(post("/v1/deliveries/{id}/failed-attempt", DELIVERY_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"expectedVersion\":1,\"failureReason\":\"CUSTOMER_UNAVAILABLE\"}"))
                .andExpect(status().isUnauthorized());

        mvc.perform(get("/v1/deliveries/{id}/attempts", DELIVERY_ID))
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/v1/deliveries/{id}/escalate", DELIVERY_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"expectedVersion\":1,\"reason\":\"Damaged\"}"))
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/v1/deliveries/{id}/return-to-base", DELIVERY_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"expectedVersion\":1,\"reason\":\"Refused\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    @DisplayName("Users missing required permissions return 403 Forbidden")
    void missingPermissionsReturns403() throws Exception {
        mvc.perform(post("/v1/deliveries/{id}/failed-attempt", DELIVERY_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"expectedVersion\":1,\"failureReason\":\"CUSTOMER_UNAVAILABLE\"}"))
                .andExpect(status().isForbidden());

        mvc.perform(get("/v1/deliveries/{id}/attempts", DELIVERY_ID))
                .andExpect(status().isForbidden());

        mvc.perform(post("/v1/deliveries/{id}/escalate", DELIVERY_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"expectedVersion\":1,\"reason\":\"Damaged\"}"))
                .andExpect(status().isForbidden());

        mvc.perform(post("/v1/deliveries/{id}/return-to-base", DELIVERY_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"expectedVersion\":1,\"reason\":\"Refused\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "DELIVERY_FAIL_RECORD")
    @DisplayName("User with DELIVERY_FAIL_RECORD can record failed attempt")
    void authorizedRecordFailedAttempt() throws Exception {
        OffsetDateTime now = OffsetDateTime.now();
        DeliveryAttempt attempt = new DeliveryAttempt(
                ATTEMPT_ID, new DeliveryId(DELIVERY_ID), 1, now,
                DeliveryFailureReason.CUSTOMER_UNAVAILABLE, "Notes",
                DeliveryFailureDisposition.REDELIVERY_ELIGIBLE, List.of(), "user", now);

        when(useCase.recordFailedAttempt(eq(DELIVERY_ID), any(), any())).thenReturn(attempt);

        mvc.perform(post("/v1/deliveries/{id}/failed-attempt", DELIVERY_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"expectedVersion\":1,\"failureReason\":\"CUSTOMER_UNAVAILABLE\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "DELIVERY_FAIL_VIEW")
    @DisplayName("User with DELIVERY_FAIL_VIEW can get attempt history")
    void authorizedGetAttempts() throws Exception {
        when(useCase.getAttemptHistory(DELIVERY_ID)).thenReturn(List.of());
        when(useCase.getEscalations(DELIVERY_ID)).thenReturn(List.of());

        mvc.perform(get("/v1/deliveries/{id}/attempts", DELIVERY_ID))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "DELIVERY_RETURN_INITIATE")
    @DisplayName("User with DELIVERY_RETURN_INITIATE can return to base")
    void authorizedReturnToBase() throws Exception {
        OffsetDateTime now = OffsetDateTime.now();
        DeliveryOrder order = new DeliveryOrder(
                new DeliveryId(DELIVERY_ID), new DeliveryNumber("DEL-2026-000001"),
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                DeliveryPriority.NORMAL, DeliveryServiceType.STANDARD,
                new DeliveryWindow(now, now.plusHours(4)), null,
                DeliveryStatus.RETURN_TO_BASE, 2L, now, now, "user", "user");

        when(useCase.initiateReturnToBase(eq(DELIVERY_ID), any(), any())).thenReturn(order);

        mvc.perform(post("/v1/deliveries/{id}/return-to-base", DELIVERY_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"expectedVersion\":1,\"reason\":\"Refused\"}"))
                .andExpect(status().isOk());
    }
}
