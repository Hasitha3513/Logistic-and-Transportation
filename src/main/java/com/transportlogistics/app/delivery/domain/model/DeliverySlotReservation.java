package com.transportlogistics.app.delivery.domain.model;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

public class DeliverySlotReservation {
    private final UUID id;
    private final UUID tenantId;
    private final UUID deliverySlotId;
    private final UUID deliveryOrderId;
    private final DeliverySlotReservationStatus status;
    private final OffsetDateTime reservedAt;
    private final String reservedBy;
    private final OffsetDateTime releasedAt;
    private final String releasedBy;
    private final boolean override;
    private final String overrideReason;
    private final long version;

    public DeliverySlotReservation(
            UUID id,
            UUID tenantId,
            UUID deliverySlotId,
            UUID deliveryOrderId,
            DeliverySlotReservationStatus status,
            OffsetDateTime reservedAt,
            String reservedBy,
            OffsetDateTime releasedAt,
            String releasedBy,
            boolean override,
            String overrideReason,
            long version
    ) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId must not be null");
        this.deliverySlotId = Objects.requireNonNull(deliverySlotId, "deliverySlotId must not be null");
        this.deliveryOrderId = Objects.requireNonNull(deliveryOrderId, "deliveryOrderId must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.reservedAt = Objects.requireNonNull(reservedAt, "reservedAt must not be null");
        this.reservedBy = Objects.requireNonNull(reservedBy, "reservedBy must not be null");
        this.releasedAt = releasedAt;
        this.releasedBy = releasedBy;
        this.override = override;
        this.overrideReason = overrideReason;
        this.version = version;
    }

    public static DeliverySlotReservation create(
            UUID id,
            UUID tenantId,
            UUID deliverySlotId,
            UUID deliveryOrderId,
            OffsetDateTime reservedAt,
            String reservedBy,
            boolean override,
            String overrideReason
    ) {
        return new DeliverySlotReservation(
                id,
                tenantId,
                deliverySlotId,
                deliveryOrderId,
                DeliverySlotReservationStatus.ACTIVE,
                reservedAt,
                reservedBy,
                null,
                null,
                override,
                overrideReason,
                0L
        );
    }

    public DeliverySlotReservation release(OffsetDateTime releasedAt, String releasedBy) {
        return new DeliverySlotReservation(
                id,
                tenantId,
                deliverySlotId,
                deliveryOrderId,
                DeliverySlotReservationStatus.RELEASED,
                reservedAt,
                reservedBy,
                releasedAt,
                releasedBy,
                override,
                overrideReason,
                version
        );
    }

    public DeliverySlotReservation cancel(OffsetDateTime cancelledAt, String cancelledBy) {
        return new DeliverySlotReservation(
                id,
                tenantId,
                deliverySlotId,
                deliveryOrderId,
                DeliverySlotReservationStatus.CANCELLED,
                reservedAt,
                reservedBy,
                cancelledAt,
                cancelledBy,
                override,
                overrideReason,
                version
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
