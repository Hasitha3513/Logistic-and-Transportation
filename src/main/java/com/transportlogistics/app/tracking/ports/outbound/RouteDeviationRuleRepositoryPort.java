package com.transportlogistics.app.tracking.ports.outbound;
import com.transportlogistics.app.tracking.domain.routedeviation.*;import java.util.*;
public interface RouteDeviationRuleRepositoryPort {Optional<RouteDeviationRule> findActive(UUID tenantId,UUID routeId,RouteVersion routeVersion);RouteDeviationRule save(RouteDeviationRule rule);}
