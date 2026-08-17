package com.transportlogistics.app.fleet.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.fleet.domain.model.VehicleReadingSourceType;
import com.transportlogistics.app.fleet.domain.model.VehicleReadingType;
import com.transportlogistics.app.fleet.domain.model.VehicleReadingUnit;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "vehicle_reading")
@Getter
@Setter
@NoArgsConstructor
class VehicleReadingEntity {
    @Id
    @Column(name = "reading_id")
    private UUID id;

    @Column(name = "vehicle_id", nullable = false)
    private UUID vehicleId;

    @Enumerated(EnumType.STRING)
    @Column(name = "reading_type", nullable = false, length = 30)
    private VehicleReadingType readingType;

    @Column(name = "value", nullable = false, precision = 19, scale = 3)
    private BigDecimal value;

    @Enumerated(EnumType.STRING)
    @Column(name = "unit", nullable = false, length = 30)
    private VehicleReadingUnit unit;

    @Column(name = "meter_epoch", nullable = false)
    private int meterEpoch;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 30)
    private VehicleReadingSourceType sourceType;

    @Column(name = "source_reference_id")
    private UUID sourceReferenceId;

    @Column(name = "recorded_at", nullable = false)
    private OffsetDateTime recordedAt;

    @Column(name = "received_at", nullable = false)
    private OffsetDateTime receivedAt;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "correction_of_reading_id")
    private UUID correctionOfReadingId;

    @Column(name = "correction_reason", columnDefinition = "TEXT")
    private String correctionReason;

    @Column(name = "idempotency_key", length = 160)
    private String idempotencyKey;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
}
