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
@Table(name = "driver_medical_record")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
class DriverMedicalRecordEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "driver_id", nullable = false)
    private UUID driverId;

    @Column(name = "assessment_date", nullable = false)
    private LocalDate assessmentDate;

    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    @Column(name = "valid_until", nullable = false)
    private LocalDate validUntil;

    @Column(name = "fitness_status", nullable = false, length = 32)
    private String fitnessStatus;

    @Column(name = "vision_test_status", length = 32)
    private String visionTestStatus;

    @Column(name = "restrictions", columnDefinition = "TEXT")
    private String restrictions;

    @Column(name = "examiner_or_provider", length = 255)
    private String examinerOrProvider;

    @Column(name = "certificate_reference", length = 128)
    private String certificateReference;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;

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
