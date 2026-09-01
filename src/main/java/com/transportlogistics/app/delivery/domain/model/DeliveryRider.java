package com.transportlogistics.app.delivery.domain.model;

import com.transportlogistics.app.shared.domain.BusinessRuleException;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class DeliveryRider {

    private final UUID id;
    private final UUID tenantId;
    private final String riderCode;
    private final UUID driverId;
    private final DeliveryRiderType riderType;
    private DeliveryTransportMode transportMode;
    private DeliveryRiderStatus status;
    private UUID primaryZoneId;
    private final Set<UUID> secondaryZoneIds;
    private int maxConcurrentDeliveries;
    private final long version;
    private final OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private final String createdBy;
    private String updatedBy;

    public DeliveryRider(
            UUID id,
            UUID tenantId,
            String riderCode,
            UUID driverId,
            DeliveryRiderType riderType,
            DeliveryTransportMode transportMode,
            DeliveryRiderStatus status,
            UUID primaryZoneId,
            Set<UUID> secondaryZoneIds,
            int maxConcurrentDeliveries,
            long version,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt,
            String createdBy,
            String updatedBy
    ) {
        if (id == null) throw new BusinessRuleException("DELIVERY_RIDER_ID_REQUIRED", "Rider ID is required");
        if (tenantId == null) throw new BusinessRuleException("DELIVERY_RIDER_TENANT_REQUIRED", "Tenant ID is required");
        if (riderCode == null || riderCode.isBlank()) throw new BusinessRuleException("DELIVERY_RIDER_CODE_REQUIRED", "Rider code is required");
        if (driverId == null) throw new BusinessRuleException("DELIVERY_RIDER_DRIVER_REQUIRED", "Driver reference is required");
        if (primaryZoneId == null) throw new BusinessRuleException("DELIVERY_RIDER_PRIMARY_ZONE_REQUIRED", "Primary zone is required");
        if (maxConcurrentDeliveries <= 0) throw new BusinessRuleException("DELIVERY_RIDER_CAPACITY_INVALID", "Max concurrent deliveries must be positive");

        this.id = id;
        this.tenantId = tenantId;
        this.riderCode = riderCode.trim().toUpperCase();
        this.driverId = driverId;
        this.riderType = riderType != null ? riderType : DeliveryRiderType.FULL_TIME;
        this.transportMode = transportMode;
        this.status = status != null ? status : DeliveryRiderStatus.ACTIVE;
        this.primaryZoneId = primaryZoneId;
        this.secondaryZoneIds = new HashSet<>(secondaryZoneIds != null ? secondaryZoneIds : Collections.emptySet());
        this.secondaryZoneIds.remove(primaryZoneId); // Primary zone should not duplicate in secondary set
        this.maxConcurrentDeliveries = maxConcurrentDeliveries;
        this.version = version;
        this.createdAt = createdAt != null ? createdAt : OffsetDateTime.now();
        this.updatedAt = updatedAt != null ? updatedAt : this.createdAt;
        this.createdBy = createdBy != null ? createdBy : "system";
        this.updatedBy = updatedBy != null ? updatedBy : this.createdBy;
    }

    public static DeliveryRider create(
            UUID id,
            UUID tenantId,
            String riderCode,
            UUID driverId,
            DeliveryRiderType riderType,
            DeliveryTransportMode transportMode,
            UUID primaryZoneId,
            Set<UUID> secondaryZoneIds,
            int maxConcurrentDeliveries,
            String actor,
            OffsetDateTime now
    ) {
        if (transportMode == null) {
            throw new BusinessRuleException("DELIVERY_RIDER_TRANSPORT_MODE_REQUIRED", "Transport mode is required");
        }
        return new DeliveryRider(
                id != null ? id : UUID.randomUUID(),
                tenantId,
                riderCode,
                driverId,
                riderType,
                transportMode,
                DeliveryRiderStatus.ACTIVE,
                primaryZoneId,
                secondaryZoneIds,
                maxConcurrentDeliveries,
                0L,
                now,
                now,
                actor,
                actor
        );
    }

    public boolean isEligibleForZone(UUID zoneId) {
        if (zoneId == null) {
            return false;
        }
        return this.primaryZoneId.equals(zoneId) || this.secondaryZoneIds.contains(zoneId);
    }

    public void updateProfile(UUID primaryZoneId, Set<UUID> secondaryZoneIds, int maxConcurrentDeliveries,
                              DeliveryTransportMode transportMode, String actor, OffsetDateTime now) {
        if (primaryZoneId == null) {
            throw new BusinessRuleException("DELIVERY_RIDER_PRIMARY_ZONE_REQUIRED", "Primary zone is required");
        }
        if (maxConcurrentDeliveries <= 0) {
            throw new BusinessRuleException("DELIVERY_RIDER_CAPACITY_INVALID", "Max concurrent deliveries must be positive");
        }
        if (transportMode == null) {
            throw new BusinessRuleException("DELIVERY_RIDER_TRANSPORT_MODE_REQUIRED", "Transport mode is required");
        }
        this.primaryZoneId = primaryZoneId;
        this.secondaryZoneIds.clear();
        if (secondaryZoneIds != null) {
            this.secondaryZoneIds.addAll(secondaryZoneIds);
            this.secondaryZoneIds.remove(primaryZoneId);
        }
        this.maxConcurrentDeliveries = maxConcurrentDeliveries;
        this.transportMode = transportMode;
        this.updatedAt = now;
        this.updatedBy = actor != null ? actor : "system";
    }

    public void activate(String actor, OffsetDateTime now) {
        this.status = DeliveryRiderStatus.ACTIVE;
        this.updatedAt = now;
        this.updatedBy = actor != null ? actor : "system";
    }

    public void deactivate(String actor, OffsetDateTime now) {
        this.status = DeliveryRiderStatus.INACTIVE;
        this.updatedAt = now;
        this.updatedBy = actor != null ? actor : "system";
    }

    public void suspend(String actor, OffsetDateTime now) {
        this.status = DeliveryRiderStatus.SUSPENDED;
        this.updatedAt = now;
        this.updatedBy = actor != null ? actor : "system";
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public String getRiderCode() { return riderCode; }
    public UUID getDriverId() { return driverId; }
    public DeliveryRiderType getRiderType() { return riderType; }
    public DeliveryTransportMode getTransportMode() { return transportMode; }
    public DeliveryRiderStatus getStatus() { return status; }
    public UUID getPrimaryZoneId() { return primaryZoneId; }
    public Set<UUID> getSecondaryZoneIds() { return Collections.unmodifiableSet(secondaryZoneIds); }
    public int getMaxConcurrentDeliveries() { return maxConcurrentDeliveries; }
    public long getVersion() { return version; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public String getCreatedBy() { return createdBy; }
    public String getUpdatedBy() { return updatedBy; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DeliveryRider that = (DeliveryRider) o;
        return Objects.equals(id, that.id) && Objects.equals(tenantId, that.tenantId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, tenantId);
    }
}
