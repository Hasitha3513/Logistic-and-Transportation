package com.transportlogistics.app.fuel.domain.service;

import com.transportlogistics.app.fuel.domain.model.*;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.shared.domain.ConflictException;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class FuelPurchasePolicyTest {
    private final FuelPurchasePolicy policy = new FuelPurchasePolicy();

    @Test void calculatesSubtotalTaxTotalAndHalfUpRounding() {
        var result = policy.calculate(new BigDecimal("3.333"), new BigDecimal("10.005"), new BigDecimal("15"), new BigDecimal("1.005"));
        assertEquals(new BigDecimal("33.35"), result.subtotal());
        assertEquals(new BigDecimal("5.00"), result.taxAmount());
        assertEquals(new BigDecimal("39.36"), result.totalAmount());
    }

    @Test void rejectsInvalidQuantityAndPrice() {
        assertCode("INVALID_FUEL_PURCHASE_QUANTITY", () -> policy.calculate(BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO));
        assertCode("INVALID_FUEL_UNIT_PRICE", () -> policy.calculate(BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
    }

    @Test void validatesPricePeriodAndLifecycle() {
        var price = new FuelPrice(UUID.randomUUID(), UUID.randomUUID(), "DIESEL", LocalDate.of(2026,1,2), LocalDate.of(2026,1,1), BigDecimal.ONE, "LKR", true, OffsetDateTime.now(), OffsetDateTime.now());
        assertCode("INVALID_FUEL_PRICE_PERIOD", () -> policy.validatePrice(price));
        assertThrows(ConflictException.class, () -> policy.requireEditable(purchase(FuelPurchaseStatus.RECONCILED)));
        assertThrows(ConflictException.class, () -> policy.requireCancellable(purchase(FuelPurchaseStatus.RECEIVED), "reason"));
    }

    private FuelPurchase purchase(FuelPurchaseStatus status) {
        var now=OffsetDateTime.now(); return new FuelPurchase(UUID.randomUUID(),"FP",UUID.randomUUID(),null,"DIESEL",LocalDate.now(),"INV",LocalDate.now(),BigDecimal.ONE,BigDecimal.ONE,BigDecimal.ONE,BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ONE,"LKR",status,ReconciliationStatus.PENDING,null,null,null,null,null,null,null,null,null,null,null,null,null,null,UUID.randomUUID(),now,now);
    }
    private void assertCode(String code,Runnable action){var ex=assertThrows(BusinessRuleException.class,action::run);assertEquals(code,ex.code());}
}
