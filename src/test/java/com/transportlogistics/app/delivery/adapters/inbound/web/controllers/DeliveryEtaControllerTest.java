package com.transportlogistics.app.delivery.adapters.inbound.web.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transportlogistics.app.delivery.domain.model.*;
import com.transportlogistics.app.delivery.ports.inbound.DeliveryEtaUseCase;
import com.transportlogistics.app.shared.domain.NotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class DeliveryEtaControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private DeliveryEtaUseCase etaUseCase;

    private final UUID orderId = UUID.randomUUID();
    private final UUID batchId = UUID.randomUUID();

    @Test
    @DisplayName("GET /api/v1/deliveries/orders/{orderId}/eta should return 200 with order ETA")
    void getOrderEta_valid_returns200() throws Exception {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        SingleOrderEtaEstimate estimate = new SingleOrderEtaEstimate(
                orderId,
                now.plusMinutes(20),
                1200L,
                5000L,
                EtaStatus.ON_TIME,
                EtaSource.HEURISTIC,
                now,
                now.plusMinutes(15)
        );

        when(etaUseCase.getOrderEta(orderId)).thenReturn(estimate);

        mockMvc.perform(get("/api/v1/deliveries/orders/{orderId}/eta", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId.toString()))
                .andExpect(jsonPath("$.travelDurationSeconds").value(1200))
                .andExpect(jsonPath("$.distanceMeters").value(5000))
                .andExpect(jsonPath("$.slaStatus").value("ON_TIME"))
                .andExpect(jsonPath("$.source").value("HEURISTIC"))
                .andExpect(jsonPath("$.isStale").value(false));
    }

    @Test
    @DisplayName("POST /api/v1/deliveries/orders/{orderId}/eta/calculate should recalculate and return 200")
    void calculateOrderEta_valid_returns200() throws Exception {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        SingleOrderEtaEstimate estimate = new SingleOrderEtaEstimate(
                orderId,
                now.plusMinutes(20),
                1200L,
                5000L,
                EtaStatus.ON_TIME,
                EtaSource.HEURISTIC,
                now,
                now.plusMinutes(15)
        );

        when(etaUseCase.calculateOrderEta(eq(orderId), any())).thenReturn(estimate);

        mockMvc.perform(post("/api/v1/deliveries/orders/{orderId}/eta/calculate", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId.toString()))
                .andExpect(jsonPath("$.slaStatus").value("ON_TIME"));
    }

    @Test
    @DisplayName("GET /api/v1/deliveries/batches/{batchId}/eta should return 200 with multi-stop batch ETA")
    void getBatchEta_valid_returns200() throws Exception {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        BatchEtaStopEstimate stop = new BatchEtaStopEstimate(
                orderId,
                1,
                now.plusMinutes(15),
                600L,
                300L,
                2500L,
                EtaStatus.ON_TIME
        );

        BatchEtaEstimate batchEstimate = new BatchEtaEstimate(
                batchId,
                now,
                now.plusMinutes(15),
                900L,
                2500L,
                now.plusMinutes(15),
                EtaSource.HEURISTIC,
                List.of(stop)
        );

        when(etaUseCase.getBatchEta(batchId)).thenReturn(batchEstimate);

        mockMvc.perform(get("/api/v1/deliveries/batches/{batchId}/eta", batchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.batchId").value(batchId.toString()))
                .andExpect(jsonPath("$.totalDurationSeconds").value(900))
                .andExpect(jsonPath("$.stops").isArray())
                .andExpect(jsonPath("$.stops[0].deliveryOrderId").value(orderId.toString()));
    }
}
