package com.transportlogistics.app.fleet.infrastructure.adapters.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "driver_violation")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
class DriverViolationEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "driver_id", nullable = false)
    private UUID driverId;

    @Column(name = "trip_id")
    private UUID tripId;

    @Column(name = "violation_type", nullable = false, length = 64)
    private String violationType;

    @Column(name = "severity", nullable = false, length = 32)
    private String severity;

    @Column(name = "violation_date", nullable = false)
    private OffsetDateTime violationDate;

    @Column(name = "penalty_points", nullable = false)
    private int penaltyPoints;

    @Column(name = "fine_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal fineAmount;

    @Column(name = "payment_status", nullable = false, length = 32)
    private String paymentStatus;

    @Column(name = "paid_at")
    private OffsetDateTime paidAt;

    @Column(name = "payment_reference", length = 128)
    private String paymentReference;

    @Column(name = "location", length = 255)
    private String location;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "created_by", length = 128)
    private String createdBy;

    @Column(name = "updated_by", length = 128)
    private String updatedBy;
}
