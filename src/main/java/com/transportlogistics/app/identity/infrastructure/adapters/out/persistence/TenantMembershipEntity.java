package com.transportlogistics.app.identity.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.identity.domain.model.TenantMembershipStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "tenant_membership")
@Getter
@Setter
@NoArgsConstructor
class TenantMembershipEntity {
    @Id
    @Column(name = "membership_id")
    private UUID membershipId;
    @Column(name = "tenant_id")
    private UUID tenantId;
    @Column(name = "user_id")
    private UUID userId;
    @Enumerated(EnumType.STRING)
    private TenantMembershipStatus status;
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
