package com.transportlogistics.app.delivery.adapters.inbound.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transportlogistics.app.delivery.domain.model.*;
import com.transportlogistics.app.delivery.ports.inbound.DeliveryOrderUseCase;
import com.transportlogistics.app.shared.domain.ConflictException;
import com.transportlogistics.app.shared.domain.NotFoundException;
import com.transportlogistics.app.support.PostgreSqlIntegrationTest;
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

@AutoConfigureMockMvc
class DeliveryOrderApiSecurityAcceptanceTest extends PostgreSqlIntegrationTest {

    private static final UUID ID = UUID.fromString("03cd51bf-7ae3-44bd-8202-817fef87341d");
    private static final UUID CUSTOMER = UUID.fromString("f7df5124-4088-450d-8da8-cce83b9a0777");
    private static final UUID ORIGIN = UUID.fromString("5467daf8-cc62-438c-bbc8-a8316684821b");
    private static final UUID DESTINATION = UUID.fromString("66df281c-60fa-443d-b75b-cb47a523f8c3");

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @MockBean private DeliveryOrderUseCase orders;

    @Test
    void unauthenticatedRequestsAreRejected() throws Exception {
        mvc.perform(get("/v1/deliveries")).andExpect(status().isUnauthorized());
        mvc.perform(post("/v1/deliveries").contentType(MediaType.APPLICATION_JSON).content(createBody()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void usersMissingExactDeliveryPermissionsAreForbidden() throws Exception {
        mvc.perform(get("/v1/deliveries")).andExpect(status().isForbidden());
        mvc.perform(post("/v1/deliveries").contentType(MediaType.APPLICATION_JSON).content(createBody()))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/v1/deliveries/{id}", ID).contentType(MediaType.APPLICATION_JSON).content(updateBody(0)))
                .andExpect(status().isForbidden());
        mvc.perform(post("/v1/deliveries/{id}/validate-readiness", ID)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"version\":0}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "DELIVERY_CREATE")
    void authorizedCreateReturnsGeneratedNumberAndValidatesPayload() throws Exception {
        when(orders.create(any(), eq("user"))).thenReturn(order(DeliveryStatus.DRAFT, 0));

        mvc.perform(post("/v1/deliveries").contentType(MediaType.APPLICATION_JSON).content(createBody()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.deliveryNumber").value("DEL-2026-000001"));
        mvc.perform(post("/v1/deliveries").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "DELIVERY_VIEW")
    void authorizedReadSupportsDetailListAndTenantSafeMissing() throws Exception {
        when(orders.get(ID)).thenReturn(order(DeliveryStatus.DRAFT, 0));
        when(orders.search(any())).thenReturn(new DeliveryOrderUseCase.PageResult<>(List.of(order(DeliveryStatus.DRAFT, 0)), 0, 20, 1, 1));

        mvc.perform(get("/v1/deliveries/{id}", ID)).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ID.toString()));
        mvc.perform(get("/v1/deliveries")).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        var crossTenantId = UUID.randomUUID();
        when(orders.get(crossTenantId)).thenThrow(new NotFoundException("DELIVERY_NOT_FOUND", "Delivery Order not found"));
        mvc.perform(get("/v1/deliveries/{id}", crossTenantId)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("DELIVERY_NOT_FOUND"));
    }

    @Test
    @WithMockUser(authorities = "DELIVERY_UPDATE")
    void authorizedUpdateSucceedsAndStaleVersionReturnsConflict() throws Exception {
        when(orders.update(eq(ID), any(), eq("user"))).thenReturn(order(DeliveryStatus.DRAFT, 1))
                .thenThrow(new ConflictException("DELIVERY_VERSION_CONFLICT", "Delivery Order was changed"));

        mvc.perform(patch("/v1/deliveries/{id}", ID).contentType(MediaType.APPLICATION_JSON).content(updateBody(0)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.version").value(1));
        mvc.perform(patch("/v1/deliveries/{id}", ID).contentType(MediaType.APPLICATION_JSON).content(updateBody(0)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("DELIVERY_VERSION_CONFLICT"));
    }

    @Test
    @WithMockUser(authorities = "DELIVERY_ASSIGN")
    void authorizedReadinessSucceedsAndStaleVersionReturnsConflict() throws Exception {
        when(orders.markReady(ID, 0, "user")).thenReturn(order(DeliveryStatus.READY_FOR_ASSIGNMENT, 1))
                .thenThrow(new ConflictException("DELIVERY_VERSION_CONFLICT", "Delivery Order was changed"));

        mvc.perform(post("/v1/deliveries/{id}/validate-readiness", ID)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"version\":0}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("READY_FOR_ASSIGNMENT"));
        mvc.perform(post("/v1/deliveries/{id}/validate-readiness", ID)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"version\":0}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("DELIVERY_VERSION_CONFLICT"));
    }

    private String createBody() throws Exception {
        return body(null);
    }

    private String updateBody(long version) throws Exception {
        return body(version);
    }

    private String body(Long version) throws Exception {
        var body = json.createObjectNode();
        if (version != null) body.put("version", version);
        body.put("customerId", CUSTOMER.toString());
        body.put("originLocationId", ORIGIN.toString());
        body.put("destinationLocationId", DESTINATION.toString());
        body.put("priority", "NORMAL");
        body.put("serviceType", "STANDARD");
        body.put("windowStart", "2026-08-30T10:00:00Z");
        body.put("windowEnd", "2026-08-31T10:00:00Z");
        body.put("instructions", "Handle carefully");
        return json.writeValueAsString(body);
    }

    private DeliveryOrder order(DeliveryStatus status, long version) {
        var now = OffsetDateTime.parse("2026-08-29T10:00:00Z");
        return new DeliveryOrder(new DeliveryId(ID), new DeliveryNumber("DEL-2026-000001"), CUSTOMER, ORIGIN,
                DESTINATION, DeliveryPriority.NORMAL, DeliveryServiceType.STANDARD,
                new DeliveryWindow(now.plusDays(1), now.plusDays(2)), "Handle carefully", status, version,
                now, now, "user", "user");
    }
}
