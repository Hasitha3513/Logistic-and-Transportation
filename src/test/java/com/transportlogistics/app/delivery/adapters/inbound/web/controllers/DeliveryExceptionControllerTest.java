package com.transportlogistics.app.delivery.adapters.inbound.web.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transportlogistics.app.delivery.adapters.inbound.web.dto.request.ReportDeliveryExceptionRequest;
import com.transportlogistics.app.delivery.adapters.inbound.web.dto.request.ResolveDeliveryExceptionRequest;
import com.transportlogistics.app.delivery.domain.model.*;
import com.transportlogistics.app.delivery.ports.inbound.DeliveryExceptionUseCase;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class DeliveryExceptionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DeliveryExceptionUseCase exceptionUseCase;

    private final UUID deliveryId = UUID.randomUUID();
    private final UUID exceptionId = UUID.randomUUID();
    private final OffsetDateTime now = OffsetDateTime.now();

    @Test
    @WithMockUser(username = "driver.bob", authorities = {"DELIVERY_EXCEPTION_CREATE"})
    void reportExceptionSucceedsForDriver() throws Exception {
        var dummyCase = new DeliveryExceptionCase(
                exceptionId,
                new DeliveryId(deliveryId),
                null,
                DeliveryExceptionType.DAMAGED_DELIVERY,
                DeliveryExceptionSeverity.HIGH,
                DeliveryExceptionStatus.OPEN,
                "Package torn",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                0L,
                now,
                "driver.bob",
                null,
                null,
                List.of()
        );

        when(exceptionUseCase.reportException(eq(deliveryId), any(), eq("driver.bob")))
                .thenReturn(dummyCase);

        var request = new ReportDeliveryExceptionRequest(
                null,
                DeliveryExceptionType.DAMAGED_DELIVERY,
                DeliveryExceptionSeverity.HIGH,
                "Package torn",
                null,
                null,
                null,
                null,
                null,
                null,
                List.of()
        );

        mockMvc.perform(post("/v1/deliveries/{id}/exceptions", deliveryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(exceptionId.toString()))
                .andExpect(jsonPath("$.exceptionType").value("DAMAGED_DELIVERY"))
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    @WithMockUser(username = "dispatcher.dan", authorities = {"DELIVERY_EXCEPTION_MANAGE"})
    void resolveDeniedForDispatcherWithoutResolvePermission() throws Exception {
        var request = new ResolveDeliveryExceptionRequest(
                0L,
                DeliveryExceptionResolutionCode.RETURN_TO_BASE_APPROVED,
                "Authorize return",
                null,
                DeliveryFailureDisposition.RETURN_TO_BASE_REQUIRED
        );

        mockMvc.perform(post("/v1/deliveries/{id}/exceptions/{exceptionId}/resolve", deliveryId, exceptionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "manager.mary", authorities = {"DELIVERY_EXCEPTION_RESOLVE"})
    void resolveSucceedsForManager() throws Exception {
        var resolution = new DeliveryExceptionResolution(
                DeliveryExceptionResolutionCode.RETURN_TO_BASE_APPROVED,
                "Authorize return",
                DeliveryFailureDisposition.RETURN_TO_BASE_REQUIRED,
                now,
                "manager.mary"
        );
        var resolvedCase = new DeliveryExceptionCase(
                exceptionId,
                new DeliveryId(deliveryId),
                null,
                DeliveryExceptionType.DAMAGED_DELIVERY,
                DeliveryExceptionSeverity.HIGH,
                DeliveryExceptionStatus.RESOLVED,
                "Package torn",
                null,
                null,
                null,
                null,
                null,
                null,
                resolution,
                1L,
                now,
                "driver.bob",
                now,
                "manager.mary",
                List.of()
        );

        when(exceptionUseCase.resolveException(eq(deliveryId), eq(exceptionId), any(), eq("manager.mary")))
                .thenReturn(resolvedCase);

        var request = new ResolveDeliveryExceptionRequest(
                0L,
                DeliveryExceptionResolutionCode.RETURN_TO_BASE_APPROVED,
                "Authorize return",
                null,
                DeliveryFailureDisposition.RETURN_TO_BASE_REQUIRED
        );

        mockMvc.perform(post("/v1/deliveries/{id}/exceptions/{exceptionId}/resolve", deliveryId, exceptionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.resolution.resolutionCode").value("RETURN_TO_BASE_APPROVED"));
    }

    @Test
    @WithMockUser(username = "viewer.val", authorities = {"DELIVERY_EXCEPTION_VIEW"})
    void listExceptionsSucceedsForViewer() throws Exception {
        when(exceptionUseCase.listExceptions(deliveryId)).thenReturn(List.of());

        mockMvc.perform(get("/v1/deliveries/{id}/exceptions", deliveryId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
