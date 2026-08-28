package com.transportlogistics.app.tenancy.infrastructure.adapters.out.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "tenant")
@Getter
@Setter
@NoArgsConstructor
class TenantEntity {
    @Id
    @Column(name = "tenant_id")
    private UUID tenantId;
    @Column(name = "tenant_code")
    private String tenantCode;
    @Column(name = "tenant_name")
    private String tenantName;
    @Column(name = "default_currency")
    private String defaultCurrency;
    @Column(name = "default_time_zone")
    private String defaultTimeZone;
    @Enumerated(EnumType.STRING)
    private com.transportlogistics.app.tenancy.domain.model.TenantStatus status;
    @Column(name = "created_at")
    private OffsetDateTime createdAt;
    @Column(name = "created_by")
    private String createdBy;
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
    @Column(name = "updated_by")
    private String updatedBy;
    @Version
    private long version;
}
