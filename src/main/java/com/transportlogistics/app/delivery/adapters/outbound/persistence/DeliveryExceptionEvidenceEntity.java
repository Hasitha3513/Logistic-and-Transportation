package com.transportlogistics.app.delivery.adapters.outbound.persistence;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "delivery_exception_evidence")
public class DeliveryExceptionEvidenceEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exception_case_id", nullable = false)
    private DeliveryExceptionCaseEntity exceptionCase;

    @Column(name = "storage_reference", nullable = false)
    private String storageReference;

    @Column(name = "detected_content_type", nullable = false, length = 50)
    private String detectedContentType;

    @Column(name = "content_length", nullable = false)
    private long contentLength;

    @Column(name = "sha256_checksum", nullable = false, length = 64)
    private String sha256Checksum;

    @Column(name = "original_filename")
    private String originalFilename;

    @Column(name = "created_by", nullable = false, length = 128)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    public DeliveryExceptionEvidenceEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public DeliveryExceptionCaseEntity getExceptionCase() { return exceptionCase; }
    public void setExceptionCase(DeliveryExceptionCaseEntity exceptionCase) { this.exceptionCase = exceptionCase; }
    public String getStorageReference() { return storageReference; }
    public void setStorageReference(String storageReference) { this.storageReference = storageReference; }
    public String getDetectedContentType() { return detectedContentType; }
    public void setDetectedContentType(String detectedContentType) { this.detectedContentType = detectedContentType; }
    public long getContentLength() { return contentLength; }
    public void setContentLength(long contentLength) { this.contentLength = contentLength; }
    public String getSha256Checksum() { return sha256Checksum; }
    public void setSha256Checksum(String sha256Checksum) { this.sha256Checksum = sha256Checksum; }
    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
