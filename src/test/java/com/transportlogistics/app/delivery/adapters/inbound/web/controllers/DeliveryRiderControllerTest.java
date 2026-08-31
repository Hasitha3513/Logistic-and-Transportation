package com.transportlogistics.app.delivery.adapters.inbound.web.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transportlogistics.app.delivery.adapters.inbound.web.dto.request.AssignRiderToDeliveryRequest;
import com.transportlogistics.app.delivery.adapters.inbound.web.dto.request.CreateDeliveryRiderShiftRequest;
import com.transportlogistics.app.delivery.adapters.inbound.web.dto.request.DeliveryRiderDutyStatusRequest;
import com.transportlogistics.app.delivery.adapters.inbound.web.dto.request.OnboardDeliveryRiderRequest;
import com.transportlogistics.app.delivery.adapters.inbound.web.dto.request.ReassignRiderToDeliveryRequest;
import com.transportlogistics.app.delivery.adapters.inbound.web.dto.request.UpdateDeliveryRiderRequest;
import com.transportlogistics.app.delivery.domain.model.DeliveryOrderRiderAssignment;
import com.transportlogistics.app.delivery.domain.model.DeliveryRider;
import com.transportlogistics.app.delivery.domain.model.DeliveryRiderAvailability;
import com.transportlogistics.app.delivery.domain.model.DeliveryRiderShift;
import com.transportlogistics.app.delivery.domain.model.DeliveryRiderStatus;
import com.transportlogistics.app.delivery.domain.model.DeliveryRiderType;
import com.transportlogistics.app.delivery.ports.inbound.DeliveryRiderUseCase;
import com.transportlogistics.app.delivery.ports.outbound.DriverEligibilityPort;
import com.transportlogistics.app.shared.domain.ConflictException;
import com.transportlogistics.app.shared.domain.NotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class DeliveryRiderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DeliveryRiderUseCase riderUseCase;

    private final UUID riderId = UUID.randomUUID();
    private final UUID driverId = UUID.randomUUID();
    private final UUID zoneId = UUID.randomUUID();
    private final OffsetDateTime now = OffsetDateTime.now();

    @Test
    @DisplayName("Should onboard rider with DELIVERY_RIDER_CREATE authority")
    @WithMockUser(authorities = "DELIVERY_RIDER_CREATE")
    void onboardRider_authorized_returnsCreated() throws Exception {
        OnboardDeliveryRiderRequest request = new OnboardDeliveryRiderRequest(
                "RDR-001", driverId, DeliveryRiderType.FULL_TIME, zoneId, Set.of(), 5
        );

        DeliveryRider rider = DeliveryRider.create(
                riderId, UUID.randomUUID(), "RDR-001", driverId, DeliveryRiderType.FULL_TIME, zoneId, Set.of(), 5, "admin", now
        );

        when(riderUseCase.onboardRider(any(), any())).thenReturn(rider);

        mockMvc.perform(post("/api/v1/delivery-riders")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.riderCode").value("RDR-001"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("Should list riders with safe summary projection and availability")
    @WithMockUser(authorities = "DELIVERY_RIDER_VIEW")
    void listRiders_authorized() throws Exception {
        DeliveryRiderUseCase.DeliveryRiderSummary summary = new DeliveryRiderUseCase.DeliveryRiderSummary(
                riderId, "RDR-001", driverId,
                new DriverEligibilityPort.DriverSummary(driverId, "EMP-100", "John", "Doe", "AVAILABLE", true),
                DeliveryRiderType.FULL_TIME, DeliveryRiderStatus.ACTIVE, DeliveryRiderAvailability.AVAILABLE,
                zoneId, Set.of(), 2, 5, Optional.empty()
        );

        when(riderUseCase.listRiders(any(), any(), any())).thenReturn(List.of(summary));

        mockMvc.perform(get("/api/v1/delivery-riders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].riderCode").value("RDR-001"))
                .andExpect(jsonPath("$[0].driverName").value("John Doe"))
                .andExpect(jsonPath("$[0].availability").value("AVAILABLE"))
                .andExpect(jsonPath("$[0].activeWorkload").value(2));
    }

    @Test
    @DisplayName("Should assign rider to delivery order with DELIVERY_RIDER_ASSIGN authority")
    @WithMockUser(authorities = "DELIVERY_RIDER_ASSIGN")
    void assignRider_authorized_returnsCreated() throws Exception {
        UUID orderId = UUID.randomUUID();
        AssignRiderToDeliveryRequest request = new AssignRiderToDeliveryRequest(riderId, false, null);

        DeliveryOrderRiderAssignment assignment = DeliveryOrderRiderAssignment.create(
                UUID.randomUUID(), UUID.randomUUID(), orderId, riderId, false, null, "dispatcher", now
        );

        when(riderUseCase.assignRider(eq(orderId), any(), any())).thenReturn(assignment);

        mockMvc.perform(post("/api/v1/deliveries/{id}/assign-rider", orderId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.riderId").value(riderId.toString()))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("Should reassign rider and return updated assignment")
    @WithMockUser(authorities = "DELIVERY_RIDER_ASSIGN")
    void reassignRider_authorized() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID newRiderId = UUID.randomUUID();
        ReassignRiderToDeliveryRequest request = new ReassignRiderToDeliveryRequest(newRiderId, true, "Shift change");

        DeliveryOrderRiderAssignment assignment = DeliveryOrderRiderAssignment.create(
                UUID.randomUUID(), UUID.randomUUID(), orderId, newRiderId, true, "Shift change", "dispatcher", now
        );

        when(riderUseCase.reassignRider(eq(orderId), any(), any())).thenReturn(assignment);

        mockMvc.perform(post("/api/v1/deliveries/{id}/reassign-rider", orderId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.riderId").value(newRiderId.toString()))
                .andExpect(jsonPath("$.isOverride").value(true));
    }
}
