package com.transportlogistics.app.delivery.adapters.outbound.persistence;

import com.transportlogistics.app.shared.infrastructure.persistence.TenantScopedEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity @Table(name="pod_evidence")
@Getter @Setter @NoArgsConstructor
class PodEvidenceEntity extends TenantScopedEntity {
    @Id private UUID id;
    @Column(name="proof_of_delivery_id", nullable=false) private UUID proofOfDeliveryId;
    @Column(name="evidence_type", nullable=false, length=20) private String evidenceType;
    @Column(name="storage_reference", length=255) private String storageReference;
    @Column(name="barcode_value", length=64) private String barcodeValue;
    @Column(name="detected_content_type", length=50) private String detectedContentType;
    @Column(name="content_length") private Long contentLength;
    @Column(name="sha256_checksum", length=64) private String checksum;
    @Column(name="original_filename", length=255) private String originalFilename;
    @Column(name="capture_source", nullable=false, length=20) private String captureSource;
    @Column(name="created_by", nullable=false, length=128) private String createdBy;
    @Column(name="created_at", nullable=false) private OffsetDateTime createdAt;
}
