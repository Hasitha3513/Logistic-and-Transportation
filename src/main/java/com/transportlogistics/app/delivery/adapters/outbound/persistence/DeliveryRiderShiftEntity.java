package com.transportlogistics.app.delivery.adapters.outbound.persistence;

import com.transportlogistics.app.delivery.domain.model.DeliveryRiderShift;
import com.transportlogistics.app.delivery.domain.model.DeliveryRiderShiftStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "delivery_rider_shift")
public class DeliveryRiderShiftEntity {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "rider_id", nullable = false)
    private UUID riderId;

    @Column(name = "shift_date", nullable = false)
    private LocalDate shiftDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "delivery_slot_id")
    private UUID deliverySlotId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private DeliveryRiderShiftStatus status;

    @Column(name = "max_deliveries", nullable = false)
    private int maxDeliveries;

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

    public DeliveryRiderShiftEntity() {
    }

    public static DeliveryRiderShiftEntity fromDomain(DeliveryRiderShift domain) {
        DeliveryRiderShiftEntity entity = new DeliveryRiderShiftEntity();
        entity.id = domain.getId();
        entity.tenantId = domain.getTenantId();
        entity.riderId = domain.getRiderId();
        entity.shiftDate = domain.getShiftDate();
        entity.startTime = domain.getStartTime();
        entity.endTime = domain.getEndTime();
        entity.deliverySlotId = domain.getDeliverySlotId();
        entity.status = domain.getStatus();
        entity.maxDeliveries = domain.getMaxDeliveries();
        entity.version = domain.getVersion();
        entity.createdAt = domain.getCreatedAt();
        entity.updatedAt = domain.getUpdatedAt();
        entity.createdBy = domain.getCreatedBy();
        entity.updatedBy = domain.getUpdatedBy();
        return entity;
    }

    public DeliveryRiderShift toDomain() {
        return new DeliveryRiderShift(
                id,
                tenantId,
                riderId,
                shiftDate,
                startTime,
                endTime,
                deliverySlotId,
                status,
                maxDeliveries,
                version,
                createdAt,
                updatedAt,
                createdBy,
                updatedBy
        );
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
}
