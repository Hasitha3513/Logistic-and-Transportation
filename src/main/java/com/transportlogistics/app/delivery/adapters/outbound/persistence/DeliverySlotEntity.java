package com.transportlogistics.app.delivery.adapters.outbound.persistence;

import com.transportlogistics.app.delivery.domain.model.DeliverySlot;
import com.transportlogistics.app.delivery.domain.model.DeliverySlotStatus;
import com.transportlogistics.app.delivery.domain.model.DeliverySlotType;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "delivery_slot")
public class DeliverySlotEntity {
    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "delivery_zone_id", nullable = false)
    private UUID deliveryZoneId;

    @Column(name = "slot_date", nullable = false)
    private LocalDate slotDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "slot_type", nullable = false)
    private DeliverySlotType slotType;

    @Column(name = "max_capacity", nullable = false)
    private int maxCapacity;

    @Column(name = "reserved_capacity", nullable = false)
    private int reservedCapacity;

    @Column(name = "cutoff_time")
    private OffsetDateTime cutoffTime;

    @Column(name = "buffer_minutes", nullable = false)
    private int bufferMinutes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private DeliverySlotStatus status;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;

    public DeliverySlotEntity() {}

    public static DeliverySlotEntity fromDomain(DeliverySlot slot) {
        DeliverySlotEntity entity = new DeliverySlotEntity();
        entity.id = slot.getId();
        entity.tenantId = slot.getTenantId();
        entity.deliveryZoneId = slot.getDeliveryZoneId();
        entity.slotDate = slot.getSlotDate();
        entity.startTime = slot.getStartTime();
        entity.endTime = slot.getEndTime();
        entity.slotType = slot.getSlotType();
        entity.maxCapacity = slot.getMaxCapacity();
        entity.reservedCapacity = slot.getReservedCapacity();
        entity.cutoffTime = slot.getCutoffTime();
        entity.bufferMinutes = slot.getBufferMinutes();
        entity.status = slot.getStatus();
        entity.version = slot.getVersion();
        entity.createdAt = slot.getCreatedAt();
        entity.updatedAt = slot.getUpdatedAt();
        entity.createdBy = slot.getCreatedBy();
        entity.updatedBy = slot.getUpdatedBy();
        return entity;
    }

    public DeliverySlot toDomain() {
        return new DeliverySlot(
                id, tenantId, deliveryZoneId, slotDate, startTime, endTime,
                slotType, maxCapacity, reservedCapacity, cutoffTime, bufferMinutes,
                status, version, createdAt, updatedAt, createdBy, updatedBy
        );
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getDeliveryZoneId() { return deliveryZoneId; }
    public LocalDate getSlotDate() { return slotDate; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public DeliverySlotType getSlotType() { return slotType; }
    public int getMaxCapacity() { return maxCapacity; }
    public int getReservedCapacity() { return reservedCapacity; }
    public OffsetDateTime getCutoffTime() { return cutoffTime; }
    public int getBufferMinutes() { return bufferMinutes; }
    public DeliverySlotStatus getStatus() { return status; }
    public long getVersion() { return version; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public String getCreatedBy() { return createdBy; }
    public String getUpdatedBy() { return updatedBy; }
}
