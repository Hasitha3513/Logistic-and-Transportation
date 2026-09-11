package com.transportlogistics.app.tracking.domain.geofence;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class Geofence {
    private final UUID id;
    private final UUID tenantId;
    private String name;
    private GeofenceType type;
    private GeofencePolygon polygon;
    private UUID locationId;
    private GeofenceAlertPolicy alertPolicy;
    private GeofenceLifecycle lifecycle;
    private long version;
    private final Instant createdAt;
    private final UUID createdBy;
    private Instant updatedAt;
    private UUID updatedBy;

    public Geofence(UUID id, UUID tenantId, String name, GeofenceType type,
                    GeofencePolygon polygon, UUID locationId, GeofenceAlertPolicy alertPolicy,
                    GeofenceLifecycle lifecycle, long version, Instant createdAt, UUID createdBy,
                    Instant updatedAt, UUID updatedBy) {
        this.id = Objects.requireNonNull(id, "Geofence ID is required");
        this.tenantId = Objects.requireNonNull(tenantId, "Tenant ID is required");
        this.name = validName(name);
        this.type = Objects.requireNonNull(type, "Geofence type is required");
        this.polygon = Objects.requireNonNull(polygon, "Geofence polygon is required");
        this.locationId = locationId;
        this.alertPolicy = requiredPolicy(type, alertPolicy);
        this.lifecycle = Objects.requireNonNull(lifecycle, "Geofence lifecycle is required");
        if (version < 0) {
            throw new IllegalArgumentException("Geofence version cannot be negative");
        }
        this.version = version;
        this.createdAt = Objects.requireNonNull(createdAt, "Created time is required");
        this.createdBy = Objects.requireNonNull(createdBy, "Created actor is required");
        this.updatedAt = Objects.requireNonNull(updatedAt, "Updated time is required");
        this.updatedBy = Objects.requireNonNull(updatedBy, "Updated actor is required");
        validateLocation(type, locationId);
    }

    public static Geofence draft(UUID id, UUID tenantId, String name, GeofenceType type,
                                 GeofencePolygon polygon, UUID locationId,
                                 GeofenceAlertPolicy alertPolicy, Instant now, UUID actorId) {
        return new Geofence(id, tenantId, name, type, polygon, locationId, alertPolicy,
                GeofenceLifecycle.DRAFT, 0, now, actorId, now, actorId);
    }

    public void updateDefinition(String newName, GeofenceType newType, GeofencePolygon newPolygon,
                                 UUID newLocationId, GeofenceAlertPolicy newAlertPolicy,
                                 Instant now, UUID actorId) {
        requireEditable();
        validateLocation(newType, newLocationId);
        name = validName(newName);
        type = Objects.requireNonNull(newType, "Geofence type is required");
        polygon = Objects.requireNonNull(newPolygon, "Geofence polygon is required");
        locationId = newLocationId;
        alertPolicy = requiredPolicy(newType, newAlertPolicy);
        touch(now, actorId);
    }

    public void activate(Instant now, UUID actorId) {
        if (lifecycle != GeofenceLifecycle.DRAFT && lifecycle != GeofenceLifecycle.DISABLED) {
            throw lifecycle("Only DRAFT or DISABLED geofences can be activated");
        }
        lifecycle = GeofenceLifecycle.ACTIVE;
        touch(now, actorId);
    }

    public void disable(Instant now, UUID actorId) {
        if (lifecycle != GeofenceLifecycle.ACTIVE) {
            throw lifecycle("Only ACTIVE geofences can be disabled");
        }
        lifecycle = GeofenceLifecycle.DISABLED;
        touch(now, actorId);
    }

    public void retire(Instant now, UUID actorId) {
        if (lifecycle == GeofenceLifecycle.ACTIVE || lifecycle == GeofenceLifecycle.RETIRED) {
            throw lifecycle("Only DRAFT or DISABLED geofences can be retired");
        }
        lifecycle = GeofenceLifecycle.RETIRED;
        touch(now, actorId);
    }

    public UUID id() { return id; }
    public UUID tenantId() { return tenantId; }
    public String name() { return name; }
    public GeofenceType type() { return type; }
    public GeofencePolygon polygon() { return polygon; }
    public UUID locationId() { return locationId; }
    public GeofenceAlertPolicy alertPolicy() { return alertPolicy; }
    public GeofenceLifecycle lifecycle() { return lifecycle; }
    public long version() { return version; }
    public Instant createdAt() { return createdAt; }
    public UUID createdBy() { return createdBy; }
    public Instant updatedAt() { return updatedAt; }
    public UUID updatedBy() { return updatedBy; }

    private void requireEditable() {
        if (lifecycle != GeofenceLifecycle.DRAFT && lifecycle != GeofenceLifecycle.DISABLED) {
            throw lifecycle("Geofence definition is editable only in DRAFT or DISABLED");
        }
    }

    private void touch(Instant now, UUID actorId) {
        updatedAt = Objects.requireNonNull(now, "Updated time is required");
        updatedBy = Objects.requireNonNull(actorId, "Updated actor is required");
        version++;
    }

    private static String validName(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new GeofenceRuleException("GEOFENCE_NAME_INVALID", "Geofence name is required");
        }
        return value.trim();
    }

    private static void validateLocation(GeofenceType type, UUID locationId) {
        Objects.requireNonNull(type, "Geofence type is required");
        if ((type == GeofenceType.DEPOT || type == GeofenceType.CUSTOMER_SITE)
                && locationId == null) {
            throw new GeofenceRuleException("GEOFENCE_LOCATION_REQUIRED",
                    "Location-backed geofences require a location");
        }
        if (type == GeofenceType.UNAUTHORIZED_ZONE && locationId != null) {
            throw new GeofenceRuleException("GEOFENCE_LOCATION_INVALID",
                    "Unauthorized zones cannot reference a location");
        }
    }

    private static GeofenceAlertPolicy requiredPolicy(GeofenceType type,
                                                       GeofenceAlertPolicy policy) {
        Objects.requireNonNull(policy, "Geofence alert policy is required");
        if (type == GeofenceType.UNAUTHORIZED_ZONE && !policy.alertOnEntry()) {
            throw new GeofenceRuleException("GEOFENCE_POLICY_INVALID",
                    "Unauthorized-zone entry alerts are mandatory");
        }
        return policy;
    }

    private static GeofenceRuleException lifecycle(String message) {
        return new GeofenceRuleException("GEOFENCE_LIFECYCLE_INVALID", message);
    }
}
