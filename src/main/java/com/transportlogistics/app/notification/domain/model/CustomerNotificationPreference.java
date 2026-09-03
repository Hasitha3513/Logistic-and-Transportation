package com.transportlogistics.app.notification.domain.model;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

public record CustomerNotificationPreference(
        UUID id,
        UUID tenantId,
        UUID customerId,
        boolean emailEnabled,
        boolean smsEnabled,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        long version
) {
    public CustomerNotificationPreference {
        Objects.requireNonNull(id, "Preference ID must not be null");
        Objects.requireNonNull(tenantId, "Tenant ID must not be null");
        Objects.requireNonNull(customerId, "Customer ID must not be null");
        Objects.requireNonNull(createdAt, "Created timestamp must not be null");
        Objects.requireNonNull(updatedAt, "Updated timestamp must not be null");
        if (version < 0) throw new IllegalArgumentException("Preference version must not be negative");
    }

    public static CustomerNotificationPreference create(UUID tenantId, UUID customerId,
                                                        boolean emailEnabled, boolean smsEnabled,
                                                        OffsetDateTime now) {
        return new CustomerNotificationPreference(UUID.randomUUID(), tenantId, customerId, emailEnabled,
                smsEnabled, now, now, 0);
    }

    public CustomerNotificationPreference update(boolean emailEnabled, boolean smsEnabled, OffsetDateTime now) {
        return new CustomerNotificationPreference(id, tenantId, customerId, emailEnabled, smsEnabled,
                createdAt, now, version);
    }
}
