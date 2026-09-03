package com.transportlogistics.app.freight.loadplanning.adapters.outbound.persistence;

import com.transportlogistics.app.freight.loadplanning.domain.LoadPlanReadinessStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "load_plan")
public class LoadPlanEntity extends com.transportlogistics.app.shared.infrastructure.persistence.TenantScopedEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "load_plan_number", nullable = false, unique = true, length = 60)
    private String loadPlanNumber;

    @Column(name = "cargo_manifest_id", nullable = false)
    private UUID cargoManifestId;

    @Column(name = "vehicle_id", nullable = false)
    private UUID vehicleId;

    @Column(name = "notes", length = 2000)
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(name = "readiness_status", nullable = false, length = 40)
    private LoadPlanReadinessStatus readinessStatus = LoadPlanReadinessStatus.DRAFT;

    @Column(name = "ready_at")
    private OffsetDateTime readyAt;

    @Column(name = "ready_by", length = 128)
    private String readyBy;

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

    @OneToMany(mappedBy = "loadPlan", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("placementOrder ASC")
    private List<LoadPlanItemPlacementEntity> placements = new ArrayList<>();

    public LoadPlanEntity() {}

    public void replacePlacements(List<LoadPlanItemPlacementEntity> newPlacements) {
        placements.clear();
        if (newPlacements != null) {
            newPlacements.forEach(placement -> {
                placement.setLoadPlan(this);
                placements.add(placement);
            });
        }
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getLoadPlanNumber() { return loadPlanNumber; }
    public void setLoadPlanNumber(String loadPlanNumber) { this.loadPlanNumber = loadPlanNumber; }

    public UUID getCargoManifestId() { return cargoManifestId; }
    public void setCargoManifestId(UUID cargoManifestId) { this.cargoManifestId = cargoManifestId; }

    public UUID getVehicleId() { return vehicleId; }
    public void setVehicleId(UUID vehicleId) { this.vehicleId = vehicleId; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LoadPlanReadinessStatus getReadinessStatus() { return readinessStatus; }
    public void setReadinessStatus(LoadPlanReadinessStatus readinessStatus) { this.readinessStatus = readinessStatus; }

    public OffsetDateTime getReadyAt() { return readyAt; }
    public void setReadyAt(OffsetDateTime readyAt) { this.readyAt = readyAt; }

    public String getReadyBy() { return readyBy; }
    public void setReadyBy(String readyBy) { this.readyBy = readyBy; }

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

    public List<LoadPlanItemPlacementEntity> getPlacements() { return placements; }
    public void setPlacements(List<LoadPlanItemPlacementEntity> placements) { this.placements = placements; }
}
