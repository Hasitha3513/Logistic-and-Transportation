package com.transportlogistics.app.fleet.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.fleet.domain.model.DriverLicenseStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "driver_license")
@Getter
@Setter
@NoArgsConstructor
class DriverLicenseEntity {
    @Id
    private UUID id;
    @Column(name = "driver_id", nullable = false)
    private UUID driverId;
    @Column(name = "license_number", nullable = false, unique = true)
    private String licenseNumber;
    @Column(name = "license_class", nullable = false)
    private String licenseClass;
    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;
    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DriverLicenseStatus status;
    @Column(nullable = false)
    private boolean active;
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
    @Column(name = "created_by", nullable = false)
    private String createdBy;
    @Column(name = "updated_by", nullable = false)
    private String updatedBy;
}
