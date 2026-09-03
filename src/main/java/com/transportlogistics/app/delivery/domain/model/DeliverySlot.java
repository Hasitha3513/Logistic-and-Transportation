package com.transportlogistics.app.delivery.domain.model;

import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.shared.domain.ConflictException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

public class DeliverySlot {
    private final UUID id;
    private final UUID tenantId;
    private final UUID deliveryZoneId;
    private final LocalDate slotDate;
    private final LocalTime startTime;
    private final LocalTime endTime;
    private final DeliverySlotType slotType;
    private final int maxCapacity;
    private final int reservedCapacity;
    private final OffsetDateTime cutoffTime;
    private final int bufferMinutes;
    private final DeliverySlotStatus status;
    private final long version;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime updatedAt;
    private final String createdBy;
    private final String updatedBy;

    public DeliverySlot(
            UUID id,
            UUID tenantId,
            UUID deliveryZoneId,
            LocalDate slotDate,
            LocalTime startTime,
            LocalTime endTime,
            DeliverySlotType slotType,
            int maxCapacity,
            int reservedCapacity,
            OffsetDateTime cutoffTime,
            int bufferMinutes,
            DeliverySlotStatus status,
            long version,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt,
            String createdBy,
            String updatedBy
    ) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId must not be null");
        this.deliveryZoneId = Objects.requireNonNull(deliveryZoneId, "deliveryZoneId must not be null");
        this.slotDate = Objects.requireNonNull(slotDate, "slotDate must not be null");
        this.startTime = Objects.requireNonNull(startTime, "startTime must not be null");
        this.endTime = Objects.requireNonNull(endTime, "endTime must not be null");
        this.slotType = slotType != null ? slotType : DeliverySlotType.STANDARD;
        if (!startTime.isBefore(endTime)) {
            throw new BusinessRuleException("DELIVERY_SLOT_INVALID_WINDOW", "Start time must be strictly before end time");
        }
        if (maxCapacity <= 0) {
            throw new BusinessRuleException("DELIVERY_SLOT_INVALID_CAPACITY", "Max capacity must be greater than zero");
        }
        if (reservedCapacity < 0) {
            throw new BusinessRuleException("DELIVERY_SLOT_INVALID_CAPACITY", "Reserved capacity cannot be negative");
        }
        if (bufferMinutes < 0) {
            throw new BusinessRuleException("DELIVERY_SLOT_INVALID_BUFFER", "Buffer minutes cannot be negative");
        }
        this.maxCapacity = maxCapacity;
        this.reservedCapacity = reservedCapacity;
        this.cutoffTime = cutoffTime;
        this.bufferMinutes = bufferMinutes;
        this.status = status != null ? status : DeliverySlotStatus.ACTIVE;
        this.version = version;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
    }

    public static DeliverySlot create(
            UUID id,
            UUID tenantId,
            UUID deliveryZoneId,
            LocalDate slotDate,
            LocalTime startTime,
            LocalTime endTime,
            DeliverySlotType slotType,
            int maxCapacity,
            OffsetDateTime cutoffTime,
            int bufferMinutes,
            String actor,
            OffsetDateTime now
    ) {
        return new DeliverySlot(
                id,
                tenantId,
                deliveryZoneId,
                slotDate,
                startTime,
                endTime,
                slotType,
                maxCapacity,
                0,
                cutoffTime,
                bufferMinutes,
                DeliverySlotStatus.ACTIVE,
                0L,
                now,
                now,
                actor,
                actor
        );
    }

    public boolean overlapsWith(DeliverySlot other) {
        if (!this.tenantId.equals(other.tenantId)
                || !this.deliveryZoneId.equals(other.deliveryZoneId)
                || !this.slotDate.equals(other.slotDate)
                || this.slotType != other.slotType
                || this.status != DeliverySlotStatus.ACTIVE
                || other.status != DeliverySlotStatus.ACTIVE) {
            return false;
        }
        return this.startTime.isBefore(other.endTime) && other.startTime.isBefore(this.endTime);
    }

    public boolean isAvailableForBooking(OffsetDateTime now, boolean isOverride) {
        if (this.status != DeliverySlotStatus.ACTIVE) {
            return false;
        }
        if (cutoffTime != null && now.isAfter(cutoffTime) && !isOverride) {
            return false;
        }
        return isOverride || reservedCapacity < maxCapacity;
    }

    public int getRemainingCapacity() {
        return Math.max(0, maxCapacity - reservedCapacity);
    }

    public DeliverySlot reserve(boolean isOverride, String overrideReason, String actor, OffsetDateTime now) {
        if (this.status != DeliverySlotStatus.ACTIVE) {
            throw new ConflictException("DELIVERY_SLOT_INACTIVE", "Cannot book into an inactive or closed slot");
        }
        if (cutoffTime != null && now.isAfter(cutoffTime) && !isOverride) {
            throw new ConflictException("DELIVERY_SLOT_CUTOFF_EXCEEDED", "Slot booking cutoff time has passed");
        }
        if (reservedCapacity >= maxCapacity && !isOverride) {
            throw new ConflictException("DELIVERY_SLOT_CAPACITY_EXCEEDED", "Slot capacity has been reached");
        }
        if (isOverride && (overrideReason == null || overrideReason.trim().isBlank())) {
            throw new BusinessRuleException("DELIVERY_SLOT_OVERRIDE_REASON_REQUIRED", "Override reason is mandatory for overbooking");
        }
        return new DeliverySlot(
                id, tenantId, deliveryZoneId, slotDate, startTime, endTime, slotType,
                maxCapacity, reservedCapacity + 1, cutoffTime, bufferMinutes, status,
                version, createdAt, now, createdBy, actor
        );
    }

    public DeliverySlot release(String actor, OffsetDateTime now) {
        if (reservedCapacity <= 0) {
            return this;
        }
        return new DeliverySlot(
                id, tenantId, deliveryZoneId, slotDate, startTime, endTime, slotType,
                maxCapacity, reservedCapacity - 1, cutoffTime, bufferMinutes, status,
                version, createdAt, now, createdBy, actor
        );
    }

    public DeliverySlot update(
            LocalTime newStartTime,
            LocalTime newEndTime,
            DeliverySlotType newSlotType,
            int newMaxCapacity,
            OffsetDateTime newCutoffTime,
            int newBufferMinutes,
            long expectedVersion,
            String actor,
            OffsetDateTime now
    ) {
        if (this.version != expectedVersion) {
            throw new ConflictException("DELIVERY_SLOT_VERSION_CONFLICT", "Slot was modified concurrently");
        }
        if (reservedCapacity > 0 && (!this.startTime.equals(newStartTime) || !this.endTime.equals(newEndTime))) {
            throw new ConflictException("DELIVERY_SLOT_WINDOW_EDIT_PROHIBITED", "Cannot change time window when active reservations exist");
        }
        if (newMaxCapacity < reservedCapacity) {
            throw new ConflictException("DELIVERY_SLOT_CAPACITY_REDUCTION_BELOW_RESERVED", "New capacity cannot be lower than current active reservations (" + reservedCapacity + ")");
        }
        return new DeliverySlot(
                id, tenantId, deliveryZoneId, slotDate, newStartTime, newEndTime,
                newSlotType != null ? newSlotType : this.slotType,
                newMaxCapacity, reservedCapacity, newCutoffTime, newBufferMinutes, status,
                version, createdAt, now, createdBy, actor
        );
    }

    public DeliverySlot activate(long expectedVersion, String actor, OffsetDateTime now) {
        if (this.version != expectedVersion) {
            throw new ConflictException("DELIVERY_SLOT_VERSION_CONFLICT", "Slot was modified concurrently");
        }
        return new DeliverySlot(
                id, tenantId, deliveryZoneId, slotDate, startTime, endTime, slotType,
                maxCapacity, reservedCapacity, cutoffTime, bufferMinutes, DeliverySlotStatus.ACTIVE,
                version, createdAt, now, createdBy, actor
        );
    }

    public DeliverySlot deactivate(long expectedVersion, String actor, OffsetDateTime now) {
        if (this.version != expectedVersion) {
            throw new ConflictException("DELIVERY_SLOT_VERSION_CONFLICT", "Slot was modified concurrently");
        }
        return new DeliverySlot(
                id, tenantId, deliveryZoneId, slotDate, startTime, endTime, slotType,
                maxCapacity, reservedCapacity, cutoffTime, bufferMinutes, DeliverySlotStatus.INACTIVE,
                version, createdAt, now, createdBy, actor
        );
    }

    public DeliverySlot close(long expectedVersion, String actor, OffsetDateTime now) {
        if (this.version != expectedVersion) {
            throw new ConflictException("DELIVERY_SLOT_VERSION_CONFLICT", "Slot was modified concurrently");
        }
        return new DeliverySlot(
                id, tenantId, deliveryZoneId, slotDate, startTime, endTime, slotType,
                maxCapacity, reservedCapacity, cutoffTime, bufferMinutes, DeliverySlotStatus.CLOSED,
                version, createdAt, now, createdBy, actor
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
