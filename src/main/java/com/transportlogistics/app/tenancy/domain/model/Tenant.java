package com.transportlogistics.app.tenancy.domain.model;

import java.time.OffsetDateTime;
import java.util.Currency;
import java.util.UUID;

public record Tenant(UUID tenantId, String tenantCode, String tenantName, Currency defaultCurrency,
                     String defaultTimeZone, TenantStatus status, OffsetDateTime createdAt, String createdBy,
                     OffsetDateTime updatedAt, String updatedBy, long version) {
    public Tenant {
        if (tenantId == null || blank(tenantCode) || blank(tenantName) || defaultCurrency == null
                || blank(defaultTimeZone) || status == null || createdAt == null || blank(createdBy)
                || updatedAt == null || blank(updatedBy) || version < 0) {
            throw new IllegalArgumentException("Tenant fields are invalid");
        }
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
