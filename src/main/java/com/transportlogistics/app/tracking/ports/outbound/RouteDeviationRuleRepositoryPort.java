package com.transportlogistics.app.tracking.ports.outbound;

import com.transportlogistics.app.tracking.domain.routedeviation.RouteDeviationRule;
import com.transportlogistics.app.tracking.domain.routedeviation.RouteVersion;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RouteDeviationRuleRepositoryPort {
    Optional<RouteDeviationRule> findActive(UUID tenantId, UUID routeId, RouteVersion routeVersion);
    default Optional<RouteDeviationRule> findRule(UUID tenantId, UUID ruleId) {
        return Optional.empty();
    }
    default List<RouteDeviationRule> list(UUID tenantId, RouteDeviationRule.Lifecycle lifecycle,
                                          int offset, int size) {
        return List.of();
    }
    default long count(UUID tenantId, RouteDeviationRule.Lifecycle lifecycle) {
        return 0;
    }
    RouteDeviationRule save(RouteDeviationRule rule);
}
