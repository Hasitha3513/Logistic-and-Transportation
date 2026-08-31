package com.transportlogistics.app.delivery.adapters.inbound.web.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transportlogistics.app.delivery.adapters.inbound.web.dto.request.AssignDeliverySlotRequest;
import com.transportlogistics.app.delivery.adapters.inbound.web.dto.request.CreateDeliverySlotRequest;
import com.transportlogistics.app.delivery.adapters.inbound.web.dto.request.UpdateDeliverySlotRequest;
import com.transportlogistics.app.delivery.domain.model.DeliverySlot;
import com.transportlogistics.app.delivery.domain.model.DeliverySlotReservation;
import com.transportlogistics.app.delivery.domain.model.DeliverySlotType;
import com.transportlogistics.app.delivery.ports.inbound.DeliverySlotUseCase;
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
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class DeliverySlotControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DeliverySlotUseCase slotUseCase;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID zoneId = UUID.randomUUID();
    private final LocalDate date = LocalDate.of(2026, 9, 15);
    private final OffsetDateTime now = OffsetDateTime.of(2026, 9, 15, 8, 0, 0, 0, ZoneOffset.UTC);

    @Test
    void shouldCreateSlot() throws Exception {
        UUID slotId = UUID.randomUUID();
        CreateDeliverySlotRequest request = new CreateDeliverySlotRequest(
                zoneId, date, LocalTime.of(9, 0), LocalTime.of(12, 0),
                DeliverySlotType.STANDARD, 10, now.plusHours(1), 15
        );

        DeliverySlot created = DeliverySlot.create(
                slotId, tenantId, zoneId, date,
                LocalTime.of(9, 0), LocalTime.of(12, 0),
                DeliverySlotType.STANDARD, 10, now.plusHours(1), 15, "admin", now
        );

        when(slotUseCase.createSlot(any(), any())).thenReturn(created);

        mockMvc.perform(post("/api/v1/delivery-slots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(slotId.toString()))
                .andExpect(jsonPath("$.maxCapacity").value(10))
                .andExpect(jsonPath("$.remainingCapacity").value(10));
    }

    @Test
    void shouldGetSlotById() throws Exception {
        UUID slotId = UUID.randomUUID();
        DeliverySlot slot = DeliverySlot.create(
                slotId, tenantId, zoneId, date,
                LocalTime.of(9, 0), LocalTime.of(12, 0),
                DeliverySlotType.STANDARD, 10, null, 0, "admin", now
        );

        when(slotUseCase.getSlot(slotId)).thenReturn(slot);

        mockMvc.perform(get("/api/v1/delivery-slots/" + slotId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(slotId.toString()));
    }

    @Test
    void shouldListAvailableSlots() throws Exception {
        UUID slotId = UUID.randomUUID();
        DeliverySlot slot = DeliverySlot.create(
                slotId, tenantId, zoneId, date,
                LocalTime.of(9, 0), LocalTime.of(12, 0),
                DeliverySlotType.STANDARD, 10, null, 0, "admin", now
        );

        when(slotUseCase.getAvailableSlots(zoneId, date)).thenReturn(List.of(slot));

        mockMvc.perform(get("/api/v1/delivery-slots/available")
                        .param("zoneId", zoneId.toString())
                        .param("date", date.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(slotId.toString()));
    }

    @Test
    void shouldAssignDeliveryOrder() throws Exception {
        UUID slotId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        AssignDeliverySlotRequest request = new AssignDeliverySlotRequest(orderId, false, null);

        DeliverySlotReservation reservation = DeliverySlotReservation.create(
                UUID.randomUUID(), tenantId, slotId, orderId, now, "admin", false, null
        );

        when(slotUseCase.assignDeliveryOrder(eq(slotId), any(), any())).thenReturn(reservation);

        mockMvc.perform(post("/api/v1/delivery-slots/" + slotId + "/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.deliveryOrderId").value(orderId.toString()))
                .andExpect(jsonPath("$.deliverySlotId").value(slotId.toString()));
    }
}
