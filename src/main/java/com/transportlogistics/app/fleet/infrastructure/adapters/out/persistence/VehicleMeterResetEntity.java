package com.transportlogistics.app.fleet.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.fleet.domain.model.VehicleReadingType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "vehicle_meter_reset")
@Getter
@Setter
@NoArgsConstructor
class VehicleMeterResetEntity {
    @Id
    @Column(name = "reset_id")
    private UUID id;

    @Column(name = "vehicle_id", nullable = false)
    private UUID vehicleId;

    @Enumerated(EnumType.STRING)
    @Column(name = "reading_type", nullable = false, length = 30)
    private VehicleReadingType readingType;

    @Column(name = "previous_reading_id")
    private UUID previousReadingId;

    @Column(name = "previous_meter_value", nullable = false, precision = 19, scale = 3)
    private BigDecimal previousMeterValue;

    @Column(name = "new_reading_id", nullable = false)
    private UUID newReadingId;

    @Column(name = "new_meter_value", nullable = false, precision = 19, scale = 3)
    private BigDecimal newMeterValue;

    @Column(name = "effective_at", nullable = false)
    private OffsetDateTime effectiveAt;

    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
}
