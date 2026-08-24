package com.transportlogistics.app.fleet.infrastructure.adapters.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "vehicle_meter_reset")
class VehicleMeterResetEntity {
    @Id
    @Column(name = "reset_id", nullable = false)
    private UUID id;

    @Column(name = "vehicle_id", nullable = false)
    private UUID vehicleId;

    @Column(name = "reading_type", nullable = false, length = 30)
    private String readingType;

    @Column(name = "from_epoch", nullable = false)
    private int fromEpoch;

    @Column(name = "to_epoch", nullable = false)
    private int toEpoch;

    @Column(name = "last_reading_value", nullable = false, precision = 19, scale = 3)
    private BigDecimal lastReadingValue;

    @Column(name = "new_meter_value", nullable = false, precision = 19, scale = 3)
    private BigDecimal newMeterValue;

    @Column(name = "effective_at", nullable = false)
    private OffsetDateTime effectiveAt;

    @Column(name = "reason", nullable = false, length = 1000)
    private String reason;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    VehicleMeterResetEntity() {
    }

    VehicleMeterResetEntity(UUID id, UUID vehicleId, String readingType, int fromEpoch, int toEpoch,
                            BigDecimal lastReadingValue, BigDecimal newMeterValue, OffsetDateTime effectiveAt,
                            String reason, UUID createdBy, OffsetDateTime createdAt) {
        this.id = id;
        this.vehicleId = vehicleId;
        this.readingType = readingType;
        this.fromEpoch = fromEpoch;
        this.toEpoch = toEpoch;
        this.lastReadingValue = lastReadingValue;
        this.newMeterValue = newMeterValue;
        this.effectiveAt = effectiveAt;
        this.reason = reason;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public UUID getVehicleId() { return vehicleId; }
    public String getReadingType() { return readingType; }
    public int getFromEpoch() { return fromEpoch; }
    public int getToEpoch() { return toEpoch; }
    public BigDecimal getLastReadingValue() { return lastReadingValue; }
    public BigDecimal getNewMeterValue() { return newMeterValue; }
    public OffsetDateTime getEffectiveAt() { return effectiveAt; }
    public String getReason() { return reason; }
    public UUID getCreatedBy() { return createdBy; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}