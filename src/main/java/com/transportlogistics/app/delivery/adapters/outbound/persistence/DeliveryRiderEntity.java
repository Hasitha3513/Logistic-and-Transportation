package com.transportlogistics.app.delivery.adapters.outbound.persistence;

import com.transportlogistics.app.delivery.domain.model.DeliveryRider;
import com.transportlogistics.app.delivery.domain.model.DeliveryRiderStatus;
import com.transportlogistics.app.delivery.domain.model.DeliveryRiderType;
import com.transportlogistics.app.delivery.domain.model.DeliveryTransportMode;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "delivery_rider")
public class DeliveryRiderEntity {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "rider_code", nullable = false, length = 40)
    private String riderCode;

    @Column(name = "driver_id", nullable = false)
    private UUID driverId;

    @Enumerated(EnumType.STRING)
    @Column(name = "rider_type", nullable = false, length = 30)
    private DeliveryRiderType riderType;

    @Enumerated(EnumType.STRING)
    @Column(name = "transport_mode", length = 20)
    private DeliveryTransportMode transportMode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private DeliveryRiderStatus status;

    @Column(name = "primary_zone_id", nullable = false)
    private UUID primaryZoneId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "delivery_rider_zone",
            joinColumns = @JoinColumn(name = "rider_id")
    )
    @Column(name = "delivery_zone_id")
    private Set<UUID> secondaryZoneIds = new HashSet<>();

    @Column(name = "max_concurrent_deliveries", nullable = false)
    private int maxConcurrentDeliveries;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;

    public DeliveryRiderEntity() {
    }

    public static DeliveryRiderEntity fromDomain(DeliveryRider domain) {
        DeliveryRiderEntity entity = new DeliveryRiderEntity();
        entity.id = domain.getId();
        entity.tenantId = domain.getTenantId();
        entity.riderCode = domain.getRiderCode();
        entity.driverId = domain.getDriverId();
        entity.riderType = domain.getRiderType();
        entity.transportMode = domain.getTransportMode();
        entity.status = domain.getStatus();
        entity.primaryZoneId = domain.getPrimaryZoneId();
        entity.secondaryZoneIds = new HashSet<>(domain.getSecondaryZoneIds());
        entity.maxConcurrentDeliveries = domain.getMaxConcurrentDeliveries();
        entity.version = domain.getVersion();
        entity.createdAt = domain.getCreatedAt();
        entity.updatedAt = domain.getUpdatedAt();
        entity.createdBy = domain.getCreatedBy();
        entity.updatedBy = domain.getUpdatedBy();
        return entity;
    }

    public DeliveryRider toDomain() {
        return new DeliveryRider(
                id,
                tenantId,
                riderCode,
                driverId,
                riderType,
                transportMode,
                status,
                primaryZoneId,
                secondaryZoneIds,
                maxConcurrentDeliveries,
                version,
                createdAt,
                updatedAt,
                createdBy,
                updatedBy
        );
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public String getRiderCode() { return riderCode; }
    public UUID getDriverId() { return driverId; }
    public DeliveryRiderType getRiderType() { return riderType; }
    public DeliveryTransportMode getTransportMode() { return transportMode; }
    public DeliveryRiderStatus getStatus() { return status; }
    public UUID getPrimaryZoneId() { return primaryZoneId; }
    public Set<UUID> getSecondaryZoneIds() { return secondaryZoneIds; }
    public int getMaxConcurrentDeliveries() { return maxConcurrentDeliveries; }
    public long getVersion() { return version; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public String getCreatedBy() { return createdBy; }
    public String getUpdatedBy() { return updatedBy; }
}
