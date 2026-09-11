package com.transportlogistics.app.tracking.ports.outbound;

import com.transportlogistics.app.tracking.domain.speed.SpeedRule;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpeedRuleRepositoryPort {
    SpeedRule save(SpeedRule rule, long expectedVersion);
    Optional<SpeedRule> find(UUID tenantId, UUID ruleId);
    Optional<SpeedRule> findActiveRouteRule(UUID tenantId, UUID routeId, String routeVersion);
    Optional<SpeedRule> findActiveTenantRule(UUID tenantId);
    List<SpeedRule> find(UUID tenantId, SpeedRule.Scope scope, SpeedRule.Lifecycle lifecycle,
                         int page, int size);
    long count(UUID tenantId, SpeedRule.Scope scope, SpeedRule.Lifecycle lifecycle);
}
