package com.transportlogistics.app.freight.loadplanning.adapters.outbound.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "load_plan_item_placement")
public class LoadPlanItemPlacementEntity extends com.transportlogistics.app.shared.infrastructure.persistence.TenantScopedEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "load_plan_id", nullable = false)
    private LoadPlanEntity loadPlan;

    @Column(name = "manifest_item_id", nullable = false)
    private UUID manifestItemId;

    @Column(name = "placement_order", nullable = false)
    private int placementOrder;

    @Column(name = "zone_reference", length = 120)
    private String zoneReference;

    @Column(name = "stack_group", length = 120)
    private String stackGroup;

    @Column(name = "container_reference", length = 200)
    private String containerReference;

    @Column(name = "loading_sequence", nullable = false)
    private int loadingSequence;

    @Column(name = "special_handling_notes", length = 500)
    private String specialHandlingNotes;

    public LoadPlanItemPlacementEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public LoadPlanEntity getLoadPlan() { return loadPlan; }
    public void setLoadPlan(LoadPlanEntity loadPlan) { this.loadPlan = loadPlan; }

    public UUID getManifestItemId() { return manifestItemId; }
    public void setManifestItemId(UUID manifestItemId) { this.manifestItemId = manifestItemId; }

    public int getPlacementOrder() { return placementOrder; }
    public void setPlacementOrder(int placementOrder) { this.placementOrder = placementOrder; }

    public String getZoneReference() { return zoneReference; }
    public void setZoneReference(String zoneReference) { this.zoneReference = zoneReference; }

    public String getStackGroup() { return stackGroup; }
    public void setStackGroup(String stackGroup) { this.stackGroup = stackGroup; }

    public String getContainerReference() { return containerReference; }
    public void setContainerReference(String containerReference) { this.containerReference = containerReference; }

    public int getLoadingSequence() { return loadingSequence; }
    public void setLoadingSequence(int loadingSequence) { this.loadingSequence = loadingSequence; }

    public String getSpecialHandlingNotes() { return specialHandlingNotes; }
    public void setSpecialHandlingNotes(String specialHandlingNotes) { this.specialHandlingNotes = specialHandlingNotes; }
}
