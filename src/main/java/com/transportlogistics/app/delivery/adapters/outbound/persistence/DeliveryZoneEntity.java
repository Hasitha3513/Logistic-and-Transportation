package com.transportlogistics.app.delivery.adapters.outbound.persistence;

import com.transportlogistics.app.delivery.domain.model.DeliveryZoneStatus;
import com.transportlogistics.app.delivery.domain.model.DeliveryZoneType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "delivery_zone")
public class DeliveryZoneEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "zone_code", nullable = false, length = 30)
    private String zoneCode;

    @Column(name = "zone_name", nullable = false, length = 120)
    private String zoneName;

    @Column(name = "description", length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "zone_type", nullable = false, length = 30)
    private DeliveryZoneType zoneType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private DeliveryZoneStatus status;

    @Column(name = "serviceable", nullable = false)
    private boolean serviceable;

    @Column(name = "daily_capacity")
    private Integer dailyCapacity;

    @Column(name = "depot_location_id")
    private UUID depotLocationId;

    @Column(name = "min_latitude", nullable = false)
    private double minLatitude;

    @Column(name = "max_latitude", nullable = false)
    private double maxLatitude;

    @Column(name = "min_longitude", nullable = false)
    private double minLongitude;

    @Column(name = "max_longitude", nullable = false)
    private double maxLongitude;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "boundary_geojson", nullable = false, columnDefinition = "jsonb")
    private String boundaryGeoJson;

    @Column(name = "priority", nullable = false)
    private int priority;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "created_by", nullable = false, length = 80)
    private String createdBy;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "updated_by", nullable = false, length = 80)
    private String updatedBy;

    public DeliveryZoneEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    public String getZoneCode() { return zoneCode; }
    public void setZoneCode(String zoneCode) { this.zoneCode = zoneCode; }

    public String getZoneName() { return zoneName; }
    public void setZoneName(String zoneName) { this.zoneName = zoneName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public DeliveryZoneType getZoneType() { return zoneType; }
    public void setZoneType(DeliveryZoneType zoneType) { this.zoneType = zoneType; }

    public DeliveryZoneStatus getStatus() { return status; }
    public void setStatus(DeliveryZoneStatus status) { this.status = status; }

    public boolean isServiceable() { return serviceable; }
    public void setServiceable(boolean serviceable) { this.serviceable = serviceable; }

    public Integer getDailyCapacity() { return dailyCapacity; }
    public void setDailyCapacity(Integer dailyCapacity) { this.dailyCapacity = dailyCapacity; }

    public UUID getDepotLocationId() { return depotLocationId; }
    public void setDepotLocationId(UUID depotLocationId) { this.depotLocationId = depotLocationId; }

    public double getMinLatitude() { return minLatitude; }
    public void setMinLatitude(double minLatitude) { this.minLatitude = minLatitude; }

    public double getMaxLatitude() { return maxLatitude; }
    public void setMaxLatitude(double maxLatitude) { this.maxLatitude = maxLatitude; }

    public double getMinLongitude() { return minLongitude; }
    public void setMinLongitude(double minLongitude) { this.minLongitude = minLongitude; }

    public double getMaxLongitude() { return maxLongitude; }
    public void setMaxLongitude(double maxLongitude) { this.maxLongitude = maxLongitude; }

    public String getBoundaryGeoJson() { return boundaryGeoJson; }
    public void setBoundaryGeoJson(String boundaryGeoJson) { this.boundaryGeoJson = boundaryGeoJson; }

    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
}
