package com.transportlogistics.app.fuel.infrastructure.adapters.in.web;

import com.transportlogistics.app.fuel.application.ports.in.FuelPriceUseCase;
import com.transportlogistics.app.fuel.application.ports.in.FuelPurchaseUseCase;
import com.transportlogistics.app.fuel.domain.model.*;
import com.transportlogistics.app.fuel.infrastructure.adapters.in.web.controllers.FuelPurchaseController;
import com.transportlogistics.app.fuel.infrastructure.adapters.in.web.mappers.FuelPurchaseWebMapper;
import com.transportlogistics.app.shared.web.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class FuelPurchaseControllerTest {
    private FuelPurchaseUseCase purchases;
    private MockMvc mvc;
    private UUID vendorId;

    @BeforeEach
    void setUp() {
        purchases = mock(FuelPurchaseUseCase.class);
        vendorId = UUID.randomUUID();
        var mapper = Mappers.getMapper(FuelPurchaseWebMapper.class);
        mvc = MockMvcBuilders.standaloneSetup(new FuelPurchaseController(purchases, mock(FuelPriceUseCase.class), mapper))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void createsDraftAndDoesNotAcceptEditableStatus() throws Exception {
        var p = purchase(FuelPurchaseStatus.DRAFT);
        when(purchases.create(any(), eq("buyer"))).thenReturn(p);
        when(purchases.vendor(vendorId)).thenReturn(new FuelPurchaseUseCase.VendorReference(vendorId, "V1", "Vendor", true));
        mvc.perform(post("/fuel-purchases").principal(() -> "buyer").contentType(MediaType.APPLICATION_JSON).content(body("10")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.purchaseNumber").value("FP-2026-000001"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.vendor.name").value("Vendor"));
    }

    @Test
    void rejectsInvalidQuantityBeforeMutation() throws Exception {
        mvc.perform(post("/fuel-purchases").principal(() -> "buyer").contentType(MediaType.APPLICATION_JSON).content(body("0")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        verifyNoInteractions(purchases);
    }

    @Test
    void delegatesExplicitApproveAction() throws Exception {
        var p = purchase(FuelPurchaseStatus.APPROVED);
        when(purchases.approve(any(), eq("Approved"), eq("manager"))).thenReturn(p);
        when(purchases.vendor(vendorId)).thenReturn(new FuelPurchaseUseCase.VendorReference(vendorId, "V1", "Vendor", true));
        mvc.perform(post("/fuel-purchases/{id}/approve", p.id()).principal(() -> "manager")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"comment\":\"Approved\"}"))
                .andExpect(status().isOk());
        verify(purchases).approve(p.id(), "Approved", "manager");
    }

    private String body(String quantity) {
        return "{\"vendorId\":\"%s\",\"fuelType\":\"DIESEL\",\"purchaseDate\":\"2026-08-16\",\"invoiceNumber\":\"INV-1\",\"invoiceDate\":\"2026-08-16\",\"quantity\":%s,\"unitPrice\":10,\"taxRate\":15,\"otherCharges\":2,\"currencyCode\":\"LKR\"}".formatted(vendorId, quantity);
    }

    private FuelPurchase purchase(FuelPurchaseStatus status) {
        var now = OffsetDateTime.parse("2026-08-16T00:00:00Z");
        return new FuelPurchase(UUID.randomUUID(), "FP-2026-000001", vendorId, null, "DIESEL",
                LocalDate.of(2026, 8, 16), "INV-1", LocalDate.of(2026, 8, 16), new BigDecimal("10"),
                new BigDecimal("10"), new BigDecimal("100"), new BigDecimal("15"), new BigDecimal("15"),
                new BigDecimal("2"), new BigDecimal("117"), "LKR", status, ReconciliationStatus.PENDING,
                null, null, null, null, null, null, null, null, null, null, null, null, null, "notes",
                UUID.randomUUID(), now, now);
    }
}
