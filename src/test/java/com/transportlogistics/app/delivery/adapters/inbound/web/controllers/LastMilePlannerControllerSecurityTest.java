package com.transportlogistics.app.delivery.adapters.inbound.web.controllers;

import com.transportlogistics.app.delivery.ports.inbound.LastMilePlannerUseCase;
import com.transportlogistics.app.shared.domain.NotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/** Guards the deployed /api/v1 path, not only the controller-relative mapping. */
@SpringBootTest
@AutoConfigureMockMvc
class LastMilePlannerControllerSecurityTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private LastMilePlannerUseCase planner;

    @Test
    @WithMockUser(authorities = "DELIVERY_VIEW")
    void literalApiV1PlannerPathRejectsUsersWithoutPlannerPermissions() throws Exception {
        mockMvc.perform(get("/api/v1/deliveries/{id}/last-mile-planner", UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "DELIVERY_FAIL_VIEW")
    void literalApiV1PlannerPathAllowsFailedDeliveryViewerPastRouteSecurity() throws Exception {
        when(planner.getContext(any())).thenThrow(new NotFoundException("DELIVERY_NOT_FOUND", "Delivery not found"));
        mockMvc.perform(get("/api/v1/deliveries/{id}/last-mile-planner", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = "DELIVERY_EXCEPTION_VIEW")
    void literalApiV1PlannerPathAllowsExceptionViewerPastRouteSecurity() throws Exception {
        when(planner.getContext(any())).thenThrow(new NotFoundException("DELIVERY_NOT_FOUND", "Delivery not found"));
        mockMvc.perform(get("/api/v1/deliveries/{id}/last-mile-planner", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }
}
