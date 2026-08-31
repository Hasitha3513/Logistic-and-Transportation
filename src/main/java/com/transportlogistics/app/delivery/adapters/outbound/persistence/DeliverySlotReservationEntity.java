package com.transportlogistics.app.delivery.adapters.outbound.persistence;

import com.transportlogistics.app.delivery.domain.model.DeliverySlotReservation;
import com.transportlogistics.app.delivery.domain.model.DeliverySlotReservationStatus;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "delivery_slot_reservation")
public class DeliverySlotReservationEntity {
    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "delivery_slot_id", nullable = false)
    private UUID deliverySlotId;

    @Column(name = "delivery_order_id", nullable = false)
    private UUID deliveryOrderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private DeliverySlotReservationStatus status;

    @Column(name = "reserved_at", nullable = false)
    private OffsetDateTime reservedAt;

    @Column(name = "reserved_by", nullable = false)
    private String reservedBy;

    @Column(name = "released_at")
    private OffsetDateTime releasedAt;

    @Column(name = "released_by")
    private String releasedBy;

    @Column(name = "is_override", nullable = false)
    private boolean override;

    @Column(name = "override_reason")
    private String overrideReason;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    public DeliverySlotReservationEntity() {}

    public static DeliverySlotReservationEntity fromDomain(DeliverySlotReservation reservation) {
        DeliverySlotReservationEntity entity = new DeliverySlotReservationEntity();
        entity.id = reservation.getId();
        entity.tenantId = reservation.getTenantId();
        entity.deliverySlotId = reservation.getDeliverySlotId();
        entity.deliveryOrderId = reservation.getDeliveryOrderId();
        entity.status = reservation.getStatus();
        entity.reservedAt = reservation.getReservedAt();
        entity.reservedBy = reservation.getReservedBy();
        entity.releasedAt = reservation.getReleasedAt();
        entity.releasedBy = reservation.getReleasedBy();
        entity.override = reservation.isOverride();
        entity.overrideReason = reservation.getOverrideReason();
        entity.version = reservation.getVersion();
        return entity;
    }

    public DeliverySlotReservation toDomain() {
        return new DeliverySlotReservation(
                id, tenantId, deliverySlotId, deliveryOrderId, status,
                reservedAt, reservedBy, releasedAt, releasedBy, override, overrideReason, version
        );
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getDeliverySlotId() { return deliverySlotId; }
    public UUID getDeliveryOrderId() { return deliveryOrderId; }
    public DeliverySlotReservationStatus getStatus() { return status; }
    public OffsetDateTime getReservedAt() { return reservedAt; }
    public String getReservedBy() { return reservedBy; }
    public OffsetDateTime getReleasedAt() { return releasedAt; }
    public String getReleasedBy() { return releasedBy; }
    public boolean isOverride() { return override; }
    public String getOverrideReason() { return overrideReason; }
    public long getVersion() { return version; }
}
