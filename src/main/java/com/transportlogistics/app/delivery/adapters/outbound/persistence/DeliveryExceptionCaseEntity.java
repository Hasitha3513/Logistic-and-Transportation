package com.transportlogistics.app.delivery.adapters.outbound.persistence;

import com.transportlogistics.app.delivery.domain.model.DeliveryExceptionResolutionCode;
import com.transportlogistics.app.delivery.domain.model.DeliveryExceptionSeverity;
import com.transportlogistics.app.delivery.domain.model.DeliveryExceptionStatus;
import com.transportlogistics.app.delivery.domain.model.DeliveryExceptionType;
import com.transportlogistics.app.delivery.domain.model.DeliveryFailureDisposition;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "delivery_exception_case")
public class DeliveryExceptionCaseEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "delivery_order_id", nullable = false)
    private UUID deliveryOrderId;

    @Column(name = "delivery_attempt_id")
    private UUID deliveryAttemptId;

    @Enumerated(EnumType.STRING)
    @Column(name = "exception_type", nullable = false, length = 50)
    private DeliveryExceptionType exceptionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    private DeliveryExceptionSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private DeliveryExceptionStatus status;

    @Column(name = "description", nullable = false, length = 1000)
    private String description;

    @Column(name = "corrected_location_id")
    private UUID correctedLocationId;

    @Column(name = "otp_attempt_reference", length = 100)
    private String otpAttemptReference;

    @Column(name = "delivered_items_description", length = 1000)
    private String deliveredItemsDescription;

    @Column(name = "undelivered_items_description", length = 1000)
    private String undeliveredItemsDescription;

    @Column(name = "quantity_delivered", precision = 12, scale = 2)
    private BigDecimal quantityDelivered;

    @Column(name = "quantity_undelivered", precision = 12, scale = 2)
    private BigDecimal quantityUndelivered;

    @Enumerated(EnumType.STRING)
    @Column(name = "resolution_code", length = 50)
    private DeliveryExceptionResolutionCode resolutionCode;

    @Column(name = "resolution_notes", length = 1000)
    private String resolutionNotes;

    @Enumerated(EnumType.STRING)
    @Column(name = "follow_up_disposition", length = 50)
    private DeliveryFailureDisposition followUpDisposition;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "reported_at", nullable = false)
    private OffsetDateTime reportedAt;

    @Column(name = "reported_by", nullable = false, length = 128)
    private String reportedBy;

    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;

    @Column(name = "resolved_by", length = 128)
    private String resolvedBy;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @OneToMany(mappedBy = "exceptionCase", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<DeliveryExceptionEvidenceEntity> evidence = new ArrayList<>();

    public DeliveryExceptionCaseEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public UUID getDeliveryOrderId() { return deliveryOrderId; }
    public void setDeliveryOrderId(UUID deliveryOrderId) { this.deliveryOrderId = deliveryOrderId; }
    public UUID getDeliveryAttemptId() { return deliveryAttemptId; }
    public void setDeliveryAttemptId(UUID deliveryAttemptId) { this.deliveryAttemptId = deliveryAttemptId; }
    public DeliveryExceptionType getExceptionType() { return exceptionType; }
    public void setExceptionType(DeliveryExceptionType exceptionType) { this.exceptionType = exceptionType; }
    public DeliveryExceptionSeverity getSeverity() { return severity; }
    public void setSeverity(DeliveryExceptionSeverity severity) { this.severity = severity; }
    public DeliveryExceptionStatus getStatus() { return status; }
    public void setStatus(DeliveryExceptionStatus status) { this.status = status; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public UUID getCorrectedLocationId() { return correctedLocationId; }
    public void setCorrectedLocationId(UUID correctedLocationId) { this.correctedLocationId = correctedLocationId; }
    public String getOtpAttemptReference() { return otpAttemptReference; }
    public void setOtpAttemptReference(String otpAttemptReference) { this.otpAttemptReference = otpAttemptReference; }
    public String getDeliveredItemsDescription() { return deliveredItemsDescription; }
    public void setDeliveredItemsDescription(String deliveredItemsDescription) { this.deliveredItemsDescription = deliveredItemsDescription; }
    public String getUndeliveredItemsDescription() { return undeliveredItemsDescription; }
    public void setUndeliveredItemsDescription(String undeliveredItemsDescription) { this.undeliveredItemsDescription = undeliveredItemsDescription; }
    public BigDecimal getQuantityDelivered() { return quantityDelivered; }
    public void setQuantityDelivered(BigDecimal quantityDelivered) { this.quantityDelivered = quantityDelivered; }
    public BigDecimal getQuantityUndelivered() { return quantityUndelivered; }
    public void setQuantityUndelivered(BigDecimal quantityUndelivered) { this.quantityUndelivered = quantityUndelivered; }
    public DeliveryExceptionResolutionCode getResolutionCode() { return resolutionCode; }
    public void setResolutionCode(DeliveryExceptionResolutionCode resolutionCode) { this.resolutionCode = resolutionCode; }
    public String getResolutionNotes() { return resolutionNotes; }
    public void setResolutionNotes(String resolutionNotes) { this.resolutionNotes = resolutionNotes; }
    public DeliveryFailureDisposition getFollowUpDisposition() { return followUpDisposition; }
    public void setFollowUpDisposition(DeliveryFailureDisposition followUpDisposition) { this.followUpDisposition = followUpDisposition; }
    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }
    public OffsetDateTime getReportedAt() { return reportedAt; }
    public void setReportedAt(OffsetDateTime reportedAt) { this.reportedAt = reportedAt; }
    public String getReportedBy() { return reportedBy; }
    public void setReportedBy(String reportedBy) { this.reportedBy = reportedBy; }
    public OffsetDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(OffsetDateTime resolvedAt) { this.resolvedAt = resolvedAt; }
    public String getResolvedBy() { return resolvedBy; }
    public void setResolvedBy(String resolvedBy) { this.resolvedBy = resolvedBy; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
    public List<DeliveryExceptionEvidenceEntity> getEvidence() { return evidence; }
    public void setEvidence(List<DeliveryExceptionEvidenceEntity> evidence) { this.evidence = evidence; }
}
