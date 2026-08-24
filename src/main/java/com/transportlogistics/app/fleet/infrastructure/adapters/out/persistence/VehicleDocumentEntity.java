package com.transportlogistics.app.fleet.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.fleet.domain.model.VehicleDocumentStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "vehicle_document")
@Getter
@Setter
@NoArgsConstructor
class VehicleDocumentEntity {
    @Id
    private UUID id;
    @Column(name = "vehicle_id", nullable = false)
    private UUID vehicleId;
    @Column(name = "document_type", nullable = false)
    private String documentType;
    @Column(name = "document_number", nullable = false)
    private String documentNumber;
    @Column(name = "issue_date")
    private LocalDate issueDate;
    @Column(name = "expiry_date")
    private LocalDate expiryDate;
    @Column(name = "file_reference")
    private String fileReference;
    @Column(name = "mandatory_for_dispatch", nullable = false)
    private boolean mandatoryForDispatch;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VehicleDocumentStatus status;
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
