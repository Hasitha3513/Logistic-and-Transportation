package com.transportlogistics.app.tracking.domain.speed;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record SpeedRule(UUID id, UUID tenantId, String name, Scope scope, UUID routeId,
                        String routeVersion, SpeedKph thresholdKph, Lifecycle lifecycle,
                        long ruleVersion, Instant effectiveAt) {
    public enum Scope { TENANT, ROUTE_VERSION }
    public enum Lifecycle { DRAFT, ACTIVE, DISABLED, RETIRED }

    public SpeedRule {
        Objects.requireNonNull(id, "Rule ID is required");
        Objects.requireNonNull(tenantId, "Tenant ID is required");
        if (name == null || name.isBlank() || name.length() > 120) {
            throw invalidScope("Rule name is required and must not exceed 120 characters");
        }
        Objects.requireNonNull(scope, "Rule scope is required");
        Objects.requireNonNull(thresholdKph, "Threshold is required");
        SpeedKph.threshold(thresholdKph.value());
        Objects.requireNonNull(lifecycle, "Rule lifecycle is required");
        if (ruleVersion < 1) {
            throw new SpeedMonitoringException("SPEED_RULE_VERSION_INVALID", "Rule version must be positive");
        }
        if (scope == Scope.TENANT && (routeId != null || routeVersion != null)) {
            throw invalidScope("Tenant rule cannot reference a route");
        }
        if (scope == Scope.ROUTE_VERSION && (routeId == null || routeVersion == null || routeVersion.isBlank())) {
            throw invalidScope("Route-version rule requires route ID and version");
        }
        if (routeVersion != null && routeVersion.length() > 120) {
            throw invalidScope("Route version must not exceed 120 characters");
        }
        if (lifecycle == Lifecycle.ACTIVE && effectiveAt == null) {
            throw new SpeedMonitoringException("SPEED_RULE_EFFECTIVE_AT_REQUIRED", "Active rule requires effective time");
        }
    }

    public boolean editable() {
        return lifecycle == Lifecycle.DRAFT || lifecycle == Lifecycle.DISABLED;
    }

    public SpeedRule update(String updatedName, SpeedKph updatedThreshold, UUID updatedRouteId,
                            String updatedRouteVersion) {
        if (!editable()) {
            throw transition("Only draft or disabled rules are editable");
        }
        return new SpeedRule(id, tenantId, updatedName, scope, updatedRouteId, updatedRouteVersion,
                updatedThreshold, lifecycle, ruleVersion + 1, effectiveAt);
    }

    public SpeedRule activate(Instant activatedAt) {
        if (lifecycle != Lifecycle.DRAFT && lifecycle != Lifecycle.DISABLED) {
            throw transition("Only draft or disabled rules may be activated");
        }
        return new SpeedRule(id, tenantId, name, scope, routeId, routeVersion, thresholdKph,
                Lifecycle.ACTIVE, ruleVersion + 1, Objects.requireNonNull(activatedAt));
    }

    public SpeedRule disable() {
        if (lifecycle != Lifecycle.ACTIVE) {
            throw transition("Only an active rule may be disabled");
        }
        return new SpeedRule(id, tenantId, name, scope, routeId, routeVersion, thresholdKph,
                Lifecycle.DISABLED, ruleVersion + 1, effectiveAt);
    }

    public SpeedRule retire() {
        if (lifecycle != Lifecycle.DRAFT && lifecycle != Lifecycle.DISABLED) {
            throw transition("Only a draft or disabled rule may be retired");
        }
        return new SpeedRule(id, tenantId, name, scope, routeId, routeVersion, thresholdKph,
                Lifecycle.RETIRED, ruleVersion + 1, effectiveAt);
    }

    private static SpeedMonitoringException invalidScope(String message) {
        return new SpeedMonitoringException("SPEED_RULE_SCOPE_INVALID", message);
    }

    private static SpeedMonitoringException transition(String message) {
        return new SpeedMonitoringException("SPEED_RULE_TRANSITION_INVALID", message);
    }
}
