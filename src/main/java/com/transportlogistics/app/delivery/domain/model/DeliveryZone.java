package com.transportlogistics.app.delivery.domain.model;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

public class DeliveryZone {
    private final UUID id;
    private final UUID tenantId;
    private String zoneCode;
    private String zoneName;
    private String description;
    private DeliveryZoneType zoneType;
    private DeliveryZoneStatus status;
    private boolean serviceable;
    private Integer dailyCapacity;
    private UUID depotLocationId;
    private DeliveryZoneBoundary boundary;
    private int priority;
    private Long version;
    private final OffsetDateTime createdAt;
    private final String createdBy;
    private OffsetDateTime updatedAt;
    private String updatedBy;

    public DeliveryZone(
            UUID id,
            UUID tenantId,
            String zoneCode,
            String zoneName,
            String description,
            DeliveryZoneType zoneType,
            DeliveryZoneStatus status,
            boolean serviceable,
            Integer dailyCapacity,
            UUID depotLocationId,
            DeliveryZoneBoundary boundary,
            int priority,
            Long version,
            OffsetDateTime createdAt,
            String createdBy,
            OffsetDateTime updatedAt,
            String updatedBy
    ) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId must not be null");
        this.zoneCode = normalizeCode(zoneCode);
        this.zoneName = Objects.requireNonNull(zoneName, "zoneName must not be null").trim();
        this.description = description != null ? description.trim() : null;
        this.zoneType = Objects.requireNonNull(zoneType, "zoneType must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.serviceable = serviceable;
        if (dailyCapacity != null && dailyCapacity < 0) {
            throw new IllegalArgumentException("dailyCapacity must not be negative");
        }
        this.dailyCapacity = dailyCapacity;
        this.depotLocationId = depotLocationId;
        this.boundary = Objects.requireNonNull(boundary, "boundary must not be null");
        this.priority = priority;
        this.version = version;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.createdBy = Objects.requireNonNull(createdBy, "createdBy must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        this.updatedBy = Objects.requireNonNull(updatedBy, "updatedBy must not be null");
    }

    public static DeliveryZone create(
            UUID tenantId,
            String zoneCode,
            String zoneName,
            String description,
            DeliveryZoneType zoneType,
            boolean serviceable,
            Integer dailyCapacity,
            UUID depotLocationId,
            DeliveryZoneBoundary boundary,
            int priority,
            String actor,
            OffsetDateTime now
    ) {
        return new DeliveryZone(
                UUID.randomUUID(),
                tenantId,
                zoneCode,
                zoneName,
                description,
                zoneType,
                DeliveryZoneStatus.ACTIVE,
                serviceable,
                dailyCapacity,
                depotLocationId,
                boundary,
                priority,
                0L,
                now,
                actor,
                now,
                actor
        );
    }

    public void update(
            String zoneName,
            String description,
            DeliveryZoneType zoneType,
            boolean serviceable,
            Integer dailyCapacity,
            UUID depotLocationId,
            DeliveryZoneBoundary boundary,
            int priority,
            String actor,
            OffsetDateTime now
    ) {
        this.zoneName = Objects.requireNonNull(zoneName, "zoneName must not be null").trim();
        this.description = description != null ? description.trim() : null;
        this.zoneType = Objects.requireNonNull(zoneType, "zoneType must not be null");
        this.serviceable = serviceable;
        if (dailyCapacity != null && dailyCapacity < 0) {
            throw new IllegalArgumentException("dailyCapacity must not be negative");
        }
        this.dailyCapacity = dailyCapacity;
        this.depotLocationId = depotLocationId;
        this.boundary = Objects.requireNonNull(boundary, "boundary must not be null");
        this.priority = priority;
        this.updatedAt = now;
        this.updatedBy = actor;
    }

    public void activate(String actor, OffsetDateTime now) {
        this.status = DeliveryZoneStatus.ACTIVE;
        this.updatedAt = now;
        this.updatedBy = actor;
    }

    public void deactivate(String actor, OffsetDateTime now) {
        this.status = DeliveryZoneStatus.INACTIVE;
        this.updatedAt = now;
        this.updatedBy = actor;
    }

    public boolean contains(double longitude, double latitude) {
        return boundary.contains(longitude, latitude);
    }

    public boolean isActive() {
        return status == DeliveryZoneStatus.ACTIVE;
    }

    private static String normalizeCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("zoneCode must not be blank");
        }
        return code.trim().toUpperCase();
    }

    public UUID id() { return id; }
    public UUID tenantId() { return tenantId; }
    public String zoneCode() { return zoneCode; }
    public String zoneName() { return zoneName; }
    public String description() { return description; }
    public DeliveryZoneType zoneType() { return zoneType; }
    public DeliveryZoneStatus status() { return status; }
    public boolean serviceable() { return serviceable; }
    public Integer dailyCapacity() { return dailyCapacity; }
    public UUID depotLocationId() { return depotLocationId; }
    public DeliveryZoneBoundary boundary() { return boundary; }
    public int priority() { return priority; }
    public Long version() { return version; }
    public OffsetDateTime createdAt() { return createdAt; }
    public String createdBy() { return createdBy; }
    public OffsetDateTime updatedAt() { return updatedAt; }
    public String updatedBy() { return updatedBy; }
}
