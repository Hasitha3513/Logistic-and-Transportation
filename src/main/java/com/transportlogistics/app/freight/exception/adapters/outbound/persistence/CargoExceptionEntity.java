package com.transportlogistics.app.freight.exception.adapters.outbound.persistence;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "cargo_exception")
public class CargoExceptionEntity extends com.transportlogistics.app.shared.infrastructure.persistence.TenantScopedEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "exception_number", nullable = false, unique = true, length = 32)
    private String exceptionNumber;

    @Column(name = "exception_type", nullable = false, length = 40)
    private String exceptionType;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "severity", nullable = false, length = 20)
    private String severity;

    @Column(name = "freight_order_id", nullable = false)
    private UUID freightOrderId;

    @Column(name = "manifest_id")
    private UUID manifestId;

    @Column(name = "manifest_item_id")
    private UUID manifestItemId;

    @Column(name = "description", nullable = false, length = 2000)
    private String description;

    @Column(name = "impact", length = 2000)
    private String impact;

    @Column(name = "restriction", length = 1000)
    private String restriction;

    @Column(name = "corrective_action", length = 2000)
    private String correctiveAction;

    @Column(name = "resolution", length = 2000)
    private String resolution;

    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;

    @Column(name = "resolved_by", length = 128)
    private String resolvedBy;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "created_by", nullable = false, length = 128)
    private String createdBy;

    @Column(name = "updated_by", nullable = false, length = 128)
    private String updatedBy;

    @OneToMany(mappedBy = "exceptionEntity", cascade = CascadeType.ALL,
               orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("occurredAt ASC")
    private List<CargoExceptionHistoryEntity> historyEntries = new ArrayList<>();

    public CargoExceptionEntity() {}

    public void replaceHistory(List<CargoExceptionHistoryEntity> entries) {
        historyEntries.clear();
        if (entries != null) {
            entries.forEach(e -> {
                e.setExceptionEntity(this);
                historyEntries.add(e);
            });
        }
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getExceptionNumber() { return exceptionNumber; }
    public void setExceptionNumber(String exceptionNumber) { this.exceptionNumber = exceptionNumber; }

    public String getExceptionType() { return exceptionType; }
    public void setExceptionType(String exceptionType) { this.exceptionType = exceptionType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public UUID getFreightOrderId() { return freightOrderId; }
    public void setFreightOrderId(UUID freightOrderId) { this.freightOrderId = freightOrderId; }

    public UUID getManifestId() { return manifestId; }
    public void setManifestId(UUID manifestId) { this.manifestId = manifestId; }

    public UUID getManifestItemId() { return manifestItemId; }
    public void setManifestItemId(UUID manifestItemId) { this.manifestItemId = manifestItemId; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImpact() { return impact; }
    public void setImpact(String impact) { this.impact = impact; }

    public String getRestriction() { return restriction; }
    public void setRestriction(String restriction) { this.restriction = restriction; }

    public String getCorrectiveAction() { return correctiveAction; }
    public void setCorrectiveAction(String correctiveAction) { this.correctiveAction = correctiveAction; }

    public String getResolution() { return resolution; }
    public void setResolution(String resolution) { this.resolution = resolution; }

    public OffsetDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(OffsetDateTime resolvedAt) { this.resolvedAt = resolvedAt; }

    public String getResolvedBy() { return resolvedBy; }
    public void setResolvedBy(String resolvedBy) { this.resolvedBy = resolvedBy; }

    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }

    public List<CargoExceptionHistoryEntity> getHistoryEntries() { return historyEntries; }
    public void setHistoryEntries(List<CargoExceptionHistoryEntity> historyEntries) {
        this.historyEntries = historyEntries;
    }
}
