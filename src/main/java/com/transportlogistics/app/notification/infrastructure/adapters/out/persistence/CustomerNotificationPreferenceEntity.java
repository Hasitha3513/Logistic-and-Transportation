package com.transportlogistics.app.notification.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.notification.domain.model.CustomerNotificationPreference;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "customer_notification_preference")
class CustomerNotificationPreferenceEntity extends com.transportlogistics.app.shared.infrastructure.persistence.TenantScopedEntity {
    @Id private UUID id;
    @Column(name = "customer_id", nullable = false) private UUID customerId;
    @Column(name = "email_enabled", nullable = false) private boolean emailEnabled;
    @Column(name = "sms_enabled", nullable = false) private boolean smsEnabled;
    @Column(name = "created_at", nullable = false) private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private OffsetDateTime updatedAt;
    @Version @Column(nullable = false) private long version;

    protected CustomerNotificationPreferenceEntity() {
    }

    static CustomerNotificationPreferenceEntity fromDomain(CustomerNotificationPreference value) {
        var entity = new CustomerNotificationPreferenceEntity();
        entity.id = value.id();
        entity.setTenantId(value.tenantId());
        entity.customerId = value.customerId();
        entity.emailEnabled = value.emailEnabled();
        entity.smsEnabled = value.smsEnabled();
        entity.createdAt = value.createdAt();
        entity.updatedAt = value.updatedAt();
        entity.version = value.version();
        return entity;
    }

    CustomerNotificationPreference toDomain() {
        return new CustomerNotificationPreference(id, getTenantId(), customerId, emailEnabled, smsEnabled,
                createdAt, updatedAt, version);
    }
}
