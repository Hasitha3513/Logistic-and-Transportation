package com.transportlogistics.app.shared.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.TenantId;

import java.util.UUID;

@MappedSuperclass
@Getter
@Setter
public abstract class TenantScopedEntity {
    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false)
    protected UUID tenantId;
}
