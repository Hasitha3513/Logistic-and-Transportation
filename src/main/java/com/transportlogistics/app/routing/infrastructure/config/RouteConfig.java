package com.transportlogistics.app.routing.infrastructure.config;

import com.transportlogistics.app.routing.RouteAssignmentLookup;
import com.transportlogistics.app.routing.RoutePerformanceTripLookupPort;
import com.transportlogistics.app.routing.application.ports.in.RouteUseCase;
import com.transportlogistics.app.routing.application.ports.out.RouteDistancePort;
import com.transportlogistics.app.routing.application.ports.out.RouteDisruptionRepository;
import com.transportlogistics.app.routing.application.ports.out.RouteEventPublisher;
import com.transportlogistics.app.routing.application.ports.out.RouteRepository;
import com.transportlogistics.app.routing.application.ports.out.RouteRevisionRepository;
import com.transportlogistics.app.routing.application.ports.out.RouteTransaction;
import com.transportlogistics.app.routing.application.service.RouteService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
class RouteConfig {
    @Bean
    RouteUseCase routeUseCase(RouteRepository repo,
                              RouteRevisionRepository revisionRepo,
                              RouteDisruptionRepository disruptionRepo,
                              RouteEventPublisher eventPublisher,
                              RouteDistancePort distancePort,
                              RoutePerformanceTripLookupPort performanceTripLookup,
                              RouteTransaction transaction,
                              Clock clock) {
        return new RouteService(repo, revisionRepo, disruptionRepo, eventPublisher, distancePort, performanceTripLookup, transaction, clock);
    }

    @Bean
    RouteAssignmentLookup routeAssignmentLookup(RouteUseCase routes) {
        return id -> {
            var route = routes.get(id);
            return new RouteAssignmentLookup.AssignmentRoute(route.id(), route.originLocationId(),
                    route.destinationLocationId(), route.active());
        };
    }
}
