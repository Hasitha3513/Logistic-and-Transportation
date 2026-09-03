package com.transportlogistics.app.fleet.infrastructure.adapters.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "driver_violation")
public class DriverViolationEntity extends com.transportlogistics.app.shared.infrastructure.persistence.TenantScopedEntity {

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

    public DriverViolationEntity() {}

    public DriverViolationEntity(UUID id, UUID driverId, UUID tripId, String violationType, String severity,
                                 OffsetDateTime violationDate, int penaltyPoints, BigDecimal fineAmount,
                                 String paymentStatus, OffsetDateTime paidAt, String paymentReference,
                                 String location, String description, OffsetDateTime createdAt,
                                 OffsetDateTime updatedAt, String createdBy, String updatedBy) {
        this.id = id;
        this.driverId = driverId;
        this.tripId = tripId;
        this.violationType = violationType;
        this.severity = severity;
        this.violationDate = violationDate;
        this.penaltyPoints = penaltyPoints;
        this.fineAmount = fineAmount;
        this.paymentStatus = paymentStatus;
        this.paidAt = paidAt;
        this.paymentReference = paymentReference;
        this.location = location;
        this.description = description;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getDriverId() { return driverId; }
    public void setDriverId(UUID driverId) { this.driverId = driverId; }

    public UUID getTripId() { return tripId; }
    public void setTripId(UUID tripId) { this.tripId = tripId; }

    public String getViolationType() { return violationType; }
    public void setViolationType(String violationType) { this.violationType = violationType; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public OffsetDateTime getViolationDate() { return violationDate; }
    public void setViolationDate(OffsetDateTime violationDate) { this.violationDate = violationDate; }

    public int getPenaltyPoints() { return penaltyPoints; }
    public void setPenaltyPoints(int penaltyPoints) { this.penaltyPoints = penaltyPoints; }

    public BigDecimal getFineAmount() { return fineAmount; }
    public void setFineAmount(BigDecimal fineAmount) { this.fineAmount = fineAmount; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public OffsetDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(OffsetDateTime paidAt) { this.paidAt = paidAt; }

    public String getPaymentReference() { return paymentReference; }
    public void setPaymentReference(String paymentReference) { this.paymentReference = paymentReference; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
}
