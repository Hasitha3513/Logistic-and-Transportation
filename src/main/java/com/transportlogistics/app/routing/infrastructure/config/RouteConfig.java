package com.transportlogistics.app.routing.infrastructure.config;

import com.transportlogistics.app.routing.PlannedRouteGeometryLookup;
import com.transportlogistics.app.routing.RouteAssignmentLookup;
import com.transportlogistics.app.routing.RoutePerformanceTripLookupPort;
import com.transportlogistics.app.routing.application.ports.in.RouteUseCase;
import com.transportlogistics.app.routing.application.ports.out.RouteDistancePort;
import com.transportlogistics.app.routing.application.ports.out.RouteDisruptionRepository;
import com.transportlogistics.app.routing.application.ports.out.RouteEventPublisher;
import com.transportlogistics.app.routing.application.ports.out.RouteOperationalExceptionPublisher;
import com.transportlogistics.app.routing.application.ports.out.RouteRepository;
import com.transportlogistics.app.routing.application.ports.out.RouteRevisionRepository;
import com.transportlogistics.app.routing.application.ports.out.RouteRevisionGeometryRepository;
import com.transportlogistics.app.routing.application.ports.out.RouteTransaction;
import com.transportlogistics.app.routing.application.service.RouteService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.util.Objects;

@Configuration
class RouteConfig {
    @Bean
    RouteUseCase routeUseCase(RouteRepository repo,
                              RouteRevisionRepository revisionRepo,
                              RouteDisruptionRepository disruptionRepo,
                              RouteEventPublisher eventPublisher,
                              RouteOperationalExceptionPublisher operationalExceptions,
                              RouteDistancePort distancePort,
                              RoutePerformanceTripLookupPort performanceTripLookup,
                              RouteTransaction transaction,
                              Clock clock) {
        return new RouteService(repo, revisionRepo, disruptionRepo, eventPublisher, operationalExceptions,
            distancePort, performanceTripLookup, transaction, clock);
    }

    @Bean
    RouteAssignmentLookup routeAssignmentLookup(RouteUseCase routes) {
        return id -> {
            var route = routes.get(id);
            var revision = routes.getRevisions(id).stream().findFirst()
                    .orElseThrow(() -> new IllegalStateException("Route has no authoritative revision: " + id));
            return new RouteAssignmentLookup.AssignmentRoute(route.id(), route.originLocationId(),
                    route.destinationLocationId(), route.active(), "REVISION:" + revision.revisionNumber());
        };
    }

    @Bean
    PlannedRouteGeometryLookup plannedRouteGeometryLookup(RouteRevisionGeometryRepository geometries) {
        return (tenantId, routeId, routeVersion) -> {
            Objects.requireNonNull(tenantId, "Tenant ID is required");
            Objects.requireNonNull(routeId, "Route ID is required");
            if (routeVersion == null || routeVersion.length() > 120
                    || !routeVersion.matches("REVISION:[1-9][0-9]*")) {
                throw new IllegalArgumentException("Route version must use REVISION:<positive-integer>");
            }
            return geometries.find(tenantId, routeId, routeVersion);
        };
    }
}
