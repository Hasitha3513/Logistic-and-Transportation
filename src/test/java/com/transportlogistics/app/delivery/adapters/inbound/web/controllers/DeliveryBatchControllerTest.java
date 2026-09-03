package com.transportlogistics.app.delivery.adapters.inbound.web.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transportlogistics.app.delivery.adapters.inbound.web.dto.request.AddOrdersToBatchRequest;
import com.transportlogistics.app.delivery.adapters.inbound.web.dto.request.AssignRiderToBatchRequest;
import com.transportlogistics.app.delivery.adapters.inbound.web.dto.request.AutoClusterBatchesRequest;
import com.transportlogistics.app.delivery.adapters.inbound.web.dto.request.CreateDeliveryBatchRequest;
import com.transportlogistics.app.delivery.domain.model.DeliveryBatch;
import com.transportlogistics.app.delivery.domain.model.DeliveryBatchCode;
import com.transportlogistics.app.delivery.domain.model.DeliveryBatchOrder;
import com.transportlogistics.app.delivery.domain.model.DeliveryBatchOrderStatus;
import com.transportlogistics.app.delivery.domain.model.DeliveryBatchStatus;
import com.transportlogistics.app.delivery.ports.inbound.DeliveryBatchUseCase;
import com.transportlogistics.app.shared.domain.ConflictException;
import com.transportlogistics.app.shared.domain.NotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class DeliveryBatchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DeliveryBatchUseCase batchUseCase;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID batchId = UUID.randomUUID();
    private final UUID zoneId = UUID.randomUUID();
    private final UUID slotId = UUID.randomUUID();
    private final UUID riderId = UUID.randomUUID();
    private final UUID orderId = UUID.randomUUID();

    private DeliveryBatch createBatch(DeliveryBatchStatus status) {
        return new DeliveryBatch(
                batchId,
                tenantId,
                new DeliveryBatchCode("BAT-2026-000001"),
                zoneId,
                slotId,
                riderId,
                status,
                5,
                0L,
                OffsetDateTime.now(),
                OffsetDateTime.now(),
                "admin",
                "admin"
        );
    }

    @Test
    @DisplayName("POST /api/v1/deliveries/batches - Create batch succeeds")
    void createBatch_succeeds() throws Exception {
        when(batchUseCase.createBatch(any())).thenReturn(createBatch(DeliveryBatchStatus.DRAFT));

        CreateDeliveryBatchRequest request = new CreateDeliveryBatchRequest(
                zoneId, slotId, 5, List.of(orderId), null
        );

        mockMvc.perform(post("/api/v1/deliveries/batches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(batchId.toString()))
                .andExpect(jsonPath("$.batchCode").value("BAT-2026-000001"))
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    @DisplayName("GET /api/v1/deliveries/batches/{id} - Found batch returns 200")
    void getBatch_found_returns200() throws Exception {
        when(batchUseCase.getBatch(batchId)).thenReturn(createBatch(DeliveryBatchStatus.DRAFT));

        mockMvc.perform(get("/api/v1/deliveries/batches/" + batchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(batchId.toString()))
                .andExpect(jsonPath("$.deliveryZoneId").value(zoneId.toString()));
    }

    @Test
    @DisplayName("GET /api/v1/deliveries/batches/{id} - Not found returns 404")
    void getBatch_notFound_returns404() throws Exception {
        when(batchUseCase.getBatch(batchId)).thenThrow(new NotFoundException("DELIVERY_BATCH_NOT_FOUND", "Batch not found"));

        mockMvc.perform(get("/api/v1/deliveries/batches/" + batchId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/v1/deliveries/batches/{id}/assign-rider - Assign rider succeeds")
    void assignRider_succeeds() throws Exception {
        when(batchUseCase.assignRider(eq(batchId), any())).thenReturn(createBatch(DeliveryBatchStatus.ASSIGNED));

        AssignRiderToBatchRequest request = new AssignRiderToBatchRequest(riderId, false, null);

        mockMvc.perform(post("/api/v1/deliveries/batches/" + batchId + "/assign-rider")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ASSIGNED"))
                .andExpect(jsonPath("$.riderId").value(riderId.toString()));
    }

    @Test
    @DisplayName("POST /api/v1/deliveries/batches/{id}/dispatch - Dispatch succeeds")
    void dispatchBatch_succeeds() throws Exception {
        when(batchUseCase.dispatchBatch(batchId)).thenReturn(createBatch(DeliveryBatchStatus.DISPATCHED));

        mockMvc.perform(post("/api/v1/deliveries/batches/" + batchId + "/dispatch"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISPATCHED"));
    }

    @Test
    @DisplayName("POST /api/v1/deliveries/batches/{id}/cancel - Cancel succeeds")
    void cancelBatch_succeeds() throws Exception {
        when(batchUseCase.cancelBatch(batchId)).thenReturn(createBatch(DeliveryBatchStatus.CANCELLED));

        mockMvc.perform(post("/api/v1/deliveries/batches/" + batchId + "/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @DisplayName("GET /api/v1/deliveries/batches/{id}/orders - Returns batch order memberships")
    void getBatchOrders_succeeds() throws Exception {
        DeliveryBatchOrder orderMembership = new DeliveryBatchOrder(
                UUID.randomUUID(), tenantId, batchId, orderId, 1, DeliveryBatchOrderStatus.ACTIVE,
                OffsetDateTime.now(), "admin", null, null, 0L
        );
        when(batchUseCase.getBatchOrderMemberships(batchId)).thenReturn(List.of(orderMembership));

        mockMvc.perform(get("/api/v1/deliveries/batches/" + batchId + "/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].batchId").value(batchId.toString()))
                .andExpect(jsonPath("$[0].deliveryOrderId").value(orderId.toString()))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));
    }
}
