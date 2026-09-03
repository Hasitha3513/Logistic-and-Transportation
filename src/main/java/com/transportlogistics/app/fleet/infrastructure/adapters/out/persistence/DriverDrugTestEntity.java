package com.transportlogistics.app.fleet.infrastructure.adapters.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "driver_drug_test")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
class DriverDrugTestEntity extends com.transportlogistics.app.shared.infrastructure.persistence.TenantScopedEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "driver_id", nullable = false)
    private UUID driverId;

    @Column(name = "test_type", nullable = false, length = 32)
    private String testType;

    @Column(name = "scheduled_date", nullable = false)
    private LocalDate scheduledDate;

    @Column(name = "sample_collected_at")
    private OffsetDateTime sampleCollectedAt;

    @Column(name = "result_date")
    private LocalDate resultDate;

    @Column(name = "result", nullable = false, length = 32)
    private String result;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "laboratory_or_provider", length = 255)
    private String laboratoryOrProvider;

    @Column(name = "reference_number", length = 128)
    private String referenceNumber;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;

    @Column(name = "return_to_duty_required", nullable = false)
    private boolean returnToDutyRequired;

    @Column(name = "return_to_duty_cleared_at")
    private OffsetDateTime returnToDutyClearedAt;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "created_by", length = 128)
    private String createdBy;

    @Column(name = "updated_by", length = 128)
    private String updatedBy;
}
