package com.transportlogistics.app.delivery.adapters.outbound.persistence;

import com.transportlogistics.app.shared.infrastructure.persistence.TenantScopedEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity @Table(name = "proof_of_delivery")
@Getter @Setter @NoArgsConstructor
class ProofOfDeliveryEntity extends TenantScopedEntity {
    @Id private UUID id;
    @Column(name="delivery_order_id", nullable=false) private UUID deliveryOrderId;
    @Column(nullable=false, length=20) private String status;
    @Column(name="device_captured_at") private OffsetDateTime deviceCapturedAt;
    @Column(precision=10, scale=7) private BigDecimal latitude;
    @Column(precision=10, scale=7) private BigDecimal longitude;
    @Column(name="accuracy_meters", precision=12, scale=3) private BigDecimal accuracyMeters;
    @Column(name="signer_name", length=200) private String signerName;
    @Column(name="signer_relationship", length=100) private String signerRelationship;
    @Column(name="accepted_at") private OffsetDateTime acceptedAt;
    @Column(name="accepted_by", length=128) private String acceptedBy;
    @Version @Column(nullable=false) private long version;
    @Column(name="created_at", nullable=false) private OffsetDateTime createdAt;
    @Column(name="updated_at", nullable=false) private OffsetDateTime updatedAt;
    @Column(name="created_by", nullable=false, length=128) private String createdBy;
    @Column(name="updated_by", nullable=false, length=128) private String updatedBy;
}
