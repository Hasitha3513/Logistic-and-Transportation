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
@Table(name = "delivery_batch_order")
@Getter
@Setter
@NoArgsConstructor
public class DeliveryBatchOrderEntity {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "batch_id", nullable = false)
    private UUID batchId;

    @Column(name = "delivery_order_id", nullable = false)
    private UUID deliveryOrderId;

    @Column(name = "sequence_hint")
    private Integer sequenceHint;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "added_at", nullable = false)
    private OffsetDateTime addedAt;

    @Column(name = "added_by", nullable = false, length = 255)
    private String addedBy;

    @Column(name = "removed_at")
    private OffsetDateTime removedAt;

    @Column(name = "removed_by", length = 255)
    private String removedBy;

    @Version
    @Column(name = "version", nullable = false)
    private long version;
}
