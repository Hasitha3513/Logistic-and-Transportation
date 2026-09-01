package com.transportlogistics.app.delivery.adapters.outbound.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "delivery_batch")
@Getter
@Setter
@NoArgsConstructor
public class DeliveryBatchEntity {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "batch_code", nullable = false, length = 40)
    private String batchCode;

    @Column(name = "delivery_zone_id", nullable = false)
    private UUID deliveryZoneId;

    @Column(name = "delivery_slot_id")
    private UUID deliverySlotId;

    @Column(name = "rider_id")
    private UUID riderId;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "max_batch_size", nullable = false)
    private int maxBatchSize;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "created_by", length = 255)
    private String createdBy;

    @Column(name = "updated_by", length = 255)
    private String updatedBy;
}
