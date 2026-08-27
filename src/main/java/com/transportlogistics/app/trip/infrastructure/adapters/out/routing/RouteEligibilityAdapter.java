package com.transportlogistics.app.trip.infrastructure.adapters.out.routing;

import com.transportlogistics.app.routing.RouteAssignmentLookup;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.trip.application.ports.out.RouteEligibilityPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
class RouteEligibilityAdapter implements RouteEligibilityPort {
    private final RouteAssignmentLookup routes;

    @Override
    public void assertAssignable(UUID routeId, UUID originLocationId, UUID destinationLocationId) {
        var route = routes.get(routeId);
        if (!route.active()) {
            throw new BusinessRuleException("ROUTE_INACTIVE", "Only an active route can be assigned to a trip");
        }
        if (!route.originLocationId().equals(originLocationId)
                || !route.destinationLocationId().equals(destinationLocationId)) {
            throw new BusinessRuleException("ROUTE_ENDPOINT_MISMATCH",
                    "Route origin and destination must match the trip");
        }
    }
}
