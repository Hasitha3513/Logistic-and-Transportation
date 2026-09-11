package com.transportlogistics.app.tracking.domain.speed;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class SpeedThresholdResolver {
    public Optional<ResolvedSpeedThreshold> resolve(UUID tenantId, SpeedAttribution attribution,
                                                     Collection<SpeedRule> rules) {
        Objects.requireNonNull(tenantId, "Tenant ID is required");
        SpeedAttribution safeAttribution = attribution == null ? SpeedAttribution.unknown() : attribution;
        Collection<SpeedRule> safeRules = Objects.requireNonNull(rules, "Rules are required");
        Optional<SpeedRule> route = safeRules.stream()
                .filter(rule -> tenantId.equals(rule.tenantId()))
                .filter(rule -> rule.lifecycle() == SpeedRule.Lifecycle.ACTIVE)
                .filter(rule -> rule.scope() == SpeedRule.Scope.ROUTE_VERSION)
                .filter(rule -> safeAttribution.matchesRoute(rule.routeId(), rule.routeVersion()))
                .findFirst();
        if (route.isPresent()) {
            return Optional.of(new ResolvedSpeedThreshold(route.get(),
                    ResolvedSpeedThreshold.ThresholdSource.ROUTE_CONFIG));
        }
        return safeRules.stream()
                .filter(rule -> tenantId.equals(rule.tenantId()))
                .filter(rule -> rule.lifecycle() == SpeedRule.Lifecycle.ACTIVE)
                .filter(rule -> rule.scope() == SpeedRule.Scope.TENANT)
                .findFirst()
                .map(rule -> new ResolvedSpeedThreshold(rule,
                        ResolvedSpeedThreshold.ThresholdSource.TENANT_CONFIG));
    }
}
