package com.transportlogistics.app.freight.order.adapters.inbound.web;

import com.transportlogistics.app.freight.order.adapters.inbound.web.controllers.FreightOrderController;
import com.transportlogistics.app.freight.order.adapters.inbound.web.mappers.FreightOrderWebMapper;
import com.transportlogistics.app.freight.order.domain.model.FreightOrder;
import com.transportlogistics.app.freight.order.domain.model.FreightOrderLine;
import com.transportlogistics.app.freight.order.ports.inbound.FreightOrderUseCase;
import com.transportlogistics.app.shared.domain.ConflictException;
import com.transportlogistics.app.shared.web.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class FreightOrderControllerTest {
    private FreightOrderUseCase orders;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        orders = mock(FreightOrderUseCase.class);
        FreightOrderWebMapper mapper = Mappers.getMapper(FreightOrderWebMapper.class);
        mvc = MockMvcBuilders.standaloneSetup(new FreightOrderController(orders, mapper))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createsSourceAlignedFreightOrder() throws Exception {
        FreightOrder created = order(0);
        when(orders.create(any(), eq("freight.manager"))).thenReturn(created);

        mvc.perform(post("/v1/freight/orders")
                        .principal(() -> "freight.manager")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(created.id().toString()))
                .andExpect(jsonPath("$.orderNumber").value("FO-2026-000001"))
                .andExpect(jsonPath("$.serviceLevel").value("STANDARD"))
                .andExpect(jsonPath("$.lines[0].description").value("Pallets"))
                .andExpect(jsonPath("$.status").doesNotExist());

        verify(orders).create(argThat(command -> command.lines().size() == 1
                && command.lines().getFirst().quantity().compareTo(new BigDecimal("2.5")) == 0),
                eq("freight.manager"));
    }

    @Test
    void returnsPaginatedSearchResults() throws Exception {
        FreightOrderUseCase.PageResult<FreightOrder> page =
                new FreightOrderUseCase.PageResult<>(List.of(order(0)), 2, 10, 24, 3);
        when(orders.search(any())).thenReturn(page);

        mvc.perform(get("/v1/freight/orders")
                        .param("page", "2")
                        .param("limit", "10")
                        .param("search", "FO-2026")
                        .param("sort", "orderNumber")
                        .param("direction", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].orderNumber").value("FO-2026-000001"))
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.limit").value(10))
                .andExpect(jsonPath("$.totalElements").value(24))
                .andExpect(jsonPath("$.totalPages").value(3));

        verify(orders).search(argThat(query -> query.page() == 2 && query.limit() == 10
                && "FO-2026".equals(query.search()) && "orderNumber".equals(query.sort())
                && "asc".equals(query.direction())));
    }

    @Test
    void updatesUsingExpectedVersionAndActor() throws Exception {
        UUID id = UUID.randomUUID();
        FreightOrder updated = order(1);
        when(orders.update(eq(id), any(), eq("editor"))).thenReturn(updated);

        mvc.perform(patch("/v1/freight/orders/{id}", id)
                        .principal(() -> "editor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"version":0,"priority":"URGENT","specialHandlingInstructions":"Keep upright"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.updatedBy").value("editor"));

        verify(orders).update(eq(id), argThat(command -> command.version() == 0
                && "URGENT".equals(command.priority())), eq("editor"));
    }

    @Test
    void rejectsInvalidRequestBeforeApplicationMutation() throws Exception {
        mvc.perform(post("/v1/freight/orders")
                        .principal(() -> "freight.manager")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"serviceLevel\":\"STANDARD\",\"priority\":\"NORMAL\",\"lines\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors").isArray());

        verifyNoInteractions(orders);
    }

    @Test
    void returnsCurrentApiErrorForStaleUpdate() throws Exception {
        UUID id = UUID.randomUUID();
        when(orders.update(eq(id), any(), eq("editor"))).thenThrow(new ConflictException(
                "FREIGHT_ORDER_CONCURRENT_UPDATE", "Freight order was changed by another user"));

        mvc.perform(patch("/v1/freight/orders/{id}", id)
                        .principal(() -> "editor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":0,\"priority\":\"URGENT\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("FREIGHT_ORDER_CONCURRENT_UPDATE"))
                .andExpect(jsonPath("$.path").value("/v1/freight/orders/" + id));
    }

    private String createJson() {
        return """
                {"customerId":"11111111-1111-4111-8111-111111111111",
                 "originLocationId":"22222222-2222-4222-8222-222222222222",
                 "destinationLocationId":"33333333-3333-4333-8333-333333333333",
                 "requestedPickupAt":"2026-09-01T08:00:00Z",
                 "requestedDeliveryAt":"2026-09-02T08:00:00Z",
                 "serviceLevel":"STANDARD","priority":"NORMAL",
                 "specialHandlingInstructions":"Keep dry",
                 "lines":[{"description":"Pallets","quantity":2.5}]}
                """;
    }

    private FreightOrder order(long version) {
        OffsetDateTime now = OffsetDateTime.parse("2026-08-25T00:00:00Z");
        return new FreightOrder(UUID.randomUUID(), "FO-2026-000001",
                UUID.fromString("11111111-1111-4111-8111-111111111111"),
                UUID.fromString("22222222-2222-4222-8222-222222222222"),
                UUID.fromString("33333333-3333-4333-8333-333333333333"),
                OffsetDateTime.parse("2026-09-01T08:00:00Z"),
                OffsetDateTime.parse("2026-09-02T08:00:00Z"), "STANDARD", "NORMAL", "Keep dry",
                List.of(new FreightOrderLine(UUID.randomUUID(), "Pallets", new BigDecimal("2.5"))),
                version, now, now, "creator", version == 0 ? "creator" : "editor");
    }
}
