package com.transportlogistics.app.delivery.adapters.outbound.persistence;

import com.transportlogistics.app.shared.infrastructure.persistence.TenantScopedEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "delivery_order", uniqueConstraints = @UniqueConstraint(name = "uk_delivery_order_tenant_number",
        columnNames = {"tenant_id", "delivery_number"}))
@Getter @Setter @NoArgsConstructor
class DeliveryOrderEntity extends TenantScopedEntity {
    @Id private UUID id;
    @Column(name = "delivery_number", nullable = false, length = 15) private String deliveryNumber;
    @Column(name = "customer_id", nullable = false) private UUID customerId;
    @Column(name = "origin_location_id", nullable = false) private UUID originLocationId;
    @Column(name = "destination_location_id", nullable = false) private UUID destinationLocationId;
    @Column(nullable = false, length = 20) private String priority;
    @Column(name = "service_type", nullable = false, length = 20) private String serviceType;
    @Column(name = "window_start", nullable = false) private OffsetDateTime windowStart;
    @Column(name = "window_end", nullable = false) private OffsetDateTime windowEnd;
    @Column(columnDefinition = "TEXT") private String instructions;
    @Column(nullable = false, length = 40) private String status;
    @Version @Column(nullable = false) private long version;
    @Column(name = "created_at", nullable = false) private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private OffsetDateTime updatedAt;
    @Column(name = "created_by", nullable = false, length = 128) private String createdBy;
    @Column(name = "updated_by", nullable = false, length = 128) private String updatedBy;
}
