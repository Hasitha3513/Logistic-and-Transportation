package com.transportlogistics.app.tenancy.domain.model;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Currency;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TenantTest {
    @Test
    void rejectsIncompleteOrNegativeVersionTenant() {
        var now = OffsetDateTime.parse("2026-08-28T00:00:00Z");
        assertThatThrownBy(() -> new Tenant(UUID.randomUUID(), " ", "CLTS", Currency.getInstance("LKR"),
                "Asia/Colombo", TenantStatus.ACTIVE, now, "system", now, "system", -1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
