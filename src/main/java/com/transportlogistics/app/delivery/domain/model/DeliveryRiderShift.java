package com.transportlogistics.app.delivery.domain.model;

import com.transportlogistics.app.shared.domain.BusinessRuleException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

public class DeliveryRiderShift {

    private final UUID id;
    private final UUID tenantId;
    private final UUID riderId;
    private final LocalDate shiftDate;
    private final LocalTime startTime;
    private final LocalTime endTime;
    private final UUID deliverySlotId;
    private DeliveryRiderShiftStatus status;
    private final int maxDeliveries;
    private final long version;
    private final OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private final String createdBy;
    private String updatedBy;

    public DeliveryRiderShift(
            UUID id,
            UUID tenantId,
            UUID riderId,
            LocalDate shiftDate,
            LocalTime startTime,
            LocalTime endTime,
            UUID deliverySlotId,
            DeliveryRiderShiftStatus status,
            int maxDeliveries,
            long version,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt,
            String createdBy,
            String updatedBy
    ) {
        if (id == null) throw new BusinessRuleException("DELIVERY_RIDER_SHIFT_ID_REQUIRED", "Shift ID is required");
        if (tenantId == null) throw new BusinessRuleException("DELIVERY_RIDER_TENANT_REQUIRED", "Tenant ID is required");
        if (riderId == null) throw new BusinessRuleException("DELIVERY_RIDER_ID_REQUIRED", "Rider ID is required");
        if (shiftDate == null) throw new BusinessRuleException("DELIVERY_RIDER_SHIFT_DATE_REQUIRED", "Shift date is required");
        if (startTime == null || endTime == null) throw new BusinessRuleException("DELIVERY_RIDER_SHIFT_TIME_REQUIRED", "Start and end times are required");
        if (!startTime.isBefore(endTime)) throw new BusinessRuleException("DELIVERY_RIDER_SHIFT_INVALID_INTERVAL", "Shift start time must be strictly before end time");
        if (maxDeliveries <= 0) throw new BusinessRuleException("DELIVERY_RIDER_SHIFT_INVALID_CAPACITY", "Shift max deliveries must be positive");

        this.id = id;
        this.tenantId = tenantId;
        this.riderId = riderId;
        this.shiftDate = shiftDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.deliverySlotId = deliverySlotId;
        this.status = status != null ? status : DeliveryRiderShiftStatus.SCHEDULED;
        this.maxDeliveries = maxDeliveries;
        this.version = version;
        this.createdAt = createdAt != null ? createdAt : OffsetDateTime.now();
        this.updatedAt = updatedAt != null ? updatedAt : this.createdAt;
        this.createdBy = createdBy != null ? createdBy : "system";
        this.updatedBy = updatedBy != null ? updatedBy : this.createdBy;
    }

    public static DeliveryRiderShift create(
            UUID id,
            UUID tenantId,
            UUID riderId,
            LocalDate shiftDate,
            LocalTime startTime,
            LocalTime endTime,
            UUID deliverySlotId,
            int maxDeliveries,
            String actor,
            OffsetDateTime now
    ) {
        return new DeliveryRiderShift(
                id != null ? id : UUID.randomUUID(),
                tenantId,
                riderId,
                shiftDate,
                startTime,
                endTime,
                deliverySlotId,
                DeliveryRiderShiftStatus.SCHEDULED,
                maxDeliveries > 0 ? maxDeliveries : 5,
                0L,
                now,
                now,
                actor,
                actor
        );
    }

    public boolean overlapsWith(LocalDate date, LocalTime start, LocalTime end) {
        if (!this.shiftDate.equals(date)) {
            return false;
        }
        if (this.status == DeliveryRiderShiftStatus.CANCELLED || this.status == DeliveryRiderShiftStatus.COMPLETED) {
            return false;
        }
        // Half-open interval overlap [startTime, endTime)
        return this.startTime.isBefore(end) && start.isBefore(this.endTime);
    }

    public void startDuty(String actor, OffsetDateTime now) {
        if (this.status != DeliveryRiderShiftStatus.SCHEDULED) {
            throw new BusinessRuleException("DELIVERY_RIDER_SHIFT_INVALID_STATE", "Only scheduled shifts can transition to on-duty");
        }
        this.status = DeliveryRiderShiftStatus.ON_DUTY;
        this.updatedAt = now;
        this.updatedBy = actor != null ? actor : "system";
    }

    public void completeDuty(String actor, OffsetDateTime now) {
        if (this.status != DeliveryRiderShiftStatus.ON_DUTY && this.status != DeliveryRiderShiftStatus.SCHEDULED) {
            throw new BusinessRuleException("DELIVERY_RIDER_SHIFT_INVALID_STATE", "Only scheduled or on-duty shifts can be completed");
        }
        this.status = DeliveryRiderShiftStatus.COMPLETED;
        this.updatedAt = now;
        this.updatedBy = actor != null ? actor : "system";
    }

    public void cancelShift(String actor, OffsetDateTime now) {
        if (this.status == DeliveryRiderShiftStatus.COMPLETED) {
            throw new BusinessRuleException("DELIVERY_RIDER_SHIFT_ALREADY_COMPLETED", "Completed shift cannot be cancelled");
        }
        this.status = DeliveryRiderShiftStatus.CANCELLED;
        this.updatedAt = now;
        this.updatedBy = actor != null ? actor : "system";
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getRiderId() { return riderId; }
    public LocalDate getShiftDate() { return shiftDate; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public UUID getDeliverySlotId() { return deliverySlotId; }
    public DeliveryRiderShiftStatus getStatus() { return status; }
    public int getMaxDeliveries() { return maxDeliveries; }
    public long getVersion() { return version; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public String getCreatedBy() { return createdBy; }
    public String getUpdatedBy() { return updatedBy; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DeliveryRiderShift that = (DeliveryRiderShift) o;
        return Objects.equals(id, that.id) && Objects.equals(tenantId, that.tenantId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, tenantId);
    }
}
